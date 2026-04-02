from dataclasses import dataclass
from unittest import TestCase, main

from typing_extensions import override

from rppmcontroller.behavior.CycleStepResult import CycleStepResult
from rppmcontroller.behavior.CycleStepResultEnum import CycleStepResultEnum
from rppmcontroller.machine.MachineConfiguration import MachineConfiguration
from rppmcontroller.machine.Runner import TransitioningMachine, Runner


class RunnerTestSuite(TestCase):
    """Validates that Runner functionality behaves as expected"""

    # TODO Add test cases for:
    #  result setter
    #  then_goto config
    #  then_goto until
    #  then_goto and_stay_for
    #  then_goto or_timeout_after
    #  then_goto without_cloning_config
    #  then_goto with_info
    #  then_run none_function
    #  then_run bool_function
    #  then_run cycle_step_result_function
    #  then_run none_function until_bool
    #  then_run none_function until_cycle_step_result
    #  then_run cycle_step_result_function until_bool
    #  then_run cycle_step_result_function until_cycle_step_result
    #  then_run and_stay_for
    #  then_run or_timeout_after
    #  then_run with_info
    #  then_run_runner_from supplier
    #  then_run_runner_from until_bool
    #  then_run_runner_from until_cycle_step_result
    #  then_run_runner_from until_runner_bool
    #  then_run_runner_from until_runner_cycle_step_result
    #  then_run_runner_from or_timeout_after
    #  then_run_runner_from with_info
    #  run single step routine
    #  run multi step routine
    #  run after done
    #  run after termination
    #  bool
    #  call
    #  as_result

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
