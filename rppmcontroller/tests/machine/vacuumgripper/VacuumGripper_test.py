import inspect
import logging
import unittest

from rppmcontroller.behavior.CycleStepResultEnum import CycleStepResultEnum
from rppmcontroller.machine.AxisConfig import AxisConfig
from rppmcontroller.machine.vacuumgripper.VacuumGripper import VacuumGripper
from rppmcontroller.machine.vacuumgripper.VacuumGripperConfig import \
    VacuumGripperConfig


class VacuumGripperTestCase(unittest.TestCase):

    def setUp(self):
        # pickupRobot1 = [2600,3550,25]
        # placeConveyorRobot1 = [2000,100,100]
        # placeRand = [2,3,4,5]
        # placeListrobot1 = [pickupRobot1, placeConveyorRobot1, placeRand]
        self.robot1 = VacuumGripper("1")

    def test_GoToconfigGripUnGrip(self):
        """Tests that the grip ungrip acts on the correct actuators """
        logging.debug(f'{inspect.stack()[0][3]} start')
        self.robot1.goto_config_CycleStep(
            VacuumGripperConfig(gripper_active=False))
        self.assertEqual(self.robot1.vacuumActValve, False)
        self.assertEqual(self.robot1.vacuumActCompressorOn, False)
        # activate grip
        logging.debug("Activate Grip")
        self.robot1.goto_config_CycleStep(
            VacuumGripperConfig(gripper_active=True))
        self.assertEqual(self.robot1.vacuumActValve, True)
        self.assertEqual(self.robot1.vacuumActCompressorOn, True)
        # release grip
        logging.debug("Release Grip")
        self.robot1.goto_config_CycleStep(
            VacuumGripperConfig(gripper_active=False))
        self.assertEqual(self.robot1.vacuumActValve, False)
        self.assertEqual(self.robot1.vacuumActCompressorOn, False)

    def testSetup(self):
        logging.debug(f'{inspect.stack()[0][3]} start')
        self.robot1.vacuumSensRotEnd = False
        self.robot1.vacuumSensArmEndIn = False
        self.robot1.vacuumSensVerticalEndUp = False
        self.robot1.vacuumActArmIn = False
        self.robot1.vacuumActArmOut = False
        self.robot1.vacuumActRotLeft = False
        self.robot1.vacuumActRotRight = False
        self.robot1.vacuumActVerticalDown = False
        self.robot1.vacuumActVerticalUp = False
        self.robot1.vacuumSensArmEncoderCounter = 200
        self.robot1.vacuumSensVerticalEncoderCounter = 200
        self.robot1.vacuumSensRotEncoderCounter = 200

        ret = self.robot1.setup_Command().run()

        # at first, it should retract the arm
        self.assertEqual(ret.result, CycleStepResultEnum.MUST_CONTINUE)
        self.assertTrue(self.robot1.vacuumActArmIn)
        self.assertFalse(self.robot1.vacuumActArmOut)
        self.assertFalse(self.robot1.vacuumActRotLeft)
        self.assertFalse(self.robot1.vacuumActRotRight)
        self.assertFalse(self.robot1.vacuumActVerticalDown)
        self.assertFalse(self.robot1.vacuumActVerticalUp)

        # suppose we retracted the arm
        self.robot1.vacuumSensArmEndIn = True
        self.robot1.vacuumSensArmEncoderCounter = 0

        ret = self.robot1.setup_Command().run()

        # now rot and vertical should move
        self.assertEqual(ret.result, CycleStepResultEnum.MUST_CONTINUE)
        self.assertFalse(self.robot1.vacuumActArmIn)
        self.assertFalse(self.robot1.vacuumActArmOut)
        self.assertFalse(self.robot1.vacuumActRotLeft)
        self.assertTrue(self.robot1.vacuumActRotRight)
        self.assertFalse(self.robot1.vacuumActVerticalDown)
        self.assertTrue(self.robot1.vacuumActVerticalUp)

        # simulate move
        # we suppose that it finish to touch the sensor
        self.robot1.vacuumSensRotEnd = True
        self.robot1.vacuumSensVerticalEndUp = True
        self.robot1.vacuumSensVerticalEncoderCounter = 0
        self.robot1.vacuumSensRotEncoderCounter = 0

        ret = self.robot1.setup_Command().run()
        self.assertEqual(ret.result, CycleStepResultEnum.DONE)

        self.assert_stopped()

    def testGotoconfigIncrease(self):
        logging.debug(f'{inspect.stack()[0][3]} start')
        self.robot1.vacuumSensRotEnd = False
        self.robot1.vacuumSensArmEndIn = False
        self.robot1.vacuumSensVerticalEndUp = False
        self.robot1.vacuumActArmIn = False
        self.robot1.vacuumActArmOut = False
        self.robot1.vacuumActRotLeft = False
        self.robot1.vacuumActRotRight = False
        self.robot1.vacuumActVerticalDown = False
        self.robot1.vacuumActVerticalUp = False
        self.robot1.vacuumSensArmEncoderCounter = 100
        self.robot1.vacuumSensVerticalEncoderCounter = 100
        self.robot1.vacuumSensRotEncoderCounter = 100

        ret = self.robot1.goto_config_CycleStep(
            VacuumGripperConfig(AxisConfig.to_counter_goal(500),
                                AxisConfig.to_counter_goal(500),
                                AxisConfig.to_counter_goal(500), False))
        self.assertEqual(ret.result, CycleStepResultEnum.MUST_CONTINUE)

        self.assertFalse(self.robot1.vacuumActArmIn)
        self.assertTrue(self.robot1.vacuumActArmOut)
        self.assertTrue(self.robot1.vacuumActRotLeft)
        self.assertFalse(self.robot1.vacuumActRotRight)
        self.assertTrue(self.robot1.vacuumActVerticalDown)
        self.assertFalse(self.robot1.vacuumActVerticalUp)

        # simulate move
        # we suppose that it finishes to have its counters close to the target
        self.robot1.vacuumSensArmEncoderCounter = 505
        self.robot1.vacuumSensVerticalEncoderCounter = 495
        self.robot1.vacuumSensRotEncoderCounter = 505

        ret = self.robot1.goto_config_CycleStep(
            VacuumGripperConfig(AxisConfig.to_counter_goal(500),
                                AxisConfig.to_counter_goal(500),
                                AxisConfig.to_counter_goal(500), False))
        self.assertEqual(ret.result, CycleStepResultEnum.DONE)

        self.assert_stopped()

    def testGotoconfigDecrease(self):
        logging.debug(f'{inspect.stack()[0][3]} start')
        self.robot1.vacuumSensRotEnd = False
        self.robot1.vacuumSensArmEndIn = False
        self.robot1.vacuumSensVerticalEndUp = False
        self.robot1.vacuumActArmIn = False
        self.robot1.vacuumActArmOut = False
        self.robot1.vacuumActRotLeft = False
        self.robot1.vacuumActRotRight = False
        self.robot1.vacuumActVerticalDown = False
        self.robot1.vacuumActVerticalUp = False
        self.robot1.vacuumSensArmEncoderCounter = 500
        self.robot1.vacuumSensVerticalEncoderCounter = 500
        self.robot1.vacuumSensRotEncoderCounter = 500

        ret = self.robot1.goto_config_CycleStep(
            VacuumGripperConfig(AxisConfig.to_counter_goal(100),
                                AxisConfig.to_counter_goal(100),
                                AxisConfig.to_counter_goal(100), False))
        self.assertEqual(ret.result, CycleStepResultEnum.MUST_CONTINUE)

        self.assertTrue(self.robot1.vacuumActArmIn)
        self.assertFalse(self.robot1.vacuumActArmOut)
        self.assertFalse(self.robot1.vacuumActRotLeft)
        self.assertTrue(self.robot1.vacuumActRotRight)
        self.assertFalse(self.robot1.vacuumActVerticalDown)
        self.assertTrue(self.robot1.vacuumActVerticalUp)

        # simulate move
        # we suppose that it finishes to have its counters close to the target
        self.robot1.vacuumSensArmEncoderCounter = 95
        self.robot1.vacuumSensVerticalEncoderCounter = 105
        self.robot1.vacuumSensRotEncoderCounter = 95

        ret = self.robot1.goto_config_CycleStep(
            VacuumGripperConfig(AxisConfig.to_counter_goal(100),
                                AxisConfig.to_counter_goal(100),
                                AxisConfig.to_counter_goal(100), False))
        self.assertEqual(ret.result, CycleStepResultEnum.DONE)

        self.assert_stopped()

    def testGotoConfigShouldNotRetractIfTouchingSensor(self):
        """Test that even if the counter say it's possible to retract but the sensor is reached, do not activate engine toward the sensor"""
        logging.debug(f'{inspect.stack()[0][3]} start')
        self.robot1.vacuumSensRotEnd = False
        self.robot1.vacuumSensArmEndIn = True
        self.robot1.vacuumSensVerticalEndUp = False
        self.robot1.vacuumActArmIn = False
        self.robot1.vacuumActArmOut = False
        self.robot1.vacuumActRotLeft = False
        self.robot1.vacuumActRotRight = False
        self.robot1.vacuumActVerticalDown = False
        self.robot1.vacuumActVerticalUp = False
        self.robot1.vacuumSensArmEncoderCounter = 500
        self.robot1.vacuumSensVerticalEncoderCounter = 500
        self.robot1.vacuumSensRotEncoderCounter = 500

        ret = self.robot1.goto_config_CycleStep(
            VacuumGripperConfig(AxisConfig.to_counter_goal(100),
                                AxisConfig.to_counter_goal(100),
                                AxisConfig.to_counter_goal(100), False))
        self.assertEqual(ret.result, CycleStepResultEnum.MUST_CONTINUE)

        self.assertFalse(self.robot1.vacuumActArmIn)
        self.assertFalse(self.robot1.vacuumActArmOut)
        self.assertFalse(self.robot1.vacuumActRotLeft)
        self.assertTrue(self.robot1.vacuumActRotRight)
        self.assertFalse(self.robot1.vacuumActVerticalDown)
        self.assertTrue(self.robot1.vacuumActVerticalUp)

    def testGotoConfigShouldNotTurnRightIfTouchingSensor(self):
        """Test that even if the counter say it's possible to turn right but the sensor is reached, do not activate engine toward the sensor"""
        logging.debug(f'{inspect.stack()[0][3]} start')
        self.robot1.vacuumSensRotEnd = True
        self.robot1.vacuumSensArmEndIn = False
        self.robot1.vacuumSensVerticalEndUp = False
        self.robot1.vacuumActArmIn = False
        self.robot1.vacuumActArmOut = False
        self.robot1.vacuumActRotLeft = False
        self.robot1.vacuumActRotRight = False
        self.robot1.vacuumActVerticalDown = False
        self.robot1.vacuumActVerticalUp = False
        self.robot1.vacuumSensArmEncoderCounter = 500
        self.robot1.vacuumSensVerticalEncoderCounter = 500
        self.robot1.vacuumSensRotEncoderCounter = 500

        ret = self.robot1.goto_config_CycleStep(
            VacuumGripperConfig(AxisConfig.to_counter_goal(100),
                                AxisConfig.to_counter_goal(100),
                                AxisConfig.to_counter_goal(100), False))
        self.assertEqual(ret.result, CycleStepResultEnum.MUST_CONTINUE)

        self.assertTrue(self.robot1.vacuumActArmIn)
        self.assertFalse(self.robot1.vacuumActArmOut)
        self.assertFalse(self.robot1.vacuumActRotLeft)
        self.assertFalse(self.robot1.vacuumActRotRight)
        self.assertFalse(self.robot1.vacuumActVerticalDown)
        self.assertTrue(self.robot1.vacuumActVerticalUp)

    def testGotoConfigShouldNotGoUpIfTouchingSensor(self):
        """Test that even if the counter say it's possible to go up but the sensor is reached, do not activate engine toward the sensor"""
        logging.debug(f'{inspect.stack()[0][3]} start')
        self.robot1.vacuumSensRotEnd = False
        self.robot1.vacuumSensArmEndIn = False
        self.robot1.vacuumSensVerticalEndUp = True
        self.robot1.vacuumActArmIn = False
        self.robot1.vacuumActArmOut = False
        self.robot1.vacuumActRotLeft = False
        self.robot1.vacuumActRotRight = False
        self.robot1.vacuumActVerticalDown = False
        self.robot1.vacuumActVerticalUp = False
        self.robot1.vacuumSensArmEncoderCounter = 500
        self.robot1.vacuumSensVerticalEncoderCounter = 500
        self.robot1.vacuumSensRotEncoderCounter = 500

        ret = self.robot1.goto_config_CycleStep(
            VacuumGripperConfig(AxisConfig.to_counter_goal(100),
                                AxisConfig.to_counter_goal(100),
                                AxisConfig.to_counter_goal(100), False))
        self.assertEqual(ret.result, CycleStepResultEnum.MUST_CONTINUE)

        self.assertTrue(self.robot1.vacuumActArmIn)
        self.assertFalse(self.robot1.vacuumActArmOut)
        self.assertFalse(self.robot1.vacuumActRotLeft)
        self.assertTrue(self.robot1.vacuumActRotRight)
        self.assertFalse(self.robot1.vacuumActVerticalDown)
        self.assertFalse(self.robot1.vacuumActVerticalUp)

    def test_move_to_zero_behaves_like_move_to_ref_switch(self):
        """
        Ensure that an instruction to move to position 0 on an axis will
        only be considered completed once the corresponding ref switch is hit,
        independently of what the counter-value says.
        """
        vgr = self.robot1

        # rotation
        config = VacuumGripperConfig()
        config.rotation_axis_config = AxisConfig.to_counter_goal(0)

        vgr.vacuumSensRotEncoderCounter = 200
        vgr.vacuumSensRotEnd = False
        vgr.goto_config_CycleStep(config)
        self.assertTrue(vgr.vacuumActRotRight)

        vgr.vacuumSensRotEncoderCounter = 0
        vgr.goto_config_CycleStep(config)
        self.assertTrue(vgr.vacuumActRotRight)

        vgr.vacuumSensRotEncoderCounter = -3
        vgr.goto_config_CycleStep(config)
        self.assertTrue(vgr.vacuumActRotRight)

        vgr.vacuumSensRotEnd = True
        vgr.goto_config_CycleStep(config)
        self.assertFalse(vgr.vacuumActRotRight)

        # vertical
        config = VacuumGripperConfig()
        config.vertical_axis_config = AxisConfig.to_counter_goal(0)

        vgr.vacuumSensVerticalEncoderCounter = 200
        vgr.vacuumSensVerticalEndUp = False
        vgr.goto_config_CycleStep(config)
        self.assertTrue(vgr.vacuumActVerticalUp)

        vgr.vacuumSensVerticalEncoderCounter = 0
        vgr.goto_config_CycleStep(config)
        self.assertTrue(vgr.vacuumActVerticalUp)

        vgr.vacuumSensVerticalEncoderCounter = -3
        vgr.goto_config_CycleStep(config)
        self.assertTrue(vgr.vacuumActVerticalUp)

        vgr.vacuumSensVerticalEndUp = True
        vgr.goto_config_CycleStep(config)
        self.assertFalse(vgr.vacuumActVerticalUp)

        # horizontal
        config = VacuumGripperConfig()
        config.horizontal_axis_config = AxisConfig.to_counter_goal(0)

        vgr.vacuumSensArmEncoderCounter = 200
        vgr.vacuumSensArmEndIn = False
        vgr.goto_config_CycleStep(config)
        self.assertTrue(vgr.vacuumActArmIn)

        vgr.vacuumSensArmEncoderCounter = 0
        vgr.goto_config_CycleStep(config)
        self.assertTrue(vgr.vacuumActArmIn)

        vgr.vacuumSensArmEncoderCounter = -3
        vgr.goto_config_CycleStep(config)
        self.assertTrue(vgr.vacuumActArmIn)

        vgr.vacuumSensArmEndIn = True
        vgr.goto_config_CycleStep(config)
        self.assertFalse(vgr.vacuumActArmIn)

    def test_abort_on_invalid_rotation(self):
        """
        Ensure that commands to move to an invalid rotational configuration
        are rejected by returning an abort
        """
        vgr = self.robot1
        invalid_rotation = vgr.parameters.max_rotational_counter_value + 100
        config = VacuumGripperConfig(rotation_axis_config=AxisConfig.to_counter_goal(
            invalid_rotation))

        res = vgr.goto_config_CycleStep(config)
        self.assertEqual(CycleStepResultEnum.ABORTED_ERROR, res.result)
        self.assert_stopped()


    def test_abort_on_invalid_vertical_goal(self):
        """
        Ensure that commands to move to an invalid vertical configuration
        are rejected by returning an abort
        """
        vgr = self.robot1
        invalid_vertical_goal = vgr.parameters.max_vertical_counter_value + 100
        config = VacuumGripperConfig(vertical_axis_config=AxisConfig.to_counter_goal(
            invalid_vertical_goal))

        res = vgr.goto_config_CycleStep(config)
        self.assertEqual(CycleStepResultEnum.ABORTED_ERROR, res.result)
        self.assert_stopped()

    def test_abort_on_invalid_horizontal_goal(self):
        """
        Ensure that commands to move to an invalid horizontal configuration
        are rejected by returning an abort
        """
        vgr = self.robot1
        invalid_horizontal_goal = vgr.parameters.max_horizontal_counter_value + 100
        config = VacuumGripperConfig(horizontal_axis_config=AxisConfig.to_counter_goal(
            invalid_horizontal_goal))

        res = vgr.goto_config_CycleStep(config)
        self.assertEqual(CycleStepResultEnum.ABORTED_ERROR, res.result)
        self.assert_stopped()

    def assert_stopped(self) -> None:
        """
        Asserts that no actuators are active
        """
        self.assertFalse(self.robot1.vacuumActArmIn)
        self.assertFalse(self.robot1.vacuumActArmOut)
        self.assertFalse(self.robot1.vacuumActRotLeft)
        self.assertFalse(self.robot1.vacuumActRotRight)
        self.assertFalse(self.robot1.vacuumActVerticalDown)
        self.assertFalse(self.robot1.vacuumActVerticalUp)


if __name__ == '__main__':
    logging.basicConfig(
        format='%(levelname)-5s: %(module)-20s,%(lineno)-3s: %(message)s',
        level=logging.DEBUG)

    unittest.main()
