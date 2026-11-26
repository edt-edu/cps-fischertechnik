import unittest

from rppmcontroller.machine.Axis import Axis, AxisType, \
    OvershootLimitExceededError
from rppmcontroller.machine.AxisConfig import AxisConfig
from rppmcontroller.machine.MachineParameters import AxisParameters
from rppmcontroller.utils.PlusMinusStop import PlusMinusStop


class AxisTestCase(unittest.TestCase):
    def setUp(self):
        counterTypePlay = Axis(AxisType.Counter, 0).play
        if counterTypePlay > 10:
            self.fail("Test is not made for such large play values!")
        tolerance = 10 - counterTypePlay
        self.axis = Axis(AxisType.Counter, tolerance)

    def testUpdate(self):
        """tests, that setter for counterInput works"""
        self.assertEqual(0, self.axis.counterinput)

        self.axis.update(False, 10)
        self.assertEqual(10, self.axis.counterinput)

    def testCounter(self):
        """Tests outputs of the goToConfig function in combination with counterInputs"""
        self.assertEqual(0, self.axis.counterinput)

        self.axis.update(False, 10)
        # values for outputplus/minus first set (no execution of counter func here)
        feedback = self.axis.aim_for(AxisConfig.to_counter_goal(100))
        self.assertEqual(False, feedback.target_reached)
        self.assertEqual(0, self.axis.counterValueCurrent)
        self.assertEqual(True, self.axis.outputplus)
        self.assertEqual(False, self.axis.outputminus)

        self.axis.update(False, 11)
        # first execution of counter func (11 set as zero point)
        feedback = self.axis.aim_for(AxisConfig.to_counter_goal(100))
        self.assertEqual(False, feedback.target_reached)
        self.assertEqual(0, self.axis.counterValueCurrent)
        self.assertEqual(True, self.axis.outputplus)
        self.assertEqual(False, self.axis.outputminus)

        self.axis.update(False, 100)
        feedback = self.axis.aim_for(AxisConfig.to_counter_goal(100))
        self.assertEqual(False, feedback.target_reached)
        self.assertEqual(89, self.axis.counterValueCurrent)
        self.assertEqual(True, self.axis.outputplus)
        self.assertEqual(False, self.axis.outputminus)

        self.axis.update(False, 130)
        feedback = self.axis.aim_for(AxisConfig.to_counter_goal(100))
        self.assertEqual(False, feedback.target_reached)
        self.assertEqual(119, self.axis.counterValueCurrent)
        self.assertEqual(False, self.axis.outputplus)
        self.assertEqual(True, self.axis.outputminus)

        self.axis.update(False, 140)
        feedback = self.axis.aim_for(AxisConfig.to_counter_goal(100))
        self.assertEqual(True, feedback.target_reached)
        self.assertEqual(109, self.axis.counterValueCurrent)
        self.assertEqual(False, self.axis.outputplus)
        self.assertEqual(False, self.axis.outputminus)

    def test_jitter_protection(self):
        """
        Verifies that the axis will abort after overshooting the target three
        times.
        """
        axis = Axis.encoder(AxisParameters(allowed_overshoots=3))
        target = AxisConfig.to_counter_goal(100)

        # initial target set, no overshoot here
        axis.update(True, 0)
        feedback = axis.aim_for(target)

        self.assertEqual(False, feedback.target_reached)
        self.assertEqual(PlusMinusStop.PLUS, feedback.direction)
        self.assertIsNone(feedback.error)

        # overshoot by 30
        axis.update(False, 130)
        feedback = axis.aim_for(target)

        self.assertEqual(False, feedback.target_reached)
        self.assertEqual(PlusMinusStop.MINUS, feedback.direction)
        self.assertIsNone(feedback.error)

        # overshoot by -20
        axis.update(False, 80)
        feedback = axis.aim_for(target)

        self.assertEqual(False, feedback.target_reached)
        self.assertEqual(PlusMinusStop.PLUS, feedback.direction)
        self.assertIsNone(feedback.error)

        # overshoot by 10
        axis.update(False, 110)
        feedback = axis.aim_for(target)

        self.assertEqual(False, feedback.target_reached)
        self.assertEqual(PlusMinusStop.MINUS, feedback.direction)
        self.assertIsNone(feedback.error)

        # overshoot by -10 and expect error
        axis.update(False, 90)
        feedback = axis.aim_for(target)

        self.assertEqual(False, feedback.target_reached)
        self.assertEqual(PlusMinusStop.STOP, feedback.direction)
        self.assertIsNotNone(feedback.error)
        self.assertIsInstance(feedback.error, OvershootLimitExceededError)


if __name__ == '__main__':
    unittest.main()
