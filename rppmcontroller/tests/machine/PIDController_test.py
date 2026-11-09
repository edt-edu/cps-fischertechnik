import unittest

from rppmcontroller.machine.PIDController import DiscretePIDController


class MyTestCase(unittest.TestCase):
    def test_output_not_zero(self):
        controller = DiscretePIDController.for_pwm_control(0.001)
        controller.target.set(2000)
        output = controller.process(0)
        self.assertGreater(output, 0)

    def test_output_zero_on_target(self):
        controller = DiscretePIDController.for_pwm_control(0.001)
        controller.target.set(400)
        output = controller.process(400)
        self.assertEqual(output, 0)


if __name__ == '__main__':
    unittest.main()
