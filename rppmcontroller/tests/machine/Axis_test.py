import unittest

from rppmcontroller.machine.Axis import Axis, AxisType


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
        t = self.axis.gotoConfig(False, 100)
        self.assertIsInstance(t, tuple)
        self.assertEqual(False, t[0])
        self.assertEqual(0, self.axis.counterValueCurrent)
        self.assertEqual(True, self.axis.outputplus)
        self.assertEqual(False, self.axis.outputminus)

        self.axis.update(False, 11)
        # first execution of counter func (11 set as zero point)
        t = self.axis.gotoConfig(False, 100)
        self.assertIsInstance(t, tuple)
        self.assertEqual(False, t[0])
        self.assertEqual(0, self.axis.counterValueCurrent)
        self.assertEqual(True, self.axis.outputplus)
        self.assertEqual(False, self.axis.outputminus)

        self.axis.update(False, 100)
        t = self.axis.gotoConfig(False, 100)
        self.assertIsInstance(t, tuple)
        self.assertEqual(False, t[0])
        self.assertEqual(89, self.axis.counterValueCurrent)
        self.assertEqual(True, self.axis.outputplus)
        self.assertEqual(False, self.axis.outputminus)

        self.axis.update(False, 130)
        t = self.axis.gotoConfig(False, 100)
        self.assertIsInstance(t, tuple)
        self.assertEqual(False, t[0])
        self.assertEqual(119, self.axis.counterValueCurrent)
        self.assertEqual(False, self.axis.outputplus)
        self.assertEqual(True, self.axis.outputminus)

        self.axis.update(False, 140)
        t = self.axis.gotoConfig(False, 100)
        self.assertIsInstance(t, tuple)
        self.assertEqual(True, t[0])
        self.assertEqual(109, self.axis.counterValueCurrent)
        self.assertEqual(False, self.axis.outputplus)
        self.assertEqual(False, self.axis.outputminus)


if __name__ == '__main__':
    unittest.main()
