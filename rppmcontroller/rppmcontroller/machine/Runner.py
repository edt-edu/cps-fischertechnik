from __future__ import annotations

import logging
import typing
from abc import abstractmethod
from copy import deepcopy
from dataclasses import dataclass
from typing import Any
from typing import Callable, Union
from typing import Optional

from rppmcontroller.machine.MachineConfiguration import MachineConfiguration
from rppmcontroller.machine.Timer import Timer


class TransitioningMachine:
    def __init__(self):
        self.__runners: [Runner] = []

    @abstractmethod
    def goto_config(self, config) -> bool:
        """
        Transition the machine into the specified configuration
        :param config: A machine config for the specific machine
        :return: True when the configuration has been reached, otherwise False
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


class Runner:
    """
    Transitions a machine into different configurations.
    """

    def __init__(self, machine: TransitioningMachine):
        """
        Create a new runner for the specified machine.

        :param machine: The machine to control
        """
        self.__machine: TransitioningMachine = machine
        self.__routine: [Callable[[], bool]] = []
        self.__routine_index: int = 0
        self.__running: bool = False

    def then_goto(self,
                  config: MachineConfiguration,
                  until: Optional[Callable[[], bool]] = None,
                  and_stay_for: float = 0.0,
                  or_timeout_after: float = 0.0,
                  clone_config: bool = True) -> typing.Self:
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
        :return: self
        """
        if clone_config:
            config = deepcopy(config)
        return self.then_run(lambda: self.__machine.goto_config(config),
                             until,
                             and_stay_for,
                             or_timeout_after)

    def then_run(self,
                 runnable: Union[Callable[[], Any], Callable[[], bool]],
                 until: Optional[Callable[[], bool]] = None,
                 and_stay_for: float = 0.0,
                 or_timeout_after: float = 0.0) -> typing.Self:
        """
        Appends the specified runnable to this routine. It'll be called
        until it
        specifies that it is done.
        :param runnable: The runnable to execute. Must return `True` in
        order to indicate that it is done.
        :param until: A check whether the runnable is done. If set,
        the return value of the runnable is ignored.
        :param and_stay_for: The number of seconds to continue to call the
        runnable after it is done.
        :param or_timeout_after: The number of seconds after which the runnable
        is considered done. Values smaller or equal to zero imply infinite
        time.
        :return: self
        """
        hold_timer = Timer(and_stay_for)
        timeout_timer = None
        if or_timeout_after > 0:
            timeout_timer = Timer(or_timeout_after)

        def sub_routine() -> bool:
            res = runnable()
            if until is not None:
                res = until()
                if not res:
                    logging.debug("waiting until condition is reached")

            if timeout_timer is not None and timeout_timer.elapsed():
                res = True  # timeout dictates we are done

            if res:
                timer_elapsed = hold_timer.elapsed()
                if not timer_elapsed:
                    logging.debug("waiting for timer...")
                return timer_elapsed
            else:
                hold_timer.reset()
                return False

        self.__routine.append(sub_routine)
        return self

    def then_run_runner_from(self,
                             runner_supplier: Callable[[], Runner],
                             until: Optional[Callable[[], bool]] = None,
                             or_timeout_after: float = 0.0) -> typing.Self:
        """
        Run the runner provided by the specified runner_supplier until it is
        done.
        :param runner_supplier: A callable returning a Runner
        :param until: When provided, the runner is called until it returns true
        :param or_timeout_after: The number of seconds after which the runner
        is considered finished. Values smaller or equal to zero imply infinite
        time.
        :return: self
        """

        @dataclass
        class RunnerPointer:
            __runner: Runner = None

            @property
            def runner(self) -> Runner:
                if self.__runner is None:
                    self.__runner = runner_supplier()
                return self.__runner

            def run(self):
                logging.debug("running sub-routine runner")
                self.runner.run()

            def finished(self) -> bool:
                finished = not self.runner.running
                logging.debug(f"sub-routine runner finished: {finished}")
                return finished

        runner_pointer = RunnerPointer()
        return self.then_run(runner_pointer.run,
                             until if until is not None else
                             runner_pointer.finished,
                             or_timeout_after)

    def run(self) -> typing.Self:
        """
        Advance the current routine
        :return: self
        """
        logging.debug(f"running subroutine {self.__routine_index + 1}/"
                      f"{len(self.__routine)}")
        self.__running = True
        sub_routine = self.__routine[self.__routine_index]
        # call the sub routine
        res = sub_routine()
        if not isinstance(res, bool):
            raise ValueError(f"sub_routine {sub_routine} was expected to "
                             f"return a bool, but instead returned {res}")

        sub_routine_finished = res
        if sub_routine_finished:
            self.__routine_index += 1
            if self.__routine_index >= len(self.__routine):
                self.__routine_index = 0  # we are done
                logging.debug("routine finished")
                self.__running = False
            else:
                self.run()  # directly start the next routine to avoid idling

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
