from __future__ import annotations

import logging
import typing
from abc import abstractmethod
from copy import deepcopy
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
                  clone_config: bool = True) -> typing.Self:
        """
        Append a transition to the specified config to this routine.
        :param config: The config to transition to.
        :param until: If present, specifies whether the configuration has
        been reached.
        :param and_stay_for: Seconds to remain in the specified
        configuration after it has been reached.
        :param clone_config: Whether to clone the config object so it can be
        reused outside of this method.
        :return: self
        """
        if clone_config:
            config = deepcopy(config)
        return self.then_run(lambda: self.__machine.goto_config(config),
                             until,
                             and_stay_for)

    def then_run(self,
                 runnable: Union[Callable[[], Any], Callable[[], bool]],
                 until: Optional[Callable[[], bool]] = None,
                 and_stay_for: float = 0.0) -> typing.Self:
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
        :return: self
        """
        timer = Timer(and_stay_for)

        def sub_routine() -> bool:
            res = runnable()
            if until is not None:
                res = until()
                if not res:
                    logging.debug("waiting until condition is reached")

            if res:
                timer_elapsed = timer.elapsed()
                if not timer_elapsed:
                    logging.debug("waiting for timer...")
                return timer_elapsed
            else:
                timer.reset()
                return False

        self.__routine.append(sub_routine)
        return self

    def then_run_runner_from(self,
                             runner_supplier: Callable[[], Runner],
                             until: Optional[Callable[[], bool]] = None) -> typing.Self:
        """
        Run the runner provided by the specified runner_supplier until it is done.
        :param runner_supplier: A callable returning a Runner
        :param until: When provided, the runner is called until it returns true
        :return: self
        """
        runner_pointer = ()
        runner_pointer.runner = None
        def run_runner():
            if runner_pointer.runner is None:
                runner_pointer.runner = runner_supplier()
            runner_pointer.runner.run()
        return self.then_run(run_runner, until)

    def run(self) -> typing.Self:
        """
        Advance the current routine
        :return: self
        """
        logging.debug(f"running subroutine {self.__routine_index + 1}/{len(self.__routine)}")
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
                self.__routine_index = 0 # we are done
                logging.debug("routine finished")
                self.__running = False
            else:
                self.run() # directly start the next routine to avoid idling

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

