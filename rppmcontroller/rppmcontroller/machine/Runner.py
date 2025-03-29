from __future__ import annotations

from abc import abstractmethod
from copy import deepcopy
from typing import Any
from typing import Callable, Self, Union
from typing import Optional

from rppmcontroller.machine.MachineConfiguration import MachineConfiguration
from rppmcontroller.machine.Timer import Timer


class TransitioningMachine:
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
        Creates a new Runner for this machine
        :return: A Runner
        """
        return Runner(self)


class Runner:
    """
    Transitions a machine into different configurations.
    """
    __machine: TransitioningMachine
    __routine: [Callable[[], bool]] = []
    __routine_index: int = 0

    def __init__(self, machine: TransitioningMachine):
        """
        Create a new runner for the specified machine.

        :param machine: The machine to control
        """
        self.__machine = machine

    def then_goto(self,
                  config: MachineConfiguration,
                  until: Optional[Callable[[], bool]] = None,
                  and_stay_for: float = 0.0,
                  clone_config: bool = True) -> Self:
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
                 and_stay_for: float = 0.0) -> Self:
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

            if res:
                return timer.elapsed()
            else:
                timer.reset()
                return False

        self.__routine.append(sub_routine)
        return self

    def run(self):
        """
        Advance the current routine
        :return: A pointer to this method
        """
        sub_routine = self.__routine[self.__routine_index]
        # call the sub routine
        res = sub_routine()
        if not isinstance(res, bool):
            raise ValueError(f"sub_routine {sub_routine} was expected to "
                             f"return a bool, but instead returned {res}")

        sub_routine_finished = res
        if sub_routine_finished:
            self.__routine_index = (
                                       self.__routine_index + 1) % len(
                self.__routine)

        return self.run
