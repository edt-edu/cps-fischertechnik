import unittest

from rppmcontroller.machine.MachineParameters import AxisMonitorParameters
from rppmcontroller.machine.vacuumgripper.VacuumGripper import VacuumGripper
from rppmcontroller.machine.vacuumgripper.VacuumGripperParameters import VacuumGripperParameters

APPROACH_PWM = 30
STANDARD_PWM = 100


class VacuumGripperAxisMonitorResetTestCase(unittest.TestCase):
    """The axis monitors judge each command on its own movements.

    Their penalty window (last measurements of an axis) used to survive from one command to the next: the deviations
    recorded at the end of a command still counted for the next one, whose first measurement was moreover compared to
    the last one of the previous command. A command sent right after another one could then abort on its very first
    cycle with "too many deviations".
    """

    def setUp(self):
        monitor_parameters = AxisMonitorParameters(measurement_interval=0)
        self.vgr = VacuumGripper("VacuumGripper01", VacuumGripperParameters(
            vertical_axis_monitor_parameters=monitor_parameters))
        self.vgr.isInitialized = True
        self.vertical = self.vgr._VacuumGripper__vertical_axis_monitor

    def end_previous_command_crawling(self):
        """The end of a command: the vertical axis crawls at approach speed (moves below the movement tolerance)."""
        self.vertical.record(APPROACH_PWM)  # first measurement: nothing to compare with
        for _ in range(4):
            self.assertFalse(self.vertical.record_and_action_is_required(APPROACH_PWM))
        self.assertEqual(8, self.vertical.calculate_total_penalty(), "the previous command ended with 8 points")

    def test_withoutResetTheNextCommandInheritsThePointsAndAbortsOnItsFirstMeasurement(self):
        self.end_previous_command_crawling()

        # the axis has stopped at its target; the next command starts moving it
        self.assertTrue(self.vertical.record_and_action_is_required(STANDARD_PWM))

    def test_aNewCommandStartsWithAnEmptyPenaltyWindow(self):
        self.end_previous_command_crawling()

        self.vgr.on_command_started()

        self.assertFalse(self.vertical.record_and_action_is_required(STANDARD_PWM))
        self.assertEqual(0, self.vertical.calculate_total_penalty())

    def test_aStalledAxisStillAbortsWithinTheCommand(self):
        self.vgr.on_command_started()

        self.vertical.record(STANDARD_PWM)
        results = [self.vertical.record_and_action_is_required(STANDARD_PWM) for _ in range(5)]

        self.assertEqual([False, False, False, False, True], results)


if __name__ == "__main__":
    unittest.main()
