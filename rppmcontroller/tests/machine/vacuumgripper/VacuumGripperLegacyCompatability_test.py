import unittest

from rppmcontroller.machine.MachineParameters import ApproachParameters
from rppmcontroller.machine.vacuumgripper.VacuumGripper import VacuumGripper
from rppmcontroller.machine.vacuumgripper.VacuumGripperParameters import \
    VacuumGripperParameters


# no inspection for deprecation here, since we explicitly test deprecation
# noinspection deprecation
class VacuumGripperLegacyCompatibilityTest(unittest.TestCase):
    def test_legacy_translation(self):
        """
        Tests that legacy parameters are respected
        """
        # this value should have precedence
        parameters = VacuumGripperParameters(pwm_standard_speed=42)
        # this value should be ignored
        parameters.rotational_axis_parameters.pwm_standard_speed = 80

        # this value should have precedence
        parameters.pwm_approach_tolerance = 10

        # this value should be ignored
        self.assertIsInstance(parameters.horizontal_axis_parameters.control_parameters,
                              ApproachParameters)
        parameters.horizontal_axis_parameters.control_parameters.approach_tolerance = 30

        # create a vgr to trigger the translation
        vgr = VacuumGripper("test", parameters)

        self.assertEqual(42, vgr.parameters.pwm_standard_speed)
        self.assertEqual(42,
                         vgr.parameters.vertical_axis_parameters.pwm_standard_speed)
        self.assertEqual(42,
                         vgr.parameters.rotational_axis_parameters.pwm_standard_speed)
        self.assertEqual(42,
                         vgr.parameters.horizontal_axis_parameters.pwm_standard_speed)

        self.assertIsInstance(vgr.parameters.horizontal_axis_parameters.control_parameters,
                              ApproachParameters)
        self.assertEqual(10,
                         vgr.parameters.horizontal_axis_parameters.control_parameters.approach_tolerance)
        self.assertIsInstance(vgr.parameters.vertical_axis_parameters.control_parameters,
                              ApproachParameters)
        self.assertEqual(10,
                         vgr.parameters.vertical_axis_parameters.control_parameters.approach_tolerance)
        self.assertIsInstance(vgr.parameters.rotational_axis_parameters.control_parameters,
                              ApproachParameters)
        self.assertEqual(10,
                         vgr.parameters.rotational_axis_parameters.control_parameters.approach_tolerance)


if __name__ == '__main__':
    unittest.main()
