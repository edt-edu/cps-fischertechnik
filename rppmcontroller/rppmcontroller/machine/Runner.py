from __future__ import annotations

import logging
import typing
from abc import abstractmethod
from copy import deepcopy
from dataclasses import dataclass
from typing import Any
from typing import Callable, Union, List
from typing import Optional
from typing_extensions import override

from rppmcontroller.behavior.CycleStepResult import CycleStepResult
from rppmcontroller.behavior.CycleStepResultEnum import CycleStepResultEnum
from rppmcontroller.machine.MachineConfiguration import MachineConfiguration
from rppmcontroller.machine.Timer import Timer


class TransitioningMachine:
    def __init__(self):
        self.__runners: List[Runner] = []
        """A list containing all runners which this machine ever created"""

    @abstractmethod
    def goto_config(self, config) -> CycleStepResult:
        """
        Transition the machine into the specified configuration
        :param config: A machine config for the specific machine
        :return: A CycleStepResult describing the progress
        """
        pass

    def create_runner(self) -> Runner:
        """
        Creates a new Runner for this machine and set it as last_runner
        :return: A Runner
        """
        runner = Runner(self)
        self.__runners.append(runner)
        return runner

    @property
    def is_executing_runner(self) -> bool:
        return any(runner.running for runner in self.__runners)
    
    @property
    def executing_runner(self) -> Optional[Subroutine]:
        for runner in self.__runners:
            if runner.running:
                return runner.__routine[runner.__routine_index]


class Subroutine:
    """
    A subroutine within a Runner, mainly representing a Callable[[],
    CycleStepResult],
    but may also contain additional info
    """

    def __init__(self,
                 runnable: Callable[[], CycleStepResult],
                 info: str = ""):
        self.__runnable: Callable[[], CycleStepResult] = runnable
        """The underlying runnable being called when this is called"""
        self.info: str = info
        """More info about what this subroutine does"""

    def __call__(self, *args, **kwargs) -> CycleStepResult:
        """
        Call the underlying runnable
        :param args: ignored
        :param kwargs: ignored
        :return: A CycleStepResult
        """
        return self.__runnable()

    def __str__(self):
        return (f"SubRoutine("
                f"{self.info if len(self.info) > 0 else f'{self.__runnable}'})")


class Runner(CycleStepResult):
    """
    Transitions a machine into different configurations.

    Can also be used as momentary CycleStepResult.
    """

    def __init__(self, machine: TransitioningMachine):
        """
        Create a new runner for the specified machine.

        :param machine: The machine to control
        """
        super().__init__(CycleStepResultEnum.MUST_CONTINUE,
                         "runner hasn't started yet")
        self.__machine: TransitioningMachine = machine
        """The machine we are working on"""
        self.__routine: List[Subroutine] = []
        """Functions which transition the machine into a desired state"""
        self.__routine_index: int = 0
        """The index of the currently aspirated state"""
        self.__running: bool = False
        """Whether we are currently executing our routine"""
        self.status_published = False

    @override
    @CycleStepResult.result.setter
    def result(self, result : CycleStepResultEnum):
        if self._result != result:
            self.status_published = False
            self._result = result

    def then_goto(self,
                  config: MachineConfiguration,
                  until: Optional[
                      Callable[[], Union[bool, CycleStepResult]]] = None,
                  and_stay_for: float = 0.0,
                  or_timeout_after: float = 0.0,
                  clone_config: bool = True,
                  info: str = "") -> typing.Self:
        """
        Append a transition to the specified config to this routine.
        :param config: The config to transition to.
        :param until: If present, specifies whether the configuration has
        been reached.
        :param and_stay_for: Seconds to remain in the specified
        configuration after it has been reached.
        :param or_timeout_after: The number of seconds after which the config
        is considered reached. Values smaller or equal to zero imply infinite
        time.
        :param clone_config: Whether to clone the config object so it can be
        reused outside of this method.
        :param info: A human-readable info what the runner is doing in this
        step, similar to a comment
        :return: self
        """
        if clone_config:
            config = deepcopy(config)
        return self.then_run(lambda: self.__machine.goto_config(config),
                             until,
                             and_stay_for=and_stay_for,
                             or_timeout_after=or_timeout_after,
                             info=info)

    def then_run(self,
                 runnable: Callable[[], Any],
                 until: Optional[
                     Callable[[], Union[bool, CycleStepResult]]] = None,
                 and_stay_for: float = 0.0,
                 or_timeout_after: float = 0.0,
                 info: str = "") -> typing.Self:
        """
        Appends the specified runnable to this routine. It'll be called
        until it specifies that it is done.
        :param runnable: The runnable to execute. Must return a
        CycleStepResult or `True` in
        order to indicate that it is done.
        :param until: A check whether the runnable is done. If set,
        the return value of the runnable is ignored.
        :param and_stay_for: The number of seconds to continue to call the
        runnable after it is done.
        :param or_timeout_after: The number of seconds after which the runnable
        is considered done. Values smaller or equal to zero imply infinite
        time.
        :param info: A human-readable info what the runner is doing in this
        step, similar to a comment
        :return: self
        """
        hold_timer = Timer(and_stay_for)
        timeout_timer = None
        if or_timeout_after > 0:
            timeout_timer = Timer(or_timeout_after)

        def sub_routine_runnable() -> CycleStepResult:
            res = runnable()
            if until is not None:
                res = until()

            if isinstance(res, bool):
                if res:
                    res = CycleStepResult(CycleStepResultEnum.DONE)
                else:
                    res = CycleStepResult(CycleStepResultEnum.MUST_CONTINUE)

            if timeout_timer is not None:
                if not timeout_timer.is_started():
                    timeout_timer.start()

                if timeout_timer.elapsed():
                    res = CycleStepResult(CycleStepResultEnum.ABORTED_TIMEOUT,
                                          "runner timeout",
                                          (f"subRoutineIndex: "
                                           f"{self.__routine_index}", res))

            if res.is_terminated():
                return res

            if res.must_continue():
                hold_timer.reset()
                return res

            # we can assume that the sub-routine is done
            if not hold_timer.is_started():
                hold_timer.start()

            timer_elapsed = hold_timer.elapsed()
            if not timer_elapsed:
                return CycleStepResult(CycleStepResultEnum.MUST_CONTINUE,
                                       "waiting for hold timer",
                                       (f"subRoutineIndex: "
                                        f"{self.__routine_index}", res))

            return res

        sub_routine = Subroutine(sub_routine_runnable, info)

        self.__routine.append(sub_routine)
        return self

    def then_run_runner_from(self,
                             runner_supplier: Callable[[], Runner],
                             until: Optional[Callable[
                                 [], Union[bool, CycleStepResult]]] = None,
                             or_timeout_after: float = 0.0,
                             info: str = "") -> typing.Self:
        """
        Run the runner provided by the specified runner_supplier until it is
        done.
        :param runner_supplier: A callable returning a Runner
        :param until: When provided, the runner is called until this function
        indicates the desired state has been reached
        :param or_timeout_after: The number of seconds after which the runner
        is considered finished. Values smaller or equal to zero imply infinite
        time.
        :param info: A human-readable info what the runner is doing in this
        step, similar to a comment
        :return: self
        """

        @dataclass
        class RunnerPointer:
            def __init__(self):
                self.__runner: Optional[Runner] = None

            @property
            def runner(self) -> Runner:
                if self.__runner is None:
                    self.__runner = runner_supplier()
                return self.__runner

            def run(self) -> CycleStepResult:
                self.runner.run()
                finished = not self.runner.running
                if finished:
                    return CycleStepResult(CycleStepResultEnum.DONE)

                return CycleStepResult(CycleStepResultEnum.MUST_CONTINUE)

        runner_pointer = RunnerPointer()
        return self.then_run(runner_pointer.run, until, or_timeout_after=or_timeout_after, info=info)

    def run(self) -> typing.Self:
        """
        Advance the current routine and update the CycleStepResult properties
        of this
        :return: self
        """
        self.__running = True
        self.result = CycleStepResultEnum.MUST_CONTINUE
        sub_routine = self.__routine[self.__routine_index]

        sub_routine_info = (f"subroutine {self.__routine_index + 1}/"
                            f"{len(self.__routine)}: {sub_routine}")
        self.info = f"running {sub_routine_info}"

        #logging.debug(self.info)

        # call the sub routine
        res = sub_routine()
        self.subCycleStepResult = (sub_routine_info, res)

        if res.is_terminated():
            self.__routine_index = 0
            self.__running = False
            self.info = "routine aborted"
            self.result = res.result
        elif not res.must_continue():
            self.__routine_index += 1
            if self.__routine_index >= len(self.__routine):
                self.__routine_index = 0  # we are done
                self.__running = False
                self.result = CycleStepResultEnum.DONE
                self.info = "routine finished"
            else:
                self.run()  # directly start the next routine to avoid idling

        # The main loop assumes that every step returns a unique CycleStepResult
        # But the Runner(subclass of CycleStepResult) aggregates multiple steps into one
        # Thus, we need to return a new object(with a different id) with the same internal state
        #return copy.deepcopy(self)
        return self

    @property
    def running(self) -> bool:
        return self.__running

    def __bool__(self):
        """
        Checks whether this routine is finished
        :return: True if this is not running
        """
        return not self.running

    def __call__(self, *args, **kwargs):
        """
        Call the run function
        :param args: ignored
        :param kwargs: ignored
        :return: self
        """
        return self.run()

    def __str__(self):
        return f"Runner(state={self.result}, info={self.info})"
