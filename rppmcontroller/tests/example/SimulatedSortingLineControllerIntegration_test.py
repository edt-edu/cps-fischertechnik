import inspect
import logging
import os
import re
import time
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
        logging.info(f'script path : {script_path}')
        dir_path = os.path.dirname(__file__)
        config_path = os.path.join(dir_path, "config.yml")
        logging.info(f'config file path : {config_path}')

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
        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"SortingLine01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")

        # controller is idle
        self.controller.mainLoopIteration()
        self.assertEqual(ctHelper.readMachineFeedbackNotification(self.controller), "")

        # send a eject command
        message = MachineCommand("COMMAND", "SORTING", 1, "EJECT", [Color.WHITE])

        ctHelper.sendMessage(self.controller, "SortingLine01", message)

        self.controller.mainLoopIteration()
        self.controller.mainLoopIteration()

        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"SortingLine01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_ACTIVE")

        endCommandReached = False
        iterationDone = 0
        start_time = None
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readCommandFeedbackNotification(self.controller)


            '''Simulate sensor changes for testing all the functionnalities of the command'''
            if iterationDone == 2:
                self.controller.sortingLineSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERINLET,False)
            elif iterationDone == 4:
                self.controller.sortingLineSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERINLET,True)
            elif iterationDone == 6:
                self.controller.sortingLineSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERBEHINDCOLORSENSOR,False)
                start_time = time.time()
            elif iterationDone == 8:
                self.controller.sortingLineSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERBEHINDCOLORSENSOR,True)
            logging.debug(f"{iterationDone} iterations")
            if (notification == "") :
                iterationDone += 1
            elif re.match(r"SortingLine01 \d+\.\d+ COMMAND_FEEDBACK 1 MUST_CONTINUE .*", notification):
                pass
            else:
                if start_time is not None:
                    elapsed_seconds = time.time() - start_time
                    logging.info(f"notification received in {elapsed_seconds}s and {iterationDone} iterations")
                    self.assertAlmostEqual  (elapsed_seconds, 
                                             self.controller.sortingLineMachine.WHITE_EJECTOR_DELAY + 
                                             self.controller.sortingLineMachine.EJECTOR_ACTIVATION_TIME +
                                             self.controller.mainLoopDelay*2,
                                             delta=0.1, 
                                             msg="Ejector timing for white was wrong")
                self.assertRegex(notification, r"SortingLine01 \d+\.\d+ COMMAND_FEEDBACK 1 DONE")
                notification = ctHelper.readMachineFeedbackNotification(self.controller)
                self.assertRegex(notification, r"SortingLine01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")
                endCommandReached = True
                self.controller.sortingLineSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERWHITE,True)
            self.assertLess(iterationDone, 30, "COMMAND not reached in less than 30 iterations" )


    def test_ejectRed(self):
        '''
            Test the EJECT command with a red object
        '''
        logging.debug(f'{inspect.stack()[0][3]} start')

        # initial feedback
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"SortingLine01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")

        # controller is idle
        self.controller.mainLoopIteration()
        self.assertEqual(ctHelper.readNotification(self.controller), "")

        # send a move command
        message = MachineCommand("COMMAND", "SORTING", 1, "EJECT", [Color.RED])

        ctHelper.sendMessage(self.controller, "SortingLine01", message)

        self.controller.mainLoopIteration()
        self.controller.mainLoopIteration()

        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"SortingLine01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_ACTIVE")

        endCommandReached = False
        iterationDone = 0
        start_time = None
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readCommandFeedbackNotification(self.controller)
            '''Simulate sensor changes for testing all the functionnalities of the command'''
            if iterationDone == 2:
                self.controller.sortingLineSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERINLET,False)
            elif iterationDone == 4:
                self.controller.sortingLineSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERINLET,True)
            elif iterationDone == 6:
                self.controller.sortingLineSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERBEHINDCOLORSENSOR,False)
                start_time = time.time()
            elif iterationDone == 8:
                self.controller.sortingLineSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERBEHINDCOLORSENSOR,True)
            if (notification == "") :
                iterationDone += 1
            elif re.match(r"SortingLine01 \d+\.\d+ COMMAND_FEEDBACK 1 MUST_CONTINUE .*", notification):
                pass
            else:
                if start_time is not None:
                    elapsed_seconds = time.time() - start_time
                    logging.info(f"notification received in {elapsed_seconds}s and {iterationDone} iterations")
                    self.assertAlmostEqual  (elapsed_seconds, 
                                             self.controller.sortingLineMachine.RED_EJECTOR_DELAY + 
                                             self.controller.sortingLineMachine.EJECTOR_ACTIVATION_TIME +
                                             self.controller.mainLoopDelay*2,
                                             delta=0.1, 
                                             msg="Ejector timing for red was wrong")
                self.assertRegex(notification, r"SortingLine01 \d+\.\d+ COMMAND_FEEDBACK 1 DONE")
                notification = ctHelper.readMachineFeedbackNotification(self.controller)
                self.assertRegex(notification, r"SortingLine01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")
                logging.debug(f"COMMAND DONE reached in {iterationDone} iterations")
                endCommandReached = True
                self.controller.sortingLineSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERRED,True)
            self.assertLess(iterationDone, 30, "COMMAND not reached in less than 30 iterations" )


    def test_ejectBlue(self):
        '''
            Test the EJECT command with a blue object
        '''
        logging.debug(f'{inspect.stack()[0][3]} start')

        # initial feedback
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"SortingLine01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")

        # controller is idle
        self.controller.mainLoopIteration()
        self.assertEqual(ctHelper.readMachineFeedbackNotification(self.controller), "")

        # send a move command
        message = MachineCommand("COMMAND", "SORTING", 1, "EJECT", [Color.BLUE])

        ctHelper.sendMessage(self.controller, "SortingLine01", message)

        self.controller.mainLoopIteration()
        self.controller.mainLoopIteration()

        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"SortingLine01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_ACTIVE")

        endCommandReached = False
        iterationDone = 0
        start_time = None
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readCommandFeedbackNotification(self.controller)
            '''Simulate sensor changes for testing all the functionnalities of the command'''
            if iterationDone == 2:
                self.controller.sortingLineSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERINLET,False)
            elif iterationDone == 4:
                self.controller.sortingLineSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERINLET,True)
            elif iterationDone == 6:
                self.controller.sortingLineSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERBEHINDCOLORSENSOR,False)
                start_time = time.time()
            elif iterationDone == 8:
                self.controller.sortingLineSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERBEHINDCOLORSENSOR,True)
            if (notification == "") :
                iterationDone += 1
            elif re.match(r"SortingLine01 \d+\.\d+ COMMAND_FEEDBACK 1 MUST_CONTINUE .*", notification):
                pass
            else:
                if start_time is not None:
                    elapsed_seconds = time.time() - start_time
                    logging.info(f"notification received in {elapsed_seconds}s and {iterationDone} iterations")
                    self.assertAlmostEqual  (elapsed_seconds, 
                                             self.controller.sortingLineMachine.BLUE_EJECTOR_DELAY + 
                                             self.controller.sortingLineMachine.EJECTOR_ACTIVATION_TIME +
                                             self.controller.mainLoopDelay*2,
                                             delta=0.1, 
                                             msg="Ejector timing for blue was wrong")
                self.assertRegex(notification, r"SortingLine01 \d+\.\d+ COMMAND_FEEDBACK 1 DONE")
                notification = ctHelper.readMachineFeedbackNotification(self.controller)
                self.assertRegex(notification, r"SortingLine01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")
                logging.debug(f"COMMAND DONE reached in {iterationDone} iterations")
                endCommandReached = True
                self.controller.sortingLineSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERBLUE,True)
            self.assertLess(iterationDone, 50, "COMMAND not reached in less than 50 iterations" )


    def test_ejectAuto(self):
        '''
            Test the EJECT command with an automatic sort
        '''
        logging.debug(f'{inspect.stack()[0][3]} start')

        # initial feedback
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"SortingLine01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")

        # controller is idle
        self.controller.mainLoopIteration()
        self.assertEqual(ctHelper.readNotification(self.controller), "")

        # send a move command
        message = MachineCommand("COMMAND", "SORTING", 1, "EJECT", [Color.AUTO])

        ctHelper.sendMessage(self.controller, "SortingLine01", message)

        self.controller.mainLoopIteration()
        self.controller.mainLoopIteration()

        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"SortingLine01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_ACTIVE")

        endCommandReached = False
        iterationDone = 0
        start_time  = None
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readCommandFeedbackNotification(self.controller)
            '''Simulate sensor changes for testing all the functionnalities of the command'''
            if iterationDone == 2:
                self.controller.sortingLineSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERINLET,False)
            elif iterationDone == 4:
                self.controller.sortingLineSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERINLET,True)
            elif iterationDone == 6:
                self.controller.sortingLineSimulator.fakeSensor(RequestedParameter.SENSCOLORDETECTOR,True)
                self.controller.sortingLineSimulator.fakeSensor(RequestedParameter.SENSREDDETECTOR,True)
            elif iterationDone == 8:
                self.controller.sortingLineSimulator.fakeSensor(RequestedParameter.SENSCOLORDETECTOR,False)
                self.controller.sortingLineSimulator.fakeSensor(RequestedParameter.SENSREDDETECTOR,False)
            elif iterationDone == 10:
                self.controller.sortingLineSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERBEHINDCOLORSENSOR,False)
                start_time = time.time()
            elif iterationDone == 12:
                self.controller.sortingLineSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERBEHINDCOLORSENSOR,True)
            if (notification == "") :
                iterationDone += 1
            elif re.match(r"SortingLine01 \d+\.\d+ COMMAND_FEEDBACK 1 MUST_CONTINUE .*", notification):
                pass
            else:
                if start_time is not None:
                    elapsed_seconds = time.time() - start_time
                    logging.info(f"notification received in {elapsed_seconds}s and {iterationDone} iterations")
                    self.assertAlmostEqual  (elapsed_seconds, 
                                             self.controller.sortingLineMachine.RED_EJECTOR_DELAY + 
                                             self.controller.sortingLineMachine.EJECTOR_ACTIVATION_TIME +
                                             self.controller.mainLoopDelay*2,
                                             delta=0.1, 
                                             msg="Ejector timing for red was wrong")
                self.assertEqual(31, iterationDone, "Timing of ejector for red was wrong")  # I'm not sure this test can be accurate 
                self.assertRegex(notification, r"SortingLine01 \d+\.\d+ COMMAND_FEEDBACK 1 DONE")
                notification = ctHelper.readMachineFeedbackNotification(self.controller)
                self.assertRegex(notification, r"SortingLine01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")
                logging.debug(f"COMMAND DONE reached in {iterationDone} iterations")
                endCommandReached = True
                self.controller.sortingLineSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERRED,True)
            self.assertLess(iterationDone, 50, "COMMAND not reached in less than 50 iterations" )

    
    def test_ejectAutoError(self):
        '''
            Test the EJECT command ability to deal with the lack or detection while doing an automatic sort
        '''
        logging.debug(f'{inspect.stack()[0][3]} start')

        # initial feedback
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"SortingLine01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")

        # controller is idle
        self.controller.mainLoopIteration()
        self.assertEqual(ctHelper.readNotification(self.controller), "")

        # send a move command
        message = MachineCommand("COMMAND", "SORTING", 1, "EJECT", [Color.AUTO])

        ctHelper.sendMessage(self.controller, "SortingLine01", message)

        self.controller.mainLoopIteration()
        self.controller.mainLoopIteration()

        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"SortingLine01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_ACTIVE")

        endCommandReached = False
        iterationDone = 0
        start_time = None
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readCommandFeedbackNotification(self.controller)
            '''Simulate sensor changes for testing all the functionnalities of the command'''
            if iterationDone == 2:
                self.controller.sortingLineSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERINLET,False)
            elif iterationDone == 4:
                self.controller.sortingLineSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERINLET,True)
            elif iterationDone == 6:
                self.controller.sortingLineSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERBEHINDCOLORSENSOR,False)
                start_time = time.time()
            elif iterationDone == 8:
                self.controller.sortingLineSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERBEHINDCOLORSENSOR,True)
            if (notification == "") :
                iterationDone += 1
            elif re.match(r"SortingLine01 \d+\.\d+ COMMAND_FEEDBACK 1 MUST_CONTINUE .*", notification):
                pass
            else:
                if start_time is not None:
                    elapsed_seconds = time.time() - start_time
                    logging.info(f"notification received in {elapsed_seconds}s and {iterationDone} iterations")
                    self.assertAlmostEqual  (elapsed_seconds, 
                                             self.controller.mainLoopDelay,
                                             delta=0.1, 
                                             msg="Ejector timing for blue was wrong")
                self.assertRegex(notification, r"SortingLine01 \d+\.\d+ COMMAND_FEEDBACK 1 ABORTED_ERROR")
                endCommandReached = True
            self.assertLess(iterationDone, 30, "Goal not reached in less than 30 iterations" )

if __name__ == '__main__':
    logging.basicConfig(format='[%(levelname)-5s] %(module)-25s,%(lineno)-3s| %(message)s', level=logging.DEBUG)

    unittest.main()
