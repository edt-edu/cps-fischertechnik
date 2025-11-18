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

    def test_mark_for_reset_if(self):
        reset_helper = ResetHelper()

        reset_helper.mark_for_reset_if(False)
        self.assertFalse(reset_helper.is_marked_for_reset, "helper should not have been marked for reset")

        reset_helper.mark_for_reset_if(True)
        self.assertTrue(reset_helper.is_marked_for_reset, "helper should have been marked for reset")

        reset_helper.mark_for_reset_if(False)
        self.assertTrue(reset_helper.is_marked_for_reset, "mark should not have been removed")

    def test_temper_tolerance(self):
        reset_helper = ResetHelper(temper_tolerance=100)

        reset_helper.mark_for_reset_if(True, 800)
        self.assertFalse(reset_helper.reset(), "800 is outside of temper tolerance")

        reset_helper.mark_for_reset_if(True, 10)
        self.assertTrue(reset_helper.reset(), "10 is within temper tolerance")

        reset_helper.mark_for_reset_if(True, -800)
        self.assertTrue(reset_helper.reset(), "negative counter values shouldn't be checked by temper tolerance")


if __name__ == '__main__':
    unittest.main()
