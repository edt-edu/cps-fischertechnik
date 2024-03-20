
import inspect
import logging

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
        

    # def testSetup(self):
    #     """Tests, whether setup activity is performed in correct order"""
    #     self.robot1.vacuumSensRotEnd = False
    #     self.robot1.vacuumSensArmEndIn = False
    #     self.robot1.vacuumSensVerticalEndUp = False
    #     self.robot1.execute(0,1)
    #     self.assertEqual(self.robot1.setupFinishedHelper, False)
    # def testSetup(self):
    #     """Tests, whether setup activity is performed in correct order"""
    #     self.robot1.vacuumSensRotEnd = False
    #     self.robot1.vacuumSensArmEndIn = False
    #     self.robot1.vacuumSensVerticalEndUp = False
    #     self.robot1.execute(0,1)
    #     self.assertEqual(self.robot1.setupFinishedHelper, False)
    #     self.assertEqual(self.robot1.pc, 0)
    #     #Arm einfahren
    #     self.assertEqual(self.robot1.vacuumActArmIn, True)
    #     self.assertEqual(self.robot1.vacuumActArmOut, False)
    #     self.assertEqual(self.robot1.vacuumActRotLeft, False)
    #     self.assertEqual(self.robot1.vacuumActRotRight, False)
    #     self.assertEqual(self.robot1.vacuumActVerticalUp, False)
    #     self.assertEqual(self.robot1.vacuumActVerticalDown, False)
    #     self.assertEqual(self.robot1.vacuumActValve, False)
    #     self.assertEqual(self.robot1.vacuumActCompressorOn, False)
    #     self.robot1.execute(0,1)
    #     self.assertEqual(self.robot1.setupFinishedHelper, False)
    #     self.assertEqual(self.robot1.pc, 0)
    #     self.assertEqual(self.robot1.vacuumActArmIn, True)
    #     self.assertEqual(self.robot1.vacuumActArmOut, False)
    #     self.assertEqual(self.robot1.vacuumActRotLeft, False)
    #     self.assertEqual(self.robot1.vacuumActRotRight, False)
    #     self.assertEqual(self.robot1.vacuumActVerticalUp, False)
    #     self.assertEqual(self.robot1.vacuumActVerticalDown, False)
    #     self.assertEqual(self.robot1.vacuumActValve, False)
    #     self.assertEqual(self.robot1.vacuumActCompressorOn, False)
    #     self.robot1.vacuumSensArmEndIn = True
    #     #Greifer öffnen, rotieren, nach oben fahren
    #     self.robot1.execute(0,1)
    #     self.assertEqual(self.robot1.setupFinishedHelper, False)
    #     self.assertEqual(self.robot1.pc, 0)
    #     self.assertEqual(self.robot1.vacuumActArmIn, False)
    #     self.assertEqual(self.robot1.vacuumActArmOut, False)
    #     self.assertEqual(self.robot1.vacuumActRotLeft, False)
    #     self.assertEqual(self.robot1.vacuumActRotRight, True)
    #     self.assertEqual(self.robot1.vacuumActVerticalUp, True)
    #     self.assertEqual(self.robot1.vacuumActVerticalDown, False)
    #     self.assertEqual(self.robot1.vacuumActValve, False)
    #     self.assertEqual(self.robot1.vacuumActCompressorOn, False)
    #     self.robot1.execute(0,1)
    #     self.assertEqual(self.robot1.vacuumActArmIn, False)
    #     self.assertEqual(self.robot1.vacuumActArmOut, False)
    #     self.assertEqual(self.robot1.vacuumActRotLeft, False)
    #     self.assertEqual(self.robot1.vacuumActRotRight, True)
    #     self.assertEqual(self.robot1.vacuumActVerticalUp, True)
    #     self.assertEqual(self.robot1.vacuumActVerticalDown, False)
    #     self.assertEqual(self.robot1.vacuumActValve, False)
    #     self.assertEqual(self.robot1.vacuumActCompressorOn, False)
    #     self.assertEqual(self.robot1.setupFinishedHelper, False)
    #     self.assertEqual(self.robot1.pc, 0)
    #     self.robot1.vacuumSensRotEnd = True
    #     self.robot1.vacuumSensVerticalEndUp = True
    #     self.robot1.execute(0,1)
    #     self.assertEqual(self.robot1.vacuumActArmIn, False)
    #     self.assertEqual(self.robot1.vacuumActArmOut, False)
    #     self.assertEqual(self.robot1.vacuumActRotLeft, False)
    #     self.assertEqual(self.robot1.vacuumActRotRight, False)
    #     self.assertEqual(self.robot1.vacuumActVerticalUp, False)
    #     self.assertEqual(self.robot1.vacuumActVerticalDown, False)
    #     self.assertEqual(self.robot1.vacuumActValve, False)
    #     self.assertEqual(self.robot1.vacuumActCompressorOn, False)
    #     self.assertEqual(self.robot1.setupFinishedHelper, True)
    #     self.assertEqual(self.robot1.pc, 0)
    #     self.robot1.execute(0,1)
    #     self.assertEqual(self.robot1.setupFinishedHelper, True)
    #     self.assertEqual(self.robot1.pc, 1)
    #     #Übergang zu tatsächlichem Execute geschafft

    # def testPickWithExecute(self):
    #     """Tests, whether the execute function uses the move list correctly with all transitions and all outputs are set accordingly"""
    #     self.setUpFaker()
    #     # PICK
    #     self.robot1.execute(0,1) # config already reached
    #     self.assertEqual(self.robot1.pc, 1)
    #     self.assertEqual(self.robot1.generateTransferMoveList(0,1)[0], VacuumGripperConfig(0,0,0,False))
    #     self.assertEqual(self.robot1.vacuumActArmIn, False)
    #     self.assertEqual(self.robot1.vacuumActArmOut, False)
    #     self.assertEqual(self.robot1.vacuumActRotLeft, False)
    #     self.assertEqual(self.robot1.vacuumActRotRight, False)
    #     self.assertEqual(self.robot1.vacuumActVerticalUp, False)
    #     self.assertEqual(self.robot1.vacuumActVerticalDown, False)
    #     self.assertEqual(self.robot1.vacuumActValve, False)
    #     self.assertEqual(self.robot1.vacuumActCompressorOn, False)
    #     self.robot1.execute(0,1)
    #     self.assertEqual(self.robot1.pc, 2)
    #     self.assertEqual(self.robot1.generateTransferMoveList(0,1)[1], VacuumGripperConfig(2350,3550,0,False))
    #     self.assertEqual(self.robot1.vacuumActArmIn, False)
    #     self.assertEqual(self.robot1.vacuumActArmOut, False)
    #     self.assertEqual(self.robot1.vacuumActRotLeft, True)
    #     self.assertEqual(self.robot1.vacuumActRotRight, False)
    #     self.assertEqual(self.robot1.vacuumActVerticalUp, False)
    #     self.assertEqual(self.robot1.vacuumActVerticalDown, True)
    #     self.assertEqual(self.robot1.vacuumActValve, False)
    #     self.assertEqual(self.robot1.vacuumActCompressorOn, False)
    #     self.robot1.vacuumSensVerticalEncoderCounter = 2350
    #     self.robot1.vacuumSensRotEncoderCounter = 3550
    #     self.robot1.execute(0,1)
    #     self.assertEqual(self.robot1.pc, 2)
    #     self.assertEqual(self.robot1.vacuumActArmIn, False)
    #     self.assertEqual(self.robot1.vacuumActArmOut, False)
    #     self.assertEqual(self.robot1.vacuumActRotLeft, False)
    #     self.assertEqual(self.robot1.vacuumActRotRight, False)
    #     self.assertEqual(self.robot1.vacuumActVerticalUp, False)
    #     self.assertEqual(self.robot1.vacuumActVerticalDown, False)
    #     self.assertEqual(self.robot1.vacuumActValve, False)
    #     self.assertEqual(self.robot1.vacuumActCompressorOn, False)
    #     self.robot1.execute(0,1)
    #     self.assertEqual(self.robot1.pc, 3)
    #     self.assertEqual(self.robot1.vacuumActArmIn, False)
    #     self.assertEqual(self.robot1.vacuumActArmOut, True)
    #     self.assertEqual(self.robot1.vacuumActRotLeft, False)
    #     self.assertEqual(self.robot1.vacuumActRotRight, False)
    #     self.assertEqual(self.robot1.vacuumActVerticalUp, False)
    #     self.assertEqual(self.robot1.vacuumActVerticalDown, False)
    #     self.assertEqual(self.robot1.vacuumActValve, False)
    #     self.assertEqual(self.robot1.vacuumActCompressorOn, False)
    #     self.robot1.vacuumSensArmEncoderCounter = 25
    #     self.robot1.execute(0,1)
    #     self.assertEqual(self.robot1.pc, 3)
    #     self.assertEqual(self.robot1.vacuumActArmIn, False)
    #     self.assertEqual(self.robot1.vacuumActArmOut, False)
    #     self.assertEqual(self.robot1.vacuumActRotLeft, False)
    #     self.assertEqual(self.robot1.vacuumActRotRight, False)
    #     self.assertEqual(self.robot1.vacuumActVerticalUp, False)
    #     self.assertEqual(self.robot1.vacuumActVerticalDown, False)
    #     self.assertEqual(self.robot1.vacuumActValve, False)
    #     self.assertEqual(self.robot1.vacuumActCompressorOn, False)
    #     self.robot1.execute(0,1)
    #     self.assertEqual(self.robot1.pc, 4)
    #     self.assertEqual(self.robot1.vacuumActArmIn, False)
    #     self.assertEqual(self.robot1.vacuumActArmOut, False)
    #     self.assertEqual(self.robot1.vacuumActRotLeft, False)
    #     self.assertEqual(self.robot1.vacuumActRotRight, False)
    #     self.assertEqual(self.robot1.vacuumActVerticalUp, False)
    #     self.assertEqual(self.robot1.vacuumActVerticalDown, True)
    #     self.assertEqual(self.robot1.vacuumActValve, False)
    #     self.assertEqual(self.robot1.vacuumActCompressorOn, False)
    #     self.robot1.vacuumSensVerticalEncoderCounter = 2850
    #     self.robot1.execute(0,1)
    #     self.assertEqual(self.robot1.pc, 4)
    #     self.assertEqual(self.robot1.vacuumActArmIn, False)
    #     self.assertEqual(self.robot1.vacuumActArmOut, False)
    #     self.assertEqual(self.robot1.vacuumActRotLeft, False)
    #     self.assertEqual(self.robot1.vacuumActRotRight, False)
    #     self.assertEqual(self.robot1.vacuumActVerticalUp, False)
    #     self.assertEqual(self.robot1.vacuumActVerticalDown, False)
    #     self.assertEqual(self.robot1.vacuumActValve, False)
    #     self.assertEqual(self.robot1.vacuumActCompressorOn, False)
    #     for i in range(11): #wait 10 steps for suction to build
    #         i += 1
    #         self.robot1.execute(0,1)
    #         self.assertEqual(self.robot1.pc, 5)
    #         self.assertEqual(self.robot1.vacuumActArmIn, False)
    #         self.assertEqual(self.robot1.vacuumActArmOut, False)
    #         self.assertEqual(self.robot1.vacuumActRotLeft, False)
    #         self.assertEqual(self.robot1.vacuumActRotRight, False)
    #         self.assertEqual(self.robot1.vacuumActVerticalUp, False)
    #         self.assertEqual(self.robot1.vacuumActVerticalDown, False)
    #         self.assertEqual(self.robot1.vacuumActValve, True)
    #         self.assertEqual(self.robot1.vacuumActCompressorOn, True)
    #     self.robot1.execute(0,1)
    #     self.assertEqual(self.robot1.pc, 6)

    # def setUpFaker(self):
    #     """Sets all the variables to fake a finished setup process"""
    #     # SETUP (not tested, just set to finished)
    #     self.robot1.vacuumSensRotEnd = True
    #     self.robot1.vacuumSensArmEndIn = True
    #     self.robot1.vacuumSensVerticalEndUp = True
    #     self.robot1.execute(0,1)
    #     self.assertEqual(self.robot1.setupFinishedHelper, True)
    #     self.assertEqual(self.robot1.pc, 0)



if __name__ == '__main__':
    logging.basicConfig(format='%(levelname)-5s: %(module)-20s,%(lineno)-3s: %(message)s', level=logging.DEBUG)

    unittest.main()
