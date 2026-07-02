from __future__ import annotations

import random
import unittest
from time import sleep

from rppmcontroller.machine.AxisMonitor import AxisMonitor, Deviation
from rppmcontroller.machine.Timer import Timer


class AxisMonitorTestSuite(unittest.TestCase):

    def setUp(self):
        self.monitor = AxisMonitor(10, 5, 10)
        random.seed("test_record")

    def tearDown(self):
        random.seed(None)
        Timer.custom_current_time = None

    def test_record(self):
        monitor = self.monitor
        counter = 0

        for step in range(100):
            self.assertEqual(Deviation.NONE,
                             monitor.record(counter, 100),
                             f"Unexpected deviation in step {step}")
            counter += random.choice([-1, 1]) * 50 + random.randint(-10, 10)

    def test_record_pwm_change(self):
        monitor = self.monitor
        counter = 0

        for step in range(50):
            self.assertEqual(Deviation.NONE,
                             monitor.record(counter, 100),
                             f"Unexpected deviation in step {step}")
            counter += random.choice([-1, 1]) * 50 + random.randint(-10, 10)

        for step in range(50, 100):
            self.assertEqual(Deviation.NONE,
                             monitor.record(counter, 50),
                             f"Unexpected deviation in step {step}")
            counter += random.choice([-1, 1]) * 25 + random.randint(-10, 10)

    def test_detect_small_deviation(self):
        monitor = self.monitor
        counter = 0

        for step in range(100):
            counter += random.choice([-1, 1]) * 50 + random.randint(-10, 10)
            self.assertEqual(Deviation.NONE, monitor.record(counter, 100))

        self.assertEqual(Deviation.SMALL, monitor.record(counter + 20, 100))

    def test_detect_high_deviation(self):
        monitor = self.monitor
        counter = 0

        for step in range(100):
            counter += random.choice([-1, 1]) * 50 + random.randint(-10, 10)
            self.assertEqual(Deviation.NONE, monitor.record(counter, 100))

        self.assertEqual(Deviation.HIGH, monitor.record(counter + 9, 100))

    def test_measurement_delay(self):
        monitor = AxisMonitor(30, 10, 3, 0.2)
        Timer.custom_current_time = 0

        self.assertTrue(monitor.is_ready_to_record())
        monitor.record(0, 100)

        self.assertEqual(0, get_last_recorded_counter_value(monitor))

        for _ in range(30):
            self.assertFalse(monitor.is_ready_to_record())
            monitor.record(1, 100)
            sleep(0.1)

        self.assertEqual(0, get_last_recorded_counter_value(monitor))

        Timer.custom_current_time = 0.2
        self.assertTrue(monitor.is_ready_to_record())
        monitor.record(2, 100)
        self.assertEqual(2, get_last_recorded_counter_value(monitor))

        self.assertFalse(monitor.is_ready_to_record())
        monitor.record(3, 100)
        self.assertEqual(2, get_last_recorded_counter_value(monitor))


def get_last_recorded_counter_value(monitor: AxisMonitor) -> int | None:
    last_recorded_data = monitor.last_recorded_data
    return last_recorded_data.counter_value if (last_recorded_data is not
                                                None) else None


if __name__ == '__main__':
    unittest.main()
