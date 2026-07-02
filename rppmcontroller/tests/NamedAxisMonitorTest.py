import unittest

from rppmcontroller.machine.Axis import Axis, AxisType
from rppmcontroller.machine.AxisMonitor import NamedAxisMonitor, Deviation
from rppmcontroller.machine.MachineParameters import AxisMonitorParameters

CYCLES_TO_MONITOR = 30


class NamedAxisMonitorTestSuite(unittest.TestCase):
    def setUp(self):
        parameters = AxisMonitorParameters(cycles_to_monitor=CYCLES_TO_MONITOR,
                                           cycles_to_penalize=10,
                                           required_cycles_to_average=10,
                                           minor_deviation_penalty=1,
                                           major_deviation_penalty=2,
                                           penalty_threshold=10,
                                           movement_tolerance=1,
                                           measurement_interval=0)
        axis = Axis(AxisType.Counter, 0)
        self.monitor = NamedAxisMonitor.new(parameters, axis, "test axis")

    def test_calculate_highest_total_penalty(self):
        monitor = self.monitor

        self.assertEqual(20, monitor.calculate_maximum_possible_penalty())

        # one step more is required since the first step wouldn't be a high deviation
        for step in range(CYCLES_TO_MONITOR + 1):
            monitor.record(100)

        deviations = monitor.monitor.get_recorded_deviations()
        self.assertEqual(CYCLES_TO_MONITOR, len(deviations))
        self.assertTrue(all(deviation is Deviation.HIGH for deviation in deviations))
        self.assertEqual(20, monitor.calculate_total_penalty())

if __name__ == '__main__':
    unittest.main()
