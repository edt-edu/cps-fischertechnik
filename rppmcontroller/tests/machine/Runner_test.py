from dataclasses import dataclass
from unittest import TestCase, main

from typing_extensions import override

from rppmcontroller.behavior.CycleStepResult import CycleStepResult
from rppmcontroller.behavior.CycleStepResultEnum import CycleStepResultEnum
from rppmcontroller.machine.MachineConfiguration import MachineConfiguration
from rppmcontroller.machine.Runner import TransitioningMachine, Runner


class RunnerTestSuite(TestCase):
    pass  # TODO add test cases


@dataclass
class GenericTransitioningMachineConfig(MachineConfiguration):
    value = 0


class GenericTransitioningMachine(
    TransitioningMachine[GenericTransitioningMachineConfig]):
    def __init__(self):
        super().__init__()
        self.__current_configuration = GenericTransitioningMachineConfig()

    @override
    def goto_config_CycleStep(self,
                              config: GenericTransitioningMachineConfig) -> (
            CycleStepResult):
        if self.__current_configuration.value < config.value:
            self.__current_configuration.value += 1
            return CycleStepResult(CycleStepResultEnum.MUST_CONTINUE,
                                   "incrementing")
        elif self.__current_configuration.value > config.value:
            self.__current_configuration.value -= 1
            return CycleStepResult(CycleStepResultEnum.MUST_CONTINUE,
                                   "decrementing")
        else:
            return CycleStepResult.done()

    def goto_value(self, target_value: int) -> Runner:
        runner = self.create_runner()
        config = GenericTransitioningMachineConfig()
        config.value = target_value
        runner.then_goto(config)
        return runner


if __name__ == '__main__':
    main()
