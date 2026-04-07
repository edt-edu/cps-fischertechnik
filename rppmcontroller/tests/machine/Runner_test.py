from __future__ import annotations

from dataclasses import dataclass
from unittest import TestCase, main

from typing_extensions import override

from rppmcontroller.behavior.CycleStepResult import CycleStepResult
from rppmcontroller.behavior.CycleStepResultEnum import CycleStepResultEnum

ABORT_RESULT = CycleStepResult(CycleStepResultEnum.ABORTED_ERROR)
from rppmcontroller.machine.MachineConfiguration import MachineConfiguration
from rppmcontroller.machine.Runner import TransitioningMachine, Runner
from rppmcontroller.machine.Timer import Timer
from rppmcontroller.protocol.decoratorFunctions import \
    protocol_command_function

MUST_CONTINUE_RESULT = CycleStepResult(CycleStepResultEnum.MUST_CONTINUE)


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
        self.assertEqual(CycleStepResultEnum.DONE,
                         result.result,
                         "Runner should be done")
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
            else MUST_CONTINUE_RESULT
        runner.then_goto(config, until=until_done)

        self.run_post_config_checks()

        for step in range(3):
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
        self.assertEqual(3,
                         self.machine.value,
                         "Value should have been reached")

    def test_then_goto_until_abort(self):
        """Tests going to a certain configuration until an abort is sent"""
        runner = self.runner
        config = TestConfig(5)
        until_abort = lambda: CycleStepResult(
            CycleStepResultEnum.ABORTED_ERROR) if self.machine.value == 3 \
            else MUST_CONTINUE_RESULT
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
        self.assertEqual(CycleStepResultEnum.DONE,
                         result.result,
                         "Runner should be done")
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
        self.assertEqual(CycleStepResultEnum.DONE,
                         result.result,
                         "Runner should be done")
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
        self.assertEqual(CycleStepResultEnum.DONE,
                         result.result,
                         "Runner should be done")
        self.assertEqual(False, runner.running, "Runner is done")
        self.assertEqual(3,
                         self.machine.value,
                         "Original value should have been reached")

    def test_then_goto_with_info(self):
        """Tests that the runner can show custom info on its status"""
        runner = self.runner
        for i in range(1, 4):
            runner.then_goto(TestConfig(i), info=f"going to {i}")
        self.run_post_config_checks(expected_routine_length=3)

        for step in range(3):
            result = runner.run()
            self.assertEqual(CycleStepResultEnum.MUST_CONTINUE,
                             result.result,
                             f"Unexpected result in step {step}")
            self.assertEqual(True, runner.running, "Runner is running")
            expected_info = f"going to {step + 1}"
            actual_info = result.info
            self.assertEqual(True, expected_info in actual_info,
                             f"'{expected_info}' should be in '"
                             f"{actual_info}' (step: {step})")

        result = runner.run()
        self.assertEqual(CycleStepResultEnum.DONE,
                         result.result,
                         "Runner should be done")
        self.assertEqual(False, runner.running, "Runner is done")
        self.assertEqual(3,
                         self.machine.value,
                         "Final value should have been reached")

    def test_then_run_none_function(self):
        """Tests that running a function which returns None fails"""
        runner = self.runner
        runner.then_run(lambda: None)
        self.run_post_config_checks()

        try:
            runner.run()
            self.fail(
                "Running a None lambda without an until condition should fail")
        except ValueError:
            pass

    def test_then_run_bool_function(self):
        """Tests that running a function that returns a bool is run until it
        returns `False`"""
        runner = self.runner
        runner.then_run(lambda: self.goto_step().is_done())
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

    def test_then_run_cycle_step_result_function(self):
        """Tests that running a function that returns a `CycleStepResult` is
        run until it returns `DONE`"""
        runner = self.runner
        runner.then_run(lambda: self.goto_step())
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

    def test_then_run_none_function_until_bool(self):
        """Test that running a function that returns `None` is executed until
        it's until-function returns `True`"""
        runner = self.runner

        def increment():
            self.machine.value += 1

        runner.then_run(increment, until=lambda: self.machine.value == 5)
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

    def test_then_run_none_function_until_done(self):
        """Tests that running a function that returns `None` is executed until
        it's until-function returns `DONE`"""
        runner = self.runner

        def increment():
            self.machine.value += 1

        runner.then_run(increment,
                        until=lambda: CycleStepResult.done() if
                        self.machine.value == 5 else MUST_CONTINUE_RESULT)
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

    def test_then_run_done_function_until_bool(self):
        """Tests that running a function that returns `DONE` is executed until
        it's until-function returns `True`"""
        runner = self.runner

        def increment():
            self.machine.value += 1
            return CycleStepResult.done()

        runner.then_run(increment, until=lambda: self.machine.value == 5)
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

    def test_then_run_abort_function_until_bool(self):
        """Tests that running a function that returns `ABORTED_ERROR` is
        executed until it's until-function returns `True`"""
        runner = self.runner

        def increment():
            self.machine.value += 1
            return CycleStepResult(CycleStepResultEnum.ABORTED_ERROR)

        runner.then_run(increment, until=lambda: self.machine.value == 5)
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

    def test_then_run_done_function_until_done(self):
        """Tests that running a function that returns `DONE` is executed until
        it's until-function returns `DONE`"""
        runner = self.runner

        def increment():
            self.machine.value += 1
            return CycleStepResult.done()

        runner.then_run(increment,
                        until=lambda: CycleStepResult.done() if
                        self.machine.value == 5 else MUST_CONTINUE_RESULT)
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

    def test_then_run_abort_function_until_done(self):
        """Tests that running a function that returns `ABORTED_ERROR` is
        executed until it's until-function returns `DONE`"""
        runner = self.runner

        def increment():
            self.machine.value += 1
            return CycleStepResult(CycleStepResultEnum.ABORTED_ERROR)

        runner.then_run(increment,
                        until=lambda: CycleStepResult.done() if
                        self.machine.value == 5 else MUST_CONTINUE_RESULT)
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

    def test_then_run_and_stay_for(self):
        """Tests that running a function that returns `True` will be executed
        for some more seconds"""
        runner = self.runner
        Timer.custom_current_time = 0

        runner.then_run(lambda: True, and_stay_for=3)
        self.run_post_config_checks()

        for step in range(3):
            result = runner.run()
            Timer.custom_current_time += 1
            self.assertEqual(CycleStepResultEnum.MUST_CONTINUE,
                             result.result,
                             f"Unexpected result in step {step}")
            self.assertEqual(True, runner.running, "Runner is running")

        result = runner.run()
        self.assertEqual(CycleStepResultEnum.DONE,
                         result.result,
                         "Runner should be done")
        self.assertEqual(False, runner.running, "Runner is done")

    def test_then_run_or_timeout_after(self):
        """Tests that a function that returns `False` will be executed until
        timeout"""
        runner = self.runner
        Timer.custom_current_time = 0

        runner.then_run(lambda: False, or_timeout_after=5)
        self.run_post_config_checks()

        for step in range(5):
            result = runner.run()
            Timer.custom_current_time += 1
            self.assertEqual(CycleStepResultEnum.MUST_CONTINUE,
                             result.result,
                             f"Unexpected result in step {step}")
            self.assertEqual(True, runner.running, "Runner is running")

        result = runner.run()
        self.assertEqual(CycleStepResultEnum.ABORTED_TIMEOUT,
                         result.result,
                         "Runner should have aborted")
        self.assertEqual(False, runner.running, "Runner is aborted")

    def test_then_run_with_info(self):
        """Tests that the runner can show custom info on its status"""
        runner = self.runner
        for i in range(1, 4):
            # copy i into target at lambda creation to avoid late-referencing
            runner.then_run(lambda target=i: self.goto_step(target),
                            info=f"going to {i}")
        self.run_post_config_checks(expected_routine_length=3)

        for step in range(3):
            result = runner.run()
            self.assertEqual(CycleStepResultEnum.MUST_CONTINUE,
                             result.result,
                             f"Unexpected result in step {step}")
            self.assertEqual(True, runner.running, "Runner is running")
            expected_info = f"going to {step + 1}"
            actual_info = result.info
            self.assertEqual(True, expected_info in actual_info,
                             f"'{expected_info}' should be in '"
                             f"{actual_info}' (step: {step})")

        result = runner.run()
        self.assertEqual(CycleStepResultEnum.DONE,
                         result.result,
                         "Runner should be done")
        self.assertEqual(False, runner.running, "Runner is done")
        self.assertEqual(3,
                         self.machine.value,
                         "Final value should have been reached")

    def test_then_run_runner_from_supplier(self):
        """Tests that running a `Runner` provided by a supplier works"""
        runner = self.runner

        runner.then_run_runner_from(lambda: self.machine.goto_value_Command(5))
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

    def test_then_run_runner_from_until_bool(self):
        """Tests that a `Runner` provided by a supplier is called until its
        until-function returns `True`"""
        runner = self.runner

        runner.then_run_runner_from(lambda: self.machine.goto_value_Command(5),
                                    until=lambda: self.machine.value == 3)
        self.run_post_config_checks()

        for step in range(3):
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
        self.assertEqual(3,
                         self.machine.value,
                         "Value should have advanced")

    def test_then_run_runner_from_until_done(self):
        """Tests that a `Runner` provided by a supplier is called until its
        until-function returns `DONE`"""
        runner = self.runner

        runner.then_run_runner_from(lambda: self.machine.goto_value_Command(5),
                                    until=lambda: CycleStepResult.done() if
                                    self.machine.value == 3 else
                                    MUST_CONTINUE_RESULT)
        self.run_post_config_checks()

        for step in range(3):
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
        self.assertEqual(3,
                         self.machine.value,
                         "Value should have advanced")

    def test_then_run_runner_from_until_abort(self):
        """Tests that a `Runner` provided by a supplier is called until its
        until-function returns `ABORTED_ERROR`"""
        runner = self.runner

        runner.then_run_runner_from(lambda: self.machine.goto_value_Command(5),
                                    until=lambda: ABORT_RESULT if
                                    self.machine.value == 3 else
                                    MUST_CONTINUE_RESULT)
        self.run_post_config_checks()

        for step in range(3):
            result = runner.run()
            self.assertEqual(CycleStepResultEnum.MUST_CONTINUE,
                             result.result,
                             f"Unexpected result in step {step}")
            self.assertEqual(True, runner.running, "Runner is running")

        result = runner.run()
        self.assertEqual(CycleStepResultEnum.ABORTED_ERROR,
                         result.result,
                         "Runner should be aborted")
        self.assertEqual(False, runner.running, "Runner is aborted")
        self.assertEqual(3,
                         self.machine.value,
                         "Value should have advanced")

    def test_then_run_runner_from_until_runner_bool(self):
        """Tests that a `Runner` provided by a supplier is called until its
        until-function that consumes that runner returns `True`"""
        runner = self.runner

        def until(rnr: Runner) -> bool:
            self.assertEqual(2,
                             len(rnr._routine),
                             "command runner should have two subroutines")
            return self.machine.value == 3

        runner.then_run_runner_from(lambda: self.machine.goto_value_Command(5),
                                    until=until)
        self.run_post_config_checks()

        for step in range(3):
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
        self.assertEqual(3,
                         self.machine.value,
                         "Value should have been reached")

    def test_then_run_runner_from_until_runner_done(self):
        """Tests that a `Runner` provided by a supplier is called until its
        until-function that consumes that runner returns `DONE`"""
        runner = self.runner

        def until(rnr: Runner) -> CycleStepResult:
            self.assertEqual(2,
                             len(rnr._routine),
                             "command runner should have two subroutines")
            if self.machine.value == 3:
                return CycleStepResult.done()
            else:
                return MUST_CONTINUE_RESULT

        runner.then_run_runner_from(lambda: self.machine.goto_value_Command(5),
                                    until=until)
        self.run_post_config_checks()

        for step in range(3):
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
        self.assertEqual(3,
                         self.machine.value,
                         "Value should have been reached")

    def test_then_run_runner_from_until_runner_abort(self):
        """Tests that a `Runner` provided by a supplier is called until its
        until-function that consumes that runner returns `ABORTED_ERROR`"""
        runner = self.runner

        def until(rnr: Runner) -> CycleStepResult:
            self.assertEqual(2,
                             len(rnr._routine),
                             "command runner should have two subroutines")
            if self.machine.value == 3:
                return ABORT_RESULT
            else:
                return MUST_CONTINUE_RESULT

        runner.then_run_runner_from(lambda: self.machine.goto_value_Command(5),
                                    until=until)
        self.run_post_config_checks()

        for step in range(3):
            result = runner.run()
            self.assertEqual(CycleStepResultEnum.MUST_CONTINUE,
                             result.result,
                             f"Unexpected result in step {step}")
            self.assertEqual(True, runner.running, "Runner is running")

        result = runner.run()
        self.assertEqual(CycleStepResultEnum.ABORTED_ERROR,
                         result.result,
                         "Runner should be aborted")
        self.assertEqual(False, runner.running, "Runner is aborted")
        self.assertEqual(3,
                         self.machine.value,
                         "Value should have advanced")

    def test_then_run_runner_from_or_timeout_after(self):
        """Tests that a `Runner` provided by a supplier will be timed out
        after a few seconds"""
        runner = self.runner
        Timer.custom_current_time = 0

        runner.then_run_runner_from(lambda: self.machine.goto_value_Command(5),
                                    or_timeout_after=3)
        self.run_post_config_checks()

        for step in range(3):
            result = runner.run()
            Timer.custom_current_time += 1
            self.assertEqual(CycleStepResultEnum.MUST_CONTINUE,
                             result.result,
                             f"Unexpected result in step {step}")
            self.assertEqual(True, runner.running, "Runner is running")

        result = runner.run()
        self.assertEqual(CycleStepResultEnum.ABORTED_TIMEOUT,
                         result.result,
                         "Runner should be aborted")
        self.assertEqual(False, runner.running, "Runner is aborted")
        self.assertEqual(3,
                         self.machine.value,
                         "Value should have advanced")

    def test_then_run_runner_from_with_info(self):
        """Tests that a `Runner` provided by a supplier can show custom info"""
        runner = self.runner
        for i in range(1, 4):
            # copy i into target at lambda creation to avoid late-referencing
            runner.then_run_runner_from(lambda target=i:
                                        self.machine.goto_value_Command(
                                            target),
                                        info=f"going to {i}")
        self.run_post_config_checks(expected_routine_length=3)

        for step in range(3):
            result = runner.run()
            self.assertEqual(CycleStepResultEnum.MUST_CONTINUE,
                             result.result,
                             f"Unexpected result in step {step}")
            self.assertEqual(True, runner.running, "Runner is running")
            expected_info = f"going to {step + 1}"
            actual_info = result.info
            self.assertEqual(True, expected_info in actual_info,
                             f"'{expected_info}' should be in '"
                             f"{actual_info}' (step: {step})")

        result = runner.run()
        self.assertEqual(CycleStepResultEnum.DONE,
                         result.result,
                         "Runner should be done")
        self.assertEqual(False, runner.running, "Runner is done")
        self.assertEqual(3,
                         self.machine.value,
                         "Final value should have been reached")

    # TODO Add test cases for:
    #  run zero step routine
    #  run single step routine
    #  run multi step routine
    #  run after done
    #  run after termination
    #  bool
    #  call
    #  as_result

    def goto_step(self, target_value: int = 5) -> CycleStepResult:
        return self.machine.goto_config_CycleStep(TestConfig(
            value=target_value))


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
        # go to the config twice to have a command which has two
        # subroutine steps
        runner.then_goto(config)
        return runner


if __name__ == '__main__':
    main()
