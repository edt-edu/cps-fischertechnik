import unittest

from rppmcontroller.machine.Axis import Axis, AxisType


class AxisTestCase(unittest.TestCase):
    def setUp(self):
        self.axis = Axis(AxisType.Counter, 10)

    def testUpdate(self):
        """tests, that setter for counterinput works"""
        self.assertEqual(self.axis.counterinput, 0)
        self.axis.update(False, 10)
        self.assertEqual(self.axis.counterinput, 10)

    def testCounter(self):
        """Tests outputs of the goToConfig function in combination with counterinputs"""
        self.assertEqual(self.axis.counterinput, 0)

        self.axis.update(False, 10)
        #values for outputplus/minus first set (no execution of counter func here)
        t = self.axis.gotoConfig(False, 100)
        self.assertEqual(t[0], False)
        self.assertEqual(self.axis.counterValueCurrent, 0)
        self.assertEqual(self.axis.outputplus, True)
        self.assertEqual(self.axis.outputminus, False)

        self.axis.update(False, 11)
        #first execution of counter func (11 set as zero point)
        t = self.axis.gotoConfig(False, 100)
        self.assertEqual(t[0], False)
        self.assertEqual(self.axis.counterValueCurrent, 0)
        self.assertEqual(self.axis.outputplus, True)
        self.assertEqual(self.axis.outputminus, False)

        self.axis.update(False, 100)
        t = self.axis.gotoConfig(False, 100)
        self.assertEqual(t[0], False) # TODO find out why this assertion currently fails
        self.assertEqual(self.axis.counterValueCurrent, 89)
        self.assertEqual(self.axis.outputplus, True)
        self.assertEqual(self.axis.outputminus, False)

        self.axis.update(False, 130)
        t = self.axis.gotoConfig(False, 100)
        self.assertEqual(t[0], False)
        self.assertEqual(self.axis.counterValueCurrent,119)
        self.assertEqual(self.axis.outputplus, False)
        self.assertEqual(self.axis.outputminus, True)

        self.axis.update(False, 140)
        t = self.axis.gotoConfig(False, 100)
        self.assertEqual(t[0], True)
        self.assertEqual(self.axis.counterValueCurrent,109)
        self.assertEqual(self.axis.outputplus, False)
        self.assertEqual(self.axis.outputminus, False)

if __name__ == '__main__':
    unittest.main()
