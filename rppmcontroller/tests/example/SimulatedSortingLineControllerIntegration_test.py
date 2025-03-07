import inspect
import logging
import os
import unittest
from unittest.mock import patch, Mock

from rppmcontroller.example.SimulatedSortingLineController import SimulatedSortingLineController
from rppmcontroller.machine.sortingLine.SortingLine import SortingLine
from rppmcontroller.machine.RequestedParameter import RequestedParameter
from rppmcontroller.machine.Color import Color
from rppmcontroller.protocol.MachineCommand import MachineCommand

import tests.controllerTestHelper as ctHelper


class SimulatedSortingLineControllerIntegrationTestCase(unittest.TestCase):

    def setUp(self):
        script_path = os.path.abspath(__file__)
        logging.warning(f'script path : {script_path}')
        dir_path = os.path.dirname(__file__)
        config_path = os.path.join(dir_path, "config.yml")
        logging.warning(f'config file path : {config_path}')

        logging.debug("setup called")
        self.controller = SimulatedSortingLineController(config_path)
        self.controller.mainLoopDelay = 0.1

    ### ________ EJECT ___________
    def test_ejectWhite(self):
        '''
            Test the EJECT command with a white object
        '''
        logging.debug(f'{inspect.stack()[0][3]} start')

        # initial feedback
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readNotification(self.controller), r"SortingLine01 \d+\.\d+ MACHINE_FEEDBACK FINISHED")

        # controller is idle
        self.controller.mainLoopIteration()
        self.assertEqual(ctHelper.readNotification(self.controller), "")

        # send a eject command
        message = MachineCommand("COMMAND", "SORTING", 1, "EJECT", [Color.WHITE])
        
        ctHelper.sendMessage(self.controller, "SortingLine01", message)
        
        self.controller.mainLoopIteration()
        self.controller.mainLoopIteration()

        self.assertRegex(ctHelper.readNotification(self.controller), r"SortingLine01 \d+\.\d+ MACHINE_FEEDBACK INACTION")

        endCommandReached = False
        iterationDone = 0
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readNotification(self.controller)
            
        
            '''Simulate sensor changes for testing all the functionnalities of the command'''
            if iterationDone == 2:
                self.controller.sortingLineSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERINLET,False)
            elif iterationDone == 4:
                self.controller.sortingLineSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERINLET,True)
            elif iterationDone == 6:
                self.controller.sortingLineSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERBEHINDCOLORSENSOR,False)
            elif iterationDone == 8:
                self.controller.sortingLineSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERBEHINDCOLORSENSOR,True)
            if iterationDone >= 8 and self.controller.sortingLineSimulator.getCounter() == 2:
                self.controller.sortingLineSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERWHITE,False)
            logging.debug(f"{iterationDone} iterations")
            if (notification == "") :
                iterationDone += 1
            else:
                self.assertRegex(notification, r"SortingLine01 \d+\.\d+ MACHINE_FEEDBACK FINISHED")
                notification = ctHelper.readCommandFeedbackNotification(self.controller)
                self.assertRegex(notification, r"SortingLine01 \d+\.\d+ COMMAND_FEEDBACK 1 SUCCESS")
                logging.debug(f"COMMAND FINISHED reached in {iterationDone} iterations")
                endCommandReached = True
                self.controller.sortingLineSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERWHITE,True)
            self.assertLess(iterationDone, 30, "COMMAND not reached in less than 30 iterations" )

    def test_ejectBlue(self):
        '''
            Test the EJECT command with a blue object
        '''
        logging.debug(f'{inspect.stack()[0][3]} start')

        # initial feedback
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readNotification(self.controller), r"SortingLine01 \d+\.\d+ MACHINE_FEEDBACK FINISHED")

        # controller is idle
        self.controller.mainLoopIteration()
        self.assertEqual(ctHelper.readNotification(self.controller), "")

        # send a move command
        message = MachineCommand("COMMAND", "SORTING", 1, "EJECT", [Color.BLUE])
        
        ctHelper.sendMessage(self.controller, "SortingLine01", message)
        
        self.controller.mainLoopIteration()
        self.controller.mainLoopIteration()

        self.assertRegex(ctHelper.readNotification(self.controller), r"SortingLine01 \d+\.\d+ MACHINE_FEEDBACK INACTION")

        endCommandReached = False
        iterationDone = 0
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readNotification(self.controller)
            '''Simulate sensor changes for testing all the functionnalities of the command'''
            if iterationDone == 2:
                self.controller.sortingLineSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERINLET,False)
            elif iterationDone == 4:
                self.controller.sortingLineSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERINLET,True)
            elif iterationDone == 6:
                self.controller.sortingLineSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERBEHINDCOLORSENSOR,False)
            elif iterationDone == 8:
                self.controller.sortingLineSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERBEHINDCOLORSENSOR,True)
            if iterationDone >= 8 and self.controller.sortingLineSimulator.getCounter() == 3:
                self.controller.sortingLineSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERBLUE,False)
            
            if (notification == "") :
                iterationDone += 1
            else:
                self.assertRegex(notification, r"SortingLine01 \d+\.\d+ MACHINE_FEEDBACK FINISHED")
                notification = ctHelper.readCommandFeedbackNotification(self.controller)
                self.assertRegex(notification, r"SortingLine01 \d+\.\d+ COMMAND_FEEDBACK 1 SUCCESS")
                logging.debug(f"COMMAND FINISHED reached in {iterationDone} iterations")
                endCommandReached = True
                self.controller.sortingLineSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERBLUE,True)
            self.assertLess(iterationDone, 30, "COMMAND not reached in less than 30 iterations" )

    def test_ejectRed(self):
        '''
            Test the EJECT command with a red object
        '''
        logging.debug(f'{inspect.stack()[0][3]} start')

        # initial feedback
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readNotification(self.controller), r"SortingLine01 \d+\.\d+ MACHINE_FEEDBACK FINISHED")

        # controller is idle
        self.controller.mainLoopIteration()
        self.assertEqual(ctHelper.readNotification(self.controller), "")

        # send a move command
        message = MachineCommand("COMMAND", "SORTING", 1, "EJECT", [Color.RED])
        
        ctHelper.sendMessage(self.controller, "SortingLine01", message)
        
        self.controller.mainLoopIteration()
        self.controller.mainLoopIteration()

        self.assertRegex(ctHelper.readNotification(self.controller), r"SortingLine01 \d+\.\d+ MACHINE_FEEDBACK INACTION")

        endCommandReached = False
        iterationDone = 0
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readNotification(self.controller)
            '''Simulate sensor changes for testing all the functionnalities of the command'''
            if iterationDone == 2:
                self.controller.sortingLineSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERINLET,False)
            elif iterationDone == 4:
                self.controller.sortingLineSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERINLET,True)
            elif iterationDone == 6:
                self.controller.sortingLineSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERBEHINDCOLORSENSOR,False)
            elif iterationDone == 8:
                self.controller.sortingLineSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERBEHINDCOLORSENSOR,True)
            if iterationDone >= 8 and self.controller.sortingLineSimulator.getCounter() == 5:
                self.controller.sortingLineSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERRED,False)
            
            if (notification == "") :
                iterationDone += 1
            else:
                self.assertRegex(notification, r"SortingLine01 \d+\.\d+ MACHINE_FEEDBACK FINISHED")
                notification = ctHelper.readCommandFeedbackNotification(self.controller)
                self.assertRegex(notification, r"SortingLine01 \d+\.\d+ COMMAND_FEEDBACK 1 SUCCESS")
                logging.debug(f"COMMAND FINISHED reached in {iterationDone} iterations")
                endCommandReached = True
                self.controller.sortingLineSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERRED,True)
            self.assertLess(iterationDone, 30, "COMMAND not reached in less than 30 iterations" )
 
if __name__ == '__main__':
    logging.basicConfig(format='[%(levelname)-5s] %(module)-25s,%(lineno)-3s| %(message)s', level=logging.DEBUG)

    unittest.main()