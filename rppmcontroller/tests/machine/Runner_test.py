from __future__ import annotations

from dataclasses import dataclass
from unittest import TestCase, main

from typing_extensions import override

from rppmcontroller.behavior.CycleStepResult import CycleStepResult
from rppmcontroller.behavior.CycleStepResultEnum import CycleStepResultEnum
from rppmcontroller.machine.MachineConfiguration import MachineConfiguration
from rppmcontroller.machine.Runner import TransitioningMachine, Runner
from rppmcontroller.machine.Timer import Timer
from rppmcontroller.protocol.decoratorFunctions import \
    protocol_command_function


class RunnerTestSuite(TestCase):
    """Validates that Runner functionality behaves as expected"""

    def setUp(self):
        self.machine = TestTransitioningMachine()
        self.runner = self.machine.create_runner()

    def tearDown(self):
        Timer.custom_current_time = None

    def run_post_config_checks(self, expected_routine_length: int = 1) -> None:
        """
        Validates that the runner has the correct number of subroutines and is
        not running yet
        :param expected_routine_length: The expected number of subroutines
        :return: None
        """
        runner = self.runner

        self.assertEqual(expected_routine_length, len(runner._routine))
        self.assertEqual(False,
                         runner.running,
                         "Runner hasn't been started yet")

    def test_init(self):
        """Tests initial properties after runner creation"""
        runner = self.runner
        self.assertEqual(CycleStepResultEnum.MUST_CONTINUE,
                         runner.result,
                         "Runner should continue after initialization")
        self.assertEqual(0, len(runner._routine), "No default routine")
        self.assertEqual(False, runner.running, "Runner should not be running")
        self.assertEqual(False,
                         runner.status_published,
                         "Status should not be published yet")

    def test_set_same_result(self):
        """Validates that setting the same result doesn't re-publish the
        status"""
        runner = self.runner
        runner.result = CycleStepResultEnum.MUST_CONTINUE
        runner.status_published = True

        runner.result = CycleStepResultEnum.MUST_CONTINUE
        self.assertEqual(CycleStepResultEnum.MUST_CONTINUE, runner.result)
        self.assertEqual(True,
                         runner.status_published,
                         "Status hasn't changed")

    def test_set_different_result(self):
        """Validates that setting a different result re-publishes the status"""
        runner = self.runner
        runner.status_published = True

        runner.result = CycleStepResultEnum.DONE
        self.assertEqual(CycleStepResultEnum.DONE, runner.result)
        self.assertEqual(False, runner.status_published, "Status changed")

    def test_then_goto_config(self):
        """Tests that going to a certain configuration works"""
        runner = self.runner
        config = TestConfig(5)
        runner.then_goto(config)
        self.run_post_config_checks()

        for step in range(5):
            result = runner.run()
            self.assertEqual(CycleStepResultEnum.MUST_CONTINUE,
                             result.result,
                             f"Unexpected result in step {step}")
            self.assertEqual(True, runner.running, "Runner is running")

        result = runner.run()
        self.assertEqual(CycleStepResultEnum.DONE,
                         result.result,
                         "Runner should be done")
        self.assertEqual(False, runner.running, "Runner is done")
        self.assertEqual(5,
                         self.machine.value,
                         "Value should have been reached")

    def test_then_goto_until_bool(self):
        """Tests going to a certain configuration until a condition is met"""
        runner = self.runner
        config = TestConfig(5)
        runner.then_goto(config, until=lambda: self.machine.value == 3)

        self.run_post_config_checks()

        for step in range(3):
            result = runner.run()
            self.assertEqual(CycleStepResultEnum.MUST_CONTINUE,
                             result.result,
                             f"Unexpected result in step {step}")
            self.assertEqual(True, runner.running, "Runner is running")

        result = runner.run()
        self.assertEqual(CycleStepResultEnum.DONE, result.result)
        self.assertEqual(False, runner.running, "Runner is done")
        self.assertEqual(3,
                         self.machine.value,
                         "Value should have been reached")

    def test_then_goto_until_done(self):
        """Tests going to a certain configuration until a DONE is sent"""
        runner = self.runner
        config = TestConfig(5)
        until_done = lambda: CycleStepResult.done() if (self.machine.value ==
                                                        3) \
            else CycleStepResult(CycleStepResultEnum.MUST_CONTINUE)
        runner.then_goto(config, until=until_done)

        self.run_post_config_checks()

        for step in range(3):
            result = runner.run()
            self.assertEqual(CycleStepResultEnum.MUST_CONTINUE,
                             result.result,
                             f"Unexpected result in step {step}")
            self.assertEqual(True, runner.running, "Runner is running")

        result = runner.run()
        self.assertEqual(CycleStepResultEnum.DONE, result.result)
        self.assertEqual(False, runner.running, "Runner is done")
        self.assertEqual(3,
                         self.machine.value,
                         "Value should have been reached")

    def test_then_goto_until_abort(self):
        """Tests going to a certain configuration until an abort is sent"""
        runner = self.runner
        config = TestConfig(5)
        until_abort = lambda: CycleStepResult(
            CycleStepResultEnum.ABORTED_ERROR) if self.machine.value == 3 \
            else CycleStepResult(CycleStepResultEnum.MUST_CONTINUE)
        runner.then_goto(config, until=until_abort)

        self.run_post_config_checks()

        for step in range(3):
            result = runner.run()
            self.assertEqual(CycleStepResultEnum.MUST_CONTINUE,
                             result.result,
                             f"Unexpected result in step {step}")
            self.assertEqual(True, runner.running, "Runner is running")

        result = runner.run()
        self.assertEqual(CycleStepResultEnum.ABORTED_ERROR, result.result)
        self.assertEqual(False, runner.running, "Runner is aborted")
        self.assertEqual(3,
                         self.machine.value,
                         "Value should have advanced")

    def test_then_goto_and_stay_for(self):
        """Tests going to a certain configuration and staying for a certain
        amount of time"""
        runner = self.runner
        Timer.custom_current_time = 0

        config = TestConfig(5)
        runner.then_goto(config, and_stay_for=3)

        self.run_post_config_checks()

        for step in range(8):
            result = runner.run()
            Timer.custom_current_time += 1
            self.assertEqual(CycleStepResultEnum.MUST_CONTINUE,
                             result.result,
                             f"Unexpected result in step {step}")
            self.assertEqual(True, runner.running, "Runner is running")

        result = runner.run()
        self.assertEqual(CycleStepResultEnum.DONE, result.result)
        self.assertEqual(False, runner.running, "Runner is done")
        self.assertEqual(5,
                         self.machine.value,
                         "Value should have been reached")

    def test_then_goto_or_timeout_after(self):
        """Tests timing out after a certain amount of time"""
        runner = self.runner
        Timer.custom_current_time = 0

        config = TestConfig(5)
        runner.then_goto(config, or_timeout_after=3)
        self.run_post_config_checks()

        for step in range(3):
            result = runner.run()
            Timer.custom_current_time += 1
            self.assertEqual(CycleStepResultEnum.MUST_CONTINUE,
                             result.result,
                             f"Unexpected result in step {step}")
            self.assertEqual(True, runner.running, "Runner is running")

        result = runner.run()
        self.assertEqual(CycleStepResultEnum.ABORTED_TIMEOUT, result.result)
        self.assertEqual(False, runner.running, "Runner is aborted")
        self.assertEqual(3,
                         self.machine.value,
                         "Value should have advanced")

    def test_then_goto_with_cloning_config(self):
        """Validates that the runner clones the configuration by default"""
        runner = self.runner
        config = TestConfig(5)
        runner.then_goto(config)

        config.value = 3

        self.run_post_config_checks()

        for step in range(5):
            result = runner.run()
            self.assertEqual(CycleStepResultEnum.MUST_CONTINUE,
                             result.result,
                             f"Unexpected result in step {step}")
            self.assertEqual(True, runner.running, "Runner is running")
            self.assertEqual(3,
                             config.value,
                             "Config value should not have changed")

        result = runner.run()
        self.assertEqual(CycleStepResultEnum.DONE, result.result)
        self.assertEqual(False, runner.running, "Runner is done")
        self.assertEqual(5,
                         self.machine.value,
                         "Original value should have been reached")

    def test_then_goto_without_cloning_config(self):
        """Validates that not cloning the config can update values on the
        fly"""
        runner = self.runner
        config = TestConfig(5)
        runner.then_goto(config, clone_config=False)

        config.value = 3

        self.run_post_config_checks()

        for step in range(3):
            result = runner.run()
            self.assertEqual(CycleStepResultEnum.MUST_CONTINUE,
                             result.result,
                             f"Unexpected result in step {step}")
            self.assertEqual(True, runner.running, "Runner is running")

        result = runner.run()
        self.assertEqual(CycleStepResultEnum.DONE, result.result)
        self.assertEqual(False, runner.running, "Runner is done")
        self.assertEqual(3,
                         self.machine.value,
                         "Original value should have been reached")

    # TODO Add test cases for:
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
    #  then_run_runner_from until_done
    #  then_run_runner_from until_abort
    #  then_run_runner_from until_runner_bool
    #  then_run_runner_from until_runner_done
    #  then_run_runner_from until_runner_abort
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
class TestConfig(MachineConfiguration):
    value: int = 0


class TestTransitioningMachine(TransitioningMachine[TestConfig]):
    def __init__(self):
        super().__init__()
        self.value = 0

    @property
    def increment_result(self) -> CycleStepResult:
        return CycleStepResult(CycleStepResultEnum.MUST_CONTINUE,
                               "incrementing")

    @property
    def decrement_result(self) -> CycleStepResult:
        return CycleStepResult(CycleStepResultEnum.MUST_CONTINUE,
                               "decrementing")

    @override
    def goto_config_CycleStep(self, config: TestConfig) -> CycleStepResult:
        if self.value < config.value:
            self.value += 1
            return self.increment_result
        elif self.value > config.value:
            self.value -= 1
            return self.decrement_result
        else:
            return CycleStepResult.done()

    @protocol_command_function(description="Goes to the given target value")
    def goto_value_Command(self, target_value: int) -> Runner:
        runner = self.create_runner()
        config = TestConfig(value=target_value)
        runner.then_goto(config)
        return runner


if __name__ == '__main__':
    main()
