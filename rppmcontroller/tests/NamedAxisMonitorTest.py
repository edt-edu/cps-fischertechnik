import unittest

from rppmcontroller.machine.Axis import Axis, AxisType
from rppmcontroller.machine.AxisMonitor import NamedAxisMonitor, Deviation
from rppmcontroller.machine.MachineParameters import AxisMonitorParameters


class NamedAxisMonitorTestSuite(unittest.TestCase):
    def setUp(self):
        parameters = AxisMonitorParameters(cycles_to_monitor=30,
                                           required_cycles_to_average=10,
                                           minor_deviation_penalty=1,
                                           major_deviation_penalty=2,
                                           penalty_threshold=60,
                                           movement_tolerance=1)
        axis = Axis(AxisType.Counter, 0)
        self.monitor = NamedAxisMonitor.new(parameters, axis, "test axis")

    def test_calculate_highest_total_penalty(self):
        monitor = self.monitor
        for step in range(31):  # one step more is required since the first step wouldn't be a high deviation
            monitor.record(100)

        deviations = monitor.monitor.get_recorded_deviations()
        self.assertEqual(30, len(deviations))
        self.assertTrue(all(deviation is Deviation.HIGH for deviation in deviations))
        self.assertEqual(60, monitor.calculate_total_penalty())

if __name__ == '__main__':
    unittest.main()
