import unittest

from rppmcontroller.machine.Axis import Axis, AxisType
from rppmcontroller.machine.AxisMonitor import NamedAxisMonitor, Deviation
from rppmcontroller.machine.MachineParameters import AxisMonitorParameters
from rppmcontroller.utils.csv import CSVReader

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
                                           measurement_interval=0,
                                           log_to_csv=False)
        axis = Axis(AxisType.Counter, 0)
        self.monitor = NamedAxisMonitor.new(parameters, axis, "test axis")

    def test_calculate_highest_total_penalty(self):
        monitor = self.monitor

        self.assertEqual(20, monitor.calculate_maximum_possible_penalty())

        # one step more is required since the first step wouldn't be a high
        # deviation
        for step in range(CYCLES_TO_MONITOR + 1):
            monitor.record(100)

        deviations = monitor.monitor.get_recorded_deviations()
        self.assertEqual(CYCLES_TO_MONITOR, len(deviations))
        self.assertTrue(all(
            deviation is Deviation.HIGH for deviation in deviations))
        self.assertEqual(20, monitor.calculate_total_penalty())

    def test_penalization_cycle(self):
        monitor = self.monitor
        pwm = 100

        deviation = monitor.record(pwm)
        self.assertEqual(Deviation.NONE, deviation)
        deviation = monitor.record(pwm)
        self.assertEqual(Deviation.HIGH, deviation)
        deviation = monitor.record(pwm)
        self.assertEqual(Deviation.HIGH, deviation)

        self.assertEqual(2 * monitor.parameters.major_deviation_penalty,
                         monitor.calculate_total_penalty())

        for step in range(monitor.parameters.cycles_to_penalize):
            monitor.axis.update(False,
                                monitor.axis.counterinput + 100 *
                                monitor.parameters.movement_tolerance)
            deviation = monitor.record(pwm)
            self.assertEqual(Deviation.NONE,
                             deviation,
                             f"got deviation in step {step}")

        self.assertEqual(0, monitor.calculate_total_penalty())

        monitor.axis.update(False,
                            monitor.axis.counterinput +
                            monitor.parameters.movement_tolerance)
        deviation = monitor.record(pwm)
        self.assertEqual(Deviation.SMALL, deviation)
        self.assertEqual(monitor.parameters.minor_deviation_penalty,
                         monitor.calculate_total_penalty())

        for step in range(monitor.parameters.cycles_to_penalize - 1):
            monitor.axis.update(False,
                                monitor.axis.counterinput + 100 *
                                monitor.parameters.movement_tolerance)
            deviation = monitor.record(pwm)
            self.assertEqual(Deviation.NONE,
                             deviation,
                             f"got deviation in step {step}")
            self.assertEqual(monitor.parameters.minor_deviation_penalty,
                             monitor.calculate_total_penalty())

        monitor.axis.update(False,
                            monitor.axis.counterinput + 100 *
                            monitor.parameters.movement_tolerance)
        deviation = monitor.record(pwm)
        self.assertEqual(Deviation.NONE, deviation)
        self.assertEqual(0, monitor.calculate_total_penalty())

    def test_penalty_calculation_on_real_data(self):
        """
        Attempts to calculate the penalty for the
        I1VacuumGripper01_rotational.csv data set.
        """
        rows = CSVReader("tests/ressources/I1VacuumGripper01_rotational.csv"
                         "").read()
        header = rows[0]
        self.assertEqual(["counter_value",
                          "pwm_value",
                          "moved_distance",
                          "deviation",
                          "total_penalty"],
                         header,
                         "CSV header mismatch")

        parameters = AxisMonitorParameters(log_to_csv=False,
                                           cycles_to_monitor=30,
                                           cycles_to_penalize=10,
                                           minor_deviation_penalty=1,
                                           major_deviation_penalty=2,
                                           measurement_interval=0,
                                           movement_tolerance=3)
        axis = Axis(AxisType.Counter, 0)
        monitor = NamedAxisMonitor.new(parameters,
                                       axis,
                                       "I1VacuumGripper01_rotational")
        for i, row in enumerate(rows[1:]):
            line_nr = i + 2
            axis.update(False, int(row[0]))
            deviation = monitor.record(int(row[1]))

            self.assertEqual(row[3],
                             str(deviation),
                             f"monitor under test calculated a different "
                             f"deviation than monitor in production for row "
                             f"{line_nr}")

            if deviation is not Deviation.NONE:
                self.assertGreater(monitor.calculate_total_penalty(),
                                   0,
                                   f"Encountered deviation should produce a "
                                   f"total penalty greater than zero (row "
                                   f"{line_nr})")


if __name__ == '__main__':
    unittest.main()
