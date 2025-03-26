
import inspect
import logging

from rppmcontroller.behavior.CycleStepResultEnum import CycleStepResultEnum
from rppmcontroller.machine.vacuumgripper.VacuumGripper import VacuumGripper
from rppmcontroller.machine.vacuumgripper.VacuumGripperConfig import VacuumGripperConfig
from rppmcontroller.utils.PlusMinusStop import PlusMinusStop
from rppmcontroller.machine.Position import Position

import unittest


class VacuumGripperTestCase(unittest.TestCase):

    def setUp(self):
        # pickupRobot1 = [2600,3550,25]
        # placeConveyorRobot1 = [2000,100,100]
        # placeRand = [2,3,4,5]
        # placeListrobot1 = [pickupRobot1, placeConveyorRobot1, placeRand]
        self.robot1 = VacuumGripper(1)
        

    def test_GoToconfigGripUnGrip(self):
        """Tests that the grip ungrip acts on the correct actuators """
        logging.debug(f'{inspect.stack()[0][3]} start')
        self.robot1.gotoconfig(VacuumGripperConfig(0,0,0,False))
        self.assertEqual(self.robot1.vacuumActValve, False)
        self.assertEqual(self.robot1.vacuumActCompressorOn, False)        
        # activate grip
        logging.debug("Activate Grip")
        self.robot1.gotoconfig(VacuumGripperConfig(0,0,0,True))
        self.assertEqual(self.robot1.vacuumActValve, True)
        self.assertEqual(self.robot1.vacuumActCompressorOn, True)
        # release grip
        logging.debug("Release Grip")
        self.robot1.gotoconfig(VacuumGripperConfig(0,0,0,False))
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
        self.robot1.vacuumSensArmEncoderCounter = 0
        self.robot1.vacuumSensVerticalEncoderCounter = 0
        self.robot1.vacuumSensRotEncoderCounter = 0

        ret = self.robot1.setup_CycleStep()
        self.assertEqual(ret.result, CycleStepResultEnum.MUST_CONTINUE)
        
        self.assertTrue(self.robot1.vacuumActArmIn)
        self.assertFalse(self.robot1.vacuumActArmOut)
        self.assertFalse(self.robot1.vacuumActRotLeft)
        self.assertTrue(self.robot1.vacuumActRotRight)
        self.assertFalse(self.robot1.vacuumActVerticalDown)
        self.assertTrue(self.robot1.vacuumActVerticalUp)

        # simulate move
        # we suppose that it finish to touch the sensor
        self.robot1.vacuumSensRotEnd = True
        self.robot1.vacuumSensArmEndIn = True
        self.robot1.vacuumSensVerticalEndUp = True

        ret = self.robot1.setup_CycleStep()
        self.assertEqual(ret.result, CycleStepResultEnum.DONE)

        self.assertFalse(self.robot1.vacuumActArmIn)
        self.assertFalse(self.robot1.vacuumActArmOut)
        self.assertFalse(self.robot1.vacuumActRotLeft)
        self.assertFalse(self.robot1.vacuumActRotRight)
        self.assertFalse(self.robot1.vacuumActVerticalDown)
        self.assertFalse(self.robot1.vacuumActVerticalUp)

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

        ret = self.robot1.gotoconfig(VacuumGripperConfig(500,500,500,False))
        self.assertEqual(ret.result, CycleStepResultEnum.MUST_CONTINUE)
        
        self.assertFalse(self.robot1.vacuumActArmIn)
        self.assertTrue(self.robot1.vacuumActArmOut)
        self.assertTrue(self.robot1.vacuumActRotLeft)
        self.assertFalse(self.robot1.vacuumActRotRight)
        self.assertTrue(self.robot1.vacuumActVerticalDown)
        self.assertFalse(self.robot1.vacuumActVerticalUp)

        # simulate move
        # we suppose that it finishes to have its counters close to the target
        self.robot1.vacuumSensArmEncoderCounter = 510
        self.robot1.vacuumSensVerticalEncoderCounter = 490
        self.robot1.vacuumSensRotEncoderCounter = 505

        ret = self.robot1.gotoconfig(VacuumGripperConfig(500,500,500,False))
        self.assertEqual(ret.result, CycleStepResultEnum.DONE)
        
        self.assertFalse(self.robot1.vacuumActArmIn)
        self.assertFalse(self.robot1.vacuumActArmOut)
        self.assertFalse(self.robot1.vacuumActRotLeft)
        self.assertFalse(self.robot1.vacuumActRotRight)
        self.assertFalse(self.robot1.vacuumActVerticalDown)
        self.assertFalse(self.robot1.vacuumActVerticalUp)


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

        ret = self.robot1.gotoconfig(VacuumGripperConfig(100,100,100,False))
        self.assertEqual(ret.result, CycleStepResultEnum.MUST_CONTINUE)
        
        self.assertTrue(self.robot1.vacuumActArmIn)
        self.assertFalse(self.robot1.vacuumActArmOut)
        self.assertFalse(self.robot1.vacuumActRotLeft)
        self.assertTrue(self.robot1.vacuumActRotRight)
        self.assertFalse(self.robot1.vacuumActVerticalDown)
        self.assertTrue(self.robot1.vacuumActVerticalUp)

        # simulate move
        # we suppose that it finishes to have its counters close to the target
        self.robot1.vacuumSensArmEncoderCounter = 90
        self.robot1.vacuumSensVerticalEncoderCounter = 110
        self.robot1.vacuumSensRotEncoderCounter = 95

        ret = self.robot1.gotoconfig(VacuumGripperConfig(100,100,100,False))
        self.assertEqual(ret.result, CycleStepResultEnum.DONE)
        
        self.assertFalse(self.robot1.vacuumActArmIn)
        self.assertFalse(self.robot1.vacuumActArmOut)
        self.assertFalse(self.robot1.vacuumActRotLeft)
        self.assertFalse(self.robot1.vacuumActRotRight)
        self.assertFalse(self.robot1.vacuumActVerticalDown)
        self.assertFalse(self.robot1.vacuumActVerticalUp)

    def testGotoconfigShouldNotRetractIfTouchingSensor(self):
        """Test that even if the counter say its possible to retract but the sensor is reached, do not activate engine toward the sensor"""
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

        ret = self.robot1.gotoconfig(VacuumGripperConfig(100,100,100,False))
        self.assertEqual(ret.result, CycleStepResultEnum.ABORTED_ERROR)

        self.assertFalse(self.robot1.vacuumActArmIn)
        self.assertFalse(self.robot1.vacuumActArmOut)
        self.assertFalse(self.robot1.vacuumActRotLeft)
        self.assertFalse(self.robot1.vacuumActRotRight)
        self.assertFalse(self.robot1.vacuumActVerticalDown)
        self.assertFalse(self.robot1.vacuumActVerticalUp)

    def testGotoconfigShouldNotTurnRightIfTouchingSensor(self):
        """Test that even if the counter say its possible to turn right but the sensor is reached, do not activate engine toward the sensor"""
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

        ret = self.robot1.gotoconfig(VacuumGripperConfig(100,100,100,False))
        self.assertEqual(ret.result, CycleStepResultEnum.ABORTED_ERROR)

        self.assertFalse(self.robot1.vacuumActArmIn)
        self.assertFalse(self.robot1.vacuumActArmOut)
        self.assertFalse(self.robot1.vacuumActRotLeft)
        self.assertFalse(self.robot1.vacuumActRotRight)
        self.assertFalse(self.robot1.vacuumActVerticalDown)
        self.assertFalse(self.robot1.vacuumActVerticalUp)


    def testGotoconfigShouldNotGoupIfTouchingSensor(self):
        """Test that even if the counter say its possible to go up but the sensor is reached, do not activate engine toward the sensor"""
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

        ret = self.robot1.gotoconfig(VacuumGripperConfig(100,100,100,False))
        self.assertEqual(ret.result, CycleStepResultEnum.ABORTED_ERROR)

        self.assertFalse(self.robot1.vacuumActArmIn)
        self.assertFalse(self.robot1.vacuumActArmOut)
        self.assertFalse(self.robot1.vacuumActRotLeft)
        self.assertFalse(self.robot1.vacuumActRotRight)
        self.assertFalse(self.robot1.vacuumActVerticalDown)
        self.assertFalse(self.robot1.vacuumActVerticalUp)

if __name__ == '__main__':
    logging.basicConfig(format='%(levelname)-5s: %(module)-20s,%(lineno)-3s: %(message)s', level=logging.DEBUG)

    unittest.main()
