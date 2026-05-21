import random
import unittest

from rppmcontroller.machine.AxisMonitor import AxisMonitor, Deviation


class AxisMonitorTestSuite(unittest.TestCase):

    def setUp(self):
        self.monitor = AxisMonitor(5, 3, 10)
        random.seed("test_record")

    def test_record(self):
        monitor = self.monitor
        counter = 0

        for step in range(100):
            self.assertEqual(Deviation.NONE,
                             monitor.record(counter, 100),
                             f"Unexpected deviation in step {step}")
            counter += 50 + random.randint(-10, 10)

    def test_record_pwm_change(self):
        monitor = self.monitor
        counter = 0

        for step in range(50):
            self.assertEqual(Deviation.NONE,
                             monitor.record(counter, 100),
                             f"Unexpected deviation in step {step}")
            counter += 50 + random.randint(-10, 10)

        for step in range(50, 100):
            self.assertEqual(Deviation.NONE,
                             monitor.record(counter, 50),
                             f"Unexpected deviation in step {step}")
            counter += 25 + random.randint(-10, 10)


if __name__ == '__main__':
    unittest.main()
