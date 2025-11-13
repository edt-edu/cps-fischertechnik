import unittest

from rppmcontroller.machine.ResetHelper import ResetHelper


class ResetHelperTestCase(unittest.TestCase):
    def test_reset(self):
        reset_helper = ResetHelper()

        self.assertEqual(False, reset_helper.reset(),
                         "Initial state should be False")

        reset_helper.mark_for_reset()
        self.assertTrue(reset_helper.is_marked_for_reset, "helper should have been marked for reset")

        self.assertEqual(True, reset_helper.reset(), "First call should return True")
        self.assertEqual(False, reset_helper.reset(), "Consecutive calls should return False")
        self.assertEqual(False, reset_helper.reset(),
                         "Value should not go back to True")


if __name__ == '__main__':
    unittest.main()
