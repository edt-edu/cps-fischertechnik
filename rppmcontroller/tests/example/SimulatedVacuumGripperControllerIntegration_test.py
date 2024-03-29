
import inspect
import logging
import time
import unittest
from unittest.mock import patch, Mock

from rppmcontroller.example.SimulatedVacuumGripperController import SimulatedVacuumGripperController
from rppmcontroller.machine.vacuumgripper.VacuumGripper import VacuumGripper
from rppmcontroller.protocol.JSONReader import JSONReader
from rppmcontroller.protocol.JSONParser import JSONParser
from rppmcontroller.protocol.JSONOutput import JSONOutput
from rppmcontroller.machine.Position import Position
from rppmcontroller.machine.BoxNumber import BoxNumber
from rppmcontroller.machine.Direction import Direction
from rppmcontroller.machine.Colour import Colour
from rppmcontroller.protocol.MachineCommand import MachineCommand
from typing import Union

import tests.controllerTestHelper as ctHelper


class SimulatedVacuumGripperControllerIntegrationTestCase(unittest.TestCase):

    def setUp(self):
    
        logging.debug("setup called")
        self.controller = SimulatedVacuumGripperController()
        self.controller.mainLoopDelay = 0.1

        
    def test_setupCommand(self):
        """Ensure that the setup command is performed and and send feedback"""
        logging.debug(f'{inspect.stack()[0][3]} start')

        # initial feedback
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readNotification(self.controller), r"VacuumGripper01 \d+\.\d+ FEEDBACK 0 FINISHED")

        # controller is idle
        self.controller.mainLoopIteration()
        self.assertEqual(ctHelper.readNotification(self.controller), "")

        # send a setup command
        message = MachineCommand("COMMAND", "VACUUM", 1, "SETUP", [])
        ctHelper.sendMessage(self.controller, "VacuumGripper01", message)
        
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readNotification(self.controller), r"VacuumGripper01 \d+\.\d+ FEEDBACK 1 INACTION")

        endCommandReached = False
        iterationDone = 0
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readNotification(self.controller)
            if (notification == "") :
                iterationDone += 1
            else:
                self.assertRegex(notification, r"VacuumGripper01 \d+\.\d+ FEEDBACK 1 FINISHED")
                logging.debug(f"Setup FINISHED reached in {iterationDone} iterations")
                endCommandReached = True
            self.assertLess(iterationDone, 20, "Setup FINISHED not reached in less than 20 iterations" )

        # controller is idle
        for _ in range(5):
            self.controller.mainLoopIteration()
            self.assertEqual(ctHelper.readNotification(self.controller), "")
        
        # check current position via feedback and/or by reading machine IO
        self.checkVGRPosition(0,0,0)



    def test_twoSetupCommands(self):
        """Ensure that the setup command is performed and and send feedback"""
        logging.debug(f'{inspect.stack()[0][3]} start')

        # initial feedback
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readNotification(self.controller), r"VacuumGripper01 \d+\.\d+ FEEDBACK 0 FINISHED")

        # controller is idle
        self.controller.mainLoopIteration()
        self.assertEqual(ctHelper.readNotification(self.controller), "")

        # send a setup command
        message = MachineCommand("COMMAND", "VACUUM", 1, "SETUP", [])
        ctHelper.sendMessage(self.controller, "VacuumGripper01", message)
        
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readNotification(self.controller), r"VacuumGripper01 \d+\.\d+ FEEDBACK 1 INACTION")

        endCommandReached = False
        iterationDone = 0
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readNotification(self.controller)
            if (notification == "") :
                iterationDone += 1
            else:
                self.assertRegex(notification, r"VacuumGripper01 \d+\.\d+ FEEDBACK 1 FINISHED")
                logging.debug(f"Setup FINISHED reached in {iterationDone} iterations")
                endCommandReached = True
            self.assertLess(iterationDone, 20, "Setup FINISHED not reached in less than 20 iterations" )

        # check current position via feedback and/or by reading machine IO
        self.checkVGRPosition(0,0,0)

        # controller is idle
        for _ in range(5):
            self.controller.mainLoopIteration()
            self.assertEqual(ctHelper.readNotification(self.controller), "")
        

        # send a second setup command
        message = MachineCommand("COMMAND", "VACUUM", 2, "SETUP", [])
        ctHelper.sendMessage(self.controller, "VacuumGripper01", message)
        
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readNotification(self.controller), r"VacuumGripper01 \d+\.\d+ FEEDBACK 2 INACTION")

        endCommandReached = False
        iterationDone = 0
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readNotification(self.controller)
            if (notification == "") :
                iterationDone += 1
            else:
                self.assertRegex(notification, r"VacuumGripper01 \d+\.\d+ FEEDBACK 2 FINISHED")
                logging.debug(f"Setup FINISHED reached in {iterationDone} iterations")
                endCommandReached = True
            self.assertLess(iterationDone, 2, "Setup FINISHED not reached in less than 2 iterations" )


        # TODO check current position via feedback and/or by reading machine IO
            
        # controller is idle
        for _ in range(5):
            self.controller.mainLoopIteration()
            self.assertEqual(ctHelper.readNotification(self.controller), "")
        # check current position via feedback and/or by reading machine IO
        self.checkVGRPosition(0,0,0)



    def test_moveCommand_with_internal_setup(self):
        """Ensure that the pick command is performed and and send feedback"""
        logging.debug(f'{inspect.stack()[0][3]} start')

        # initial feedback
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readNotification(self.controller), r"VacuumGripper01 \d+\.\d+ FEEDBACK 0 FINISHED")

        # controller is idle
        self.controller.mainLoopIteration()
        self.assertEqual(ctHelper.readNotification(self.controller), "")

        # send a setup command
        message = MachineCommand("COMMAND", "VACUUM", 1, "MOVE", [
            Position("START", 500, 200, 400),
            Position("END", 500, 1000, 1200)
        ])
        ctHelper.sendMessage(self.controller, "VacuumGripper01", message)
        
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readNotification(self.controller), r"VacuumGripper01 \d+\.\d+ FEEDBACK 1 INACTION")

        endCommandReached = False
        iterationDone = 0
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readNotification(self.controller)
            if (notification == "") :
                iterationDone += 1
            else:
                self.assertRegex(notification, r"VacuumGripper01 \d+\.\d+ FEEDBACK 1 FINISHED")
                logging.debug(f"MOVE FINISHED reached in {iterationDone} iterations")
                endCommandReached = True
            self.assertLess(iterationDone, 40, "MOVE FINISHED not reached in less than 40 iterations" )


        
        # check current position via feedback and/or by reading machine IO
        self.checkVGRPosition(500 - 250,1000,1200) # 250 is the offset of the move command # TODO have a better management of this offset
            
        # controller is idle
        for _ in range(2):
            self.controller.mainLoopIteration()
            self.assertEqual(ctHelper.readNotification(self.controller), "")
        

    def test_setup_then_moveCommands(self):
        """Ensure that the setup command is performed and and send feedback"""
        logging.debug(f'{inspect.stack()[0][3]} start')

        # initial feedback
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readNotification(self.controller), r"VacuumGripper01 \d+\.\d+ FEEDBACK 0 FINISHED")

        # controller is idle
        self.controller.mainLoopIteration()
        self.assertEqual(ctHelper.readNotification(self.controller), "")

        # send a setup command
        message = MachineCommand("COMMAND", "VACUUM", 1, "SETUP", [])
        ctHelper.sendMessage(self.controller, "VacuumGripper01", message)
        
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readNotification(self.controller), r"VacuumGripper01 \d+\.\d+ FEEDBACK 1 INACTION")

        endCommandReached = False
        iterationDone = 0
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readNotification(self.controller)
            if (notification == "") :
                iterationDone += 1
            else:
                self.assertRegex(notification, r"VacuumGripper01 \d+\.\d+ FEEDBACK 1 FINISHED")
                logging.debug(f"Setup FINISHED reached in {iterationDone} iterations")
                endCommandReached = True
            self.assertLess(iterationDone, 20, "Setup FINISHED not reached in less than 20 iterations" )

        # controller is idle
        for _ in range(2):
            self.controller.mainLoopIteration()
            self.assertEqual(ctHelper.readNotification(self.controller), "")

        # send a setup command
        message = MachineCommand("COMMAND", "VACUUM", 2, "MOVE", [
            Position("START", 500, 200, 400),
            Position("END", 500, 1000, 1200) 
        ])
        ctHelper.sendMessage(self.controller, "VacuumGripper01", message)
        
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readNotification(self.controller), r"VacuumGripper01 \d+\.\d+ FEEDBACK 2 INACTION")

        endCommandReached = False
        iterationDone = 0
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readNotification(self.controller)
            if (notification == "") :
                iterationDone += 1
            else:
                self.assertRegex(notification, r"VacuumGripper01 \d+\.\d+ FEEDBACK 2 FINISHED")
                logging.debug(f"MOVE FINISHED reached in {iterationDone} iterations")
                endCommandReached = True
            self.assertLess(iterationDone, 400, "MOVE FINISHED not reached in less than 400 iterations" )


        
        # check current position via feedback and/or by reading machine IO
        self.checkVGRPosition(500 - 250, 1000, 1200) # 250 is the offset of the move command # TODO have a better management of this offset
            
        # controller is idle
        for _ in range(2):
            self.controller.mainLoopIteration()
            self.assertEqual(ctHelper.readNotification(self.controller), "")


    # TODO move to a test helper module
    def checkVGRPosition(self, expectedVerticalEncoder : int , expectedRotEncoder : int, expectedArmEncoder :int) -> None:
        """verifies that the Vacuum Gripper encoder values are close enought to the expected values taking into account the simulation increment"""
        vgr = self.controller.machines[0]
        assert isinstance(vgr,VacuumGripper)
        delta = self.controller.vaccumGripperSimulator.encoderIncrement - round(self.controller.vaccumGripperSimulator.encoderIncrement/3)
        self.assertAlmostEqual(vgr.vacuumSensVerticalEncoderCounter, expectedVerticalEncoder, delta)
        self.assertAlmostEqual(vgr.vacuumSensRotEncoderCounter, expectedRotEncoder, delta)
        self.assertAlmostEqual(vgr.vacuumSensArmEncoderCounter, expectedArmEncoder, delta)
        

if __name__ == '__main__':
    logging.basicConfig(format='[%(levelname)-5s] %(module)-25s,%(lineno)-3s| %(message)s', level=logging.DEBUG)

    unittest.main()
