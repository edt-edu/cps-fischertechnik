
import inspect
import logging
import os
import re
import unittest
from unittest.mock import patch, Mock

from rppmcontroller.example.SimulatedVacuumGripperController import SimulatedVacuumGripperController
from rppmcontroller.machine.vacuumgripper.VacuumGripper import VacuumGripper
from rppmcontroller.machine.Position import Position
from rppmcontroller.protocol.MachineCommand import MachineCommand

import tests.controllerTestHelper as ctHelper


class SimulatedVacuumGripperControllerIntegrationTestCase(unittest.TestCase):

    def setUp(self):
        script_path = os.path.abspath(__file__)
        logging.info(f'script path : {script_path}')
        dir_path = os.path.dirname(__file__)
        config_path = os.path.join(dir_path, "config.yml")
        logging.info(f'config file path : {config_path}')


        logging.debug("setup called")
        self.controller = SimulatedVacuumGripperController(config_path)
        self.controller.mainLoopDelay = 0.05


    def test_setupCommand(self):
        """Ensure that the setup command is performed and and send feedback"""
        logging.debug(f'{inspect.stack()[0][3]} start')

        # initial feedback
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"VacuumGripper01 \d+\.\d+ MACHINE_FEEDBACK UNINITIALIZED_IDLE")

        # controller is idle
        self.controller.mainLoopIteration()
        self.assertEqual(ctHelper.readMachineFeedbackNotification(self.controller), "")

        # send a setup command
        message = MachineCommand("COMMAND", "VACUUM", 1, "SETUP", [])
        ctHelper.sendMessage(self.controller, "VacuumGripper01", message)

        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"VacuumGripper01 \d+\.\d+ MACHINE_FEEDBACK UNINITIALIZED_ACTIVE")

        endCommandReached = False
        iterationDone = 0
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readMachineFeedbackNotification(self.controller)
            if notification == "":
                iterationDone += 1
            elif re.match(r"VacuumGripper01 \d+\.\d+ COMMAND_FEEDBACK 1 MUST_CONTINUE .*", notification):
                pass
            else:
                self.assertRegex(notification, r"VacuumGripper01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")
                logging.debug(f"Setup DONE reached in {iterationDone} iterations")
                endCommandReached = True
            self.assertLess(iterationDone, 20, "Setup DONE not reached in less than 20 iterations" )

        # controller is idle
        for _ in range(5):
            self.controller.mainLoopIteration()
            self.assertEqual(ctHelper.readMachineFeedbackNotification(self.controller), "")

        # check current position via feedback and/or by reading machine IO
        self.checkVGRPosition(0,0,0)



    def test_twoSetupCommands(self):
        """Ensure that the setup command is performed and and send feedback"""
        logging.debug(f'{inspect.stack()[0][3]} start')

        # initial feedback
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"VacuumGripper01 \d+\.\d+ MACHINE_FEEDBACK UNINITIALIZED_IDLE")

        # controller is idle
        self.controller.mainLoopIteration()
        self.assertEqual(ctHelper.readMachineFeedbackNotification(self.controller), "")

        # send a setup command
        message = MachineCommand("COMMAND", "VACUUM", 1, "SETUP", [])
        ctHelper.sendMessage(self.controller, "VacuumGripper01", message)

        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"VacuumGripper01 \d+\.\d+ MACHINE_FEEDBACK UNINITIALIZED_ACTIVE")

        endCommandReached = False
        iterationDone = 0
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readCommandFeedbackNotification(self.controller)
            if notification == "":
                iterationDone += 1
            elif re.match(r"VacuumGripper01 \d+\.\d+ COMMAND_FEEDBACK 1 MUST_CONTINUE .*", notification):
                pass
            else:
                self.assertRegex(notification, r"VacuumGripper01 \d+\.\d+ COMMAND_FEEDBACK 1 DONE")
                notification = ctHelper.readMachineFeedbackNotification(self.controller)
                self.assertRegex(notification, r"VacuumGripper01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")
                logging.debug(f"Setup DONE reached in {iterationDone} iterations")
                endCommandReached = True
            self.assertLess(iterationDone, 20, "Setup DONE not reached in less than 20 iterations" )

        # check current position via feedback and/or by reading machine IO
        self.checkVGRPosition(0,0,0)

        # controller is idle
        for _ in range(5):
            self.controller.mainLoopIteration()
            self.assertEqual(ctHelper.readMachineFeedbackNotification(self.controller), "")


        # send a second setup command
        message = MachineCommand("COMMAND", "VACUUM", 2, "SETUP", [])
        ctHelper.sendMessage(self.controller, "VacuumGripper01", message)

        self.controller.mainLoopIteration()
        # already on the sensor, so we get an immediate SUCCESS
        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"VacuumGripper01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_ACTIVE")

        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readCommandFeedbackNotification(self.controller), r"VacuumGripper01 \d+\.\d+ COMMAND_FEEDBACK 2 DONE")

        endCommandReached = False
        iterationDone = 0
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readMachineFeedbackNotification(self.controller)
            if notification == "":
                iterationDone += 1
            else:
                self.assertRegex(notification, r"VacuumGripper01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")
                logging.debug(f"Setup DONE reached in {iterationDone} iterations")
                endCommandReached = True
            self.assertLess(iterationDone, 20, "Setup DONE not reached in less than 20 iterations" )


        # TODO check current position via feedback and/or by reading machine IO

        # controller is idle
        for _ in range(5):
            self.controller.mainLoopIteration()
            self.assertEqual(ctHelper.readMachineFeedbackNotification(self.controller), "")
        # check current position via feedback and/or by reading machine IO
        self.checkVGRPosition(0,0,0)



    def test_moveCommand_with_internal_setup(self):
        """Ensure that the pick command is performed and and send feedback"""
        logging.debug(f'{inspect.stack()[0][3]} start')

        # initial feedback
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"VacuumGripper01 \d+\.\d+ MACHINE_FEEDBACK UNINITIALIZED_IDLE")

        # controller is idle
        self.controller.mainLoopIteration()
        self.assertEqual(ctHelper.readMachineFeedbackNotification(self.controller), "")

        # send a setup command
        message = MachineCommand("COMMAND", "VACUUM", 1, "MOVE", [
            Position("START", 500, 200, 400),
            Position("END", 500, 1000, 1200)
        ])
        ctHelper.sendMessage(self.controller, "VacuumGripper01", message)

        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"VacuumGripper01 \d+\.\d+ MACHINE_FEEDBACK UNINITIALIZED_ACTIVE")

        endCommandReached = False
        iterationDone = 0
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readCommandFeedbackNotification(self.controller)
            if notification == "":
                iterationDone += 1
            elif re.match(r"VacuumGripper01 \d+\.\d+ COMMAND_FEEDBACK 1 MUST_CONTINUE .*", notification):
                pass
            else:
                self.assertRegex(notification, r"VacuumGripper01 \d+\.\d+ COMMAND_FEEDBACK 1 DONE")
                notification = ctHelper.readMachineFeedbackNotification(self.controller)
                self.assertRegex(notification, r"VacuumGripper01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")
                logging.debug(f"MOVE DONE reached in {iterationDone} iterations")
                endCommandReached = True
            self.assertLess(iterationDone, 200, "MOVE DONE not reached in less than 200 iterations" )



        # check current position via feedback and/or by reading machine IO
        self.checkVGRPosition(500 - 250,1000,1200) # 250 is the offset of the move command # TODO have a better management of this offset

        # controller is idle
        for _ in range(2):
            self.controller.mainLoopIteration()
            self.assertEqual(ctHelper.readMachineFeedbackNotification(self.controller), "")


    def test_setup_then_moveCommands(self):
        """Ensure that the setup command is performed and and send feedback"""
        logging.debug(f'{inspect.stack()[0][3]} start')

        # initial feedback
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"VacuumGripper01 \d+\.\d+ MACHINE_FEEDBACK UNINITIALIZED_IDLE")

        # controller is idle
        self.controller.mainLoopIteration()
        self.assertEqual(ctHelper.readMachineFeedbackNotification(self.controller), "")

        # send a setup command
        message = MachineCommand("COMMAND", "VACUUM", 1, "SETUP", [])
        ctHelper.sendMessage(self.controller, "VacuumGripper01", message)

        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"VacuumGripper01 \d+\.\d+ MACHINE_FEEDBACK UNINITIALIZED_ACTIVE")

        endCommandReached = False
        iterationDone = 0
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readCommandFeedbackNotification(self.controller)
            if notification == "":
                iterationDone += 1
            elif re.match(r"VacuumGripper01 \d+\.\d+ COMMAND_FEEDBACK 1 MUST_CONTINUE .*", notification):
                pass
            else:
                self.assertRegex(notification, r"VacuumGripper01 \d+\.\d+ COMMAND_FEEDBACK 1 DONE")
                notification = ctHelper.readMachineFeedbackNotification(self.controller)
                self.assertRegex(notification, r"VacuumGripper01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")
                logging.debug(f"Setup DONE reached in {iterationDone} iterations")
                endCommandReached = True
            self.assertLess(iterationDone, 20, "Setup DONE not reached in less than 20 iterations" )

        # controller is idle
        for _ in range(2):
            self.controller.mainLoopIteration()
            self.assertEqual(ctHelper.readMachineFeedbackNotification(self.controller), "")

        # send a setup command
        message = MachineCommand("COMMAND", "VACUUM", 2, "MOVE", [
            Position("START", 500, 200, 400),
            Position("END", 500, 1000, 1200)
        ])
        ctHelper.sendMessage(self.controller, "VacuumGripper01", message)

        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"VacuumGripper01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_ACTIVE")

        endCommandReached = False
        iterationDone = 0
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readCommandFeedbackNotification(self.controller)
            if notification == "":
                iterationDone += 1
            elif re.match(r"VacuumGripper01 \d+\.\d+ COMMAND_FEEDBACK 2 MUST_CONTINUE .*", notification):
                pass
            else:
                self.assertGreater(iterationDone, 50, "MOVE COMMAND_FEEDBACK reached in less than 50 iterations, it was probably not done" )
                self.assertRegex(notification, r"VacuumGripper01 \d+\.\d+ COMMAND_FEEDBACK 2 DONE")
                notification = ctHelper.readMachineFeedbackNotification(self.controller)
                self.assertRegex(notification, r"VacuumGripper01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")
                logging.debug(f"MOVE DONE reached in {iterationDone} iterations")
                endCommandReached = True
            self.assertLess(iterationDone, 400, "MOVE DONE not reached in less than 400 iterations" )



        # check current position via feedback and/or by reading machine IO
        self.checkVGRPosition(500 - 250, 1000, 1200) # 250 is the offset of the move command # TODO have a better management of this offset

        # controller is idle
        for _ in range(2):
            self.controller.mainLoopIteration()
            self.assertEqual(ctHelper.readMachineFeedbackNotification(self.controller), "")


    def test_two_identical_moveCommands(self):
        """Ensure that the pick command is performed and and send feedback"""
        logging.debug(f'{inspect.stack()[0][3]} start')


        self.fakeSetupDoneAndSetPos()

        # initial feedback
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"VacuumGripper01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")

        # controller is idle
        self.controller.mainLoopIteration()
        self.assertEqual(ctHelper.readMachineFeedbackNotification(self.controller), "")

        # send a setup command
        message = MachineCommand("COMMAND", "VACUUM", 1, "MOVE", [
            Position("START", 500, 200, 400),
            Position("END", 500, 1000, 1200)
        ])
        ctHelper.sendMessage(self.controller, "VacuumGripper01", message)

        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"VacuumGripper01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_ACTIVE")

        endCommandReached = False
        iterationDone = 0
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readCommandFeedbackNotification(self.controller)
            if notification == "":
                iterationDone += 1
            elif re.match(r"VacuumGripper01 \d+\.\d+ COMMAND_FEEDBACK 1 MUST_CONTINUE .*", notification):
                pass
            else:
                self.assertGreater(iterationDone, 50, "MOVE INITIALIZED_IDLE reached in less than 50 iterations, it was probably not done" )
                self.assertRegex(notification, r"VacuumGripper01 \d+\.\d+ COMMAND_FEEDBACK 1 DONE")
                notification = ctHelper.readMachineFeedbackNotification(self.controller)
                self.assertRegex(notification, r"VacuumGripper01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")
                logging.debug(f"MOVE DONE reached in {iterationDone} iterations")
                endCommandReached = True
            self.assertLess(iterationDone, 200, "MOVE DONE not reached in less than 200 iterations" )



        # check current position via feedback and/or by reading machine IO
        self.checkVGRPosition(500 - 250,1000,1200) # 250 is the offset of the move command # TODO have a better management of this offset

        # controller is idle
        for _ in range(2):
            self.controller.mainLoopIteration()
            self.assertEqual(ctHelper.readMachineFeedbackNotification(self.controller), "")


        # send a setup command
        message = MachineCommand("COMMAND", "VACUUM", 2, "MOVE", [
            Position("START", 500, 200, 400),
            Position("END", 500, 1000, 1200)
        ])
        ctHelper.sendMessage(self.controller, "VacuumGripper01", message)

        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"VacuumGripper01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_ACTIVE")

        endCommandReached = False
        iterationDone = 0
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readCommandFeedbackNotification(self.controller)
            if notification == "":
                iterationDone += 1
            elif re.match(r"VacuumGripper01 \d+\.\d+ COMMAND_FEEDBACK 2 MUST_CONTINUE .*", notification):
                pass
            else:
                self.assertGreater(iterationDone, 50, "MOVE INITIALIZED_IDLE reached in less than 50 iterations, it was probably not done" )
                self.assertRegex(notification, r"VacuumGripper01 \d+\.\d+ COMMAND_FEEDBACK 2 DONE")
                notification = ctHelper.readMachineFeedbackNotification(self.controller)
                self.assertRegex(notification, r"VacuumGripper01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")
                logging.debug(f"MOVE DONE reached in {iterationDone} iterations")
                endCommandReached = True
            self.assertLess(iterationDone, 200, "MOVE DONE not reached in less than 200 iterations" )



        # check current position via feedback and/or by reading machine IO
        self.checkVGRPosition(500 - 250,1000,1200) # 250 is the offset of the move command # TODO have a better management of this offset


    def test_two_different_moveCommands(self):
        """Ensure that the pick command is performed and and send feedback"""
        logging.debug(f'{inspect.stack()[0][3]} start')


        self.fakeSetupDoneAndSetPos()

        # initial feedback
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"VacuumGripper01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")

        # controller is idle
        self.controller.mainLoopIteration()
        self.assertEqual(ctHelper.readMachineFeedbackNotification(self.controller), "")

        # send a move command
        message = MachineCommand("COMMAND", "VACUUM", 1, "MOVE", [
            Position("START", 500, 200, 400),
            Position("END", 500, 1000, 1200)
        ])
        ctHelper.sendMessage(self.controller, "VacuumGripper01", message)

        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"VacuumGripper01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_ACTIVE")

        endCommandReached = False
        iterationDone = 0
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readCommandFeedbackNotification(self.controller)
            if notification == "":
                iterationDone += 1
            elif re.match(r"VacuumGripper01 \d+\.\d+ COMMAND_FEEDBACK 1 MUST_CONTINUE .*", notification):
                pass
            else:
                self.assertGreater(iterationDone, 50, "COMMAND_FEEDBACK reached in less than 50 iterations, it was probably not done" )
                self.assertRegex(notification, r"VacuumGripper01 \d+\.\d+ COMMAND_FEEDBACK 1 DONE")
                notification = ctHelper.readMachineFeedbackNotification(self.controller)
                self.assertRegex(notification, r"VacuumGripper01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")
                logging.debug(f"MOVE DONE reached in {iterationDone} iterations")
                endCommandReached = True
            self.assertLess(iterationDone, 200, "MOVE DONE not reached in less than 200 iterations" )



        # check current position via feedback and/or by reading machine IO
        self.checkVGRPosition(500 - 250,1000,1200) # 250 is the offset of the move command # TODO have a better management of this offset

        # controller is idle
        for _ in range(2):
            self.controller.mainLoopIteration()
            self.assertEqual(ctHelper.readMachineFeedbackNotification(self.controller), "")


        # send a move command
        message = MachineCommand("COMMAND", "VACUUM", 2, "MOVE", [
            Position("START", 550, 250, 450),
            Position("END", 550, 1050, 1250)
        ])
        ctHelper.sendMessage(self.controller, "VacuumGripper01", message)

        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"VacuumGripper01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_ACTIVE")

        endCommandReached = False
        iterationDone = 0
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readCommandFeedbackNotification(self.controller)
            if notification == "":
                iterationDone += 1
            elif re.match(r"VacuumGripper01 \d+\.\d+ COMMAND_FEEDBACK 2 MUST_CONTINUE .*", notification):
                pass
            else:
                self.assertRegex(notification, r"VacuumGripper01 \d+\.\d+ COMMAND_FEEDBACK 2 DONE")
                self.assertGreater(iterationDone, 50, "MOVE DONE reached in less than 50 iterations, it was probably not done" )
                notification = ctHelper.readMachineFeedbackNotification(self.controller)
                self.assertRegex(notification, r"VacuumGripper01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")
                logging.debug(f"MOVE DONE reached in {iterationDone} iterations")
                endCommandReached = True
            self.assertLess(iterationDone, 200, "MOVE DONE not reached in less than 200 iterations" )



        # check current position via feedback and/or by reading machine IO
        self.checkVGRPosition(550 - 250,1050,1250) # 250 is the offset of the move command # TODO have a better management of this offset



    def test_placeCommand(self):
        """Ensure that the pick command is performed and and send feedback"""
        logging.debug(f'{inspect.stack()[0][3]} start')

        self.fakeSetupDoneAndSetPos()

        # initial feedback
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"VacuumGripper01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")

        # controller is idle
        self.controller.mainLoopIteration()
        self.assertEqual(ctHelper.readMachineFeedbackNotification(self.controller), "")

        # send a place command
        message = MachineCommand("COMMAND", "VACUUM", 1, "PLACE", [
            Position("END", 500, 0, 1200)
        ])
        ctHelper.sendMessage(self.controller, "VacuumGripper01", message)

        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"VacuumGripper01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_ACTIVE")

        endCommandReached = False
        iterationDone = 0
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readCommandFeedbackNotification(self.controller)
            if notification == "":
                iterationDone += 1
            elif re.match(r"VacuumGripper01 \d+\.\d+ COMMAND_FEEDBACK 1 MUST_CONTINUE .*", notification):
                pass
            else:
                self.assertGreater(iterationDone, 40, "COMMAND_FEEDBACK reached in less than 40 iterations, it was probably not done" )
                self.assertRegex(notification, r"VacuumGripper01 \d+\.\d+ COMMAND_FEEDBACK 1 DONE")
                notification = ctHelper.readMachineFeedbackNotification(self.controller)
                self.assertRegex(notification, r"VacuumGripper01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")
                logging.debug(f"PLACE DONE reached in {iterationDone} iterations")
                endCommandReached = True
            self.assertLess(iterationDone, 200, "PLACE DONE not reached in less than 200 iterations" )


    def test_placeCommand2(self):
        """Ensure that the pick command is performed and and send feedback"""
        logging.debug(f'{inspect.stack()[0][3]} start')

        self.fakeSetupDoneAndSetPos()

        # initial feedback
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"VacuumGripper01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")

        # controller is idle
        self.controller.mainLoopIteration()
        self.assertEqual(ctHelper.readMachineFeedbackNotification(self.controller), "")

        # send a place command
        message = MachineCommand("COMMAND", "VACUUM", 1, "PLACE", [
            Position("END", 500, 0, 1200)
        ])
        ctHelper.sendMessage(self.controller, "VacuumGripper01", message)

        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"VacuumGripper01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_ACTIVE")

        endCommandReached = False
        iterationDone = 0
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readCommandFeedbackNotification(self.controller)
            if notification == "":
                iterationDone += 1
            elif re.match(r"VacuumGripper01 \d+\.\d+ COMMAND_FEEDBACK 1 MUST_CONTINUE .*", notification):
                pass
            else:
                self.assertGreater(iterationDone, 40, "COMMAND_FEEDBACK reached in less than 40 iterations, it was probably not done" )
                self.assertRegex(notification, r"VacuumGripper01 \d+\.\d+ COMMAND_FEEDBACK 1 DONE")
                notification = ctHelper.readMachineFeedbackNotification(self.controller)
                self.assertRegex(notification, r"VacuumGripper01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")
                logging.debug(f"PLACE DONE reached in {iterationDone} iterations")
                endCommandReached = True
            self.assertLess(iterationDone, 200, "PLACE DONE not reached in less than 200 iterations" )


    def test_placeCommandFromOtherPos(self):
        """Ensure that the pick command is performed and and send feedback"""
        logging.debug(f'{inspect.stack()[0][3]} start')

        self.fakeSetupDoneAndSetPos(100, -69, 1000)

        # initial feedback
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"VacuumGripper01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")

        # controller is idle
        self.controller.mainLoopIteration()
        self.assertEqual(ctHelper.readMachineFeedbackNotification(self.controller), "")

        # send a place command
        message = MachineCommand("COMMAND", "VACUUM", 1, "PLACE", [
            Position("END", 500, 0, 1200)
        ])
        ctHelper.sendMessage(self.controller, "VacuumGripper01", message)

        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"VacuumGripper01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_ACTIVE")

        endCommandReached = False
        iterationDone = 0
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readCommandFeedbackNotification(self.controller)
            if notification == "":
                iterationDone += 1
            elif re.match(r"VacuumGripper01 \d+\.\d+ COMMAND_FEEDBACK 1 MUST_CONTINUE .*", notification):
                pass
            else:
                self.assertGreater(iterationDone, 50, "COMMAND_FEEDBACK reached in less than 50 iterations, it was probably not done" )
                self.assertRegex(notification, r"VacuumGripper01 \d+\.\d+ COMMAND_FEEDBACK 1 DONE")
                notification = ctHelper.readMachineFeedbackNotification(self.controller)
                self.assertRegex(notification, r"VacuumGripper01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")
                logging.debug(f"PLACE DONE reached in {iterationDone} iterations")
                endCommandReached = True
            self.assertLess(iterationDone, 200, "PLACE DONE not reached in less than 200 iterations" )




        # check current position via feedback and/or by reading machine IO
        self.checkVGRPosition(500 - 250,0,1200) # 250 is the offset of the move command # TODO have a better management of this offset

        # controller is idle
        for _ in range(2):
            self.controller.mainLoopIteration()
            self.assertEqual(ctHelper.readMachineFeedbackNotification(self.controller), "")

    def test_grip_releaseCommand(self):
        """Ensure that the grip and release commands are performed and and send feedback"""
        logging.debug(f'{inspect.stack()[0][3]} start')

        self.fakeSetupDoneAndSetPos()

        # initial feedback
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"VacuumGripper01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")

        # controller is idle
        self.controller.mainLoopIteration()
        self.assertEqual(ctHelper.readMachineFeedbackNotification(self.controller), "")

        # send a grip command
        message = MachineCommand("COMMAND", "VACUUM", 1, "GRIP", [])
        ctHelper.sendMessage(self.controller, "VacuumGripper01", message)


        endCommandReached = False
        iterationDone = 0
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readCommandFeedbackNotification(self.controller)
            if notification == "":
                iterationDone += 1
            elif re.match(r"VacuumGripper01 \d+\.\d+ COMMAND_FEEDBACK 1 MUST_CONTINUE .*", notification):
                pass
            else:
                self.assertRegex(notification, r"VacuumGripper01 \d+\.\d+ COMMAND_FEEDBACK 1 DONE")
                notification = ctHelper.readMachineFeedbackNotification(self.controller)
                self.assertRegex(notification, r"VacuumGripper01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_ACTIVE") # grip implies that the machine is active after execution
                logging.debug(f"GRIP DONE reached in {iterationDone} iterations")
                endCommandReached = True
            self.assertLess(iterationDone, 10, "GRIP DONE not reached in less than 10 iterations" )


        # send a release command
        message = MachineCommand("COMMAND", "VACUUM", 2, "RELEASE", [])
        ctHelper.sendMessage(self.controller, "VacuumGripper01", message)


        endCommandReached = False
        iterationDone = 0
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readCommandFeedbackNotification(self.controller)
            if notification == "":
                iterationDone += 1
            elif re.match(r"VacuumGripper01 \d+\.\d+ COMMAND_FEEDBACK 2 MUST_CONTINUE .*", notification):
                pass
            else:
                self.assertRegex(notification, r"VacuumGripper01 \d+\.\d+ COMMAND_FEEDBACK 2 DONE")


                self.controller.mainLoopIteration()
                notification = ctHelper.readMachineFeedbackNotification(self.controller)

                self.assertRegex(notification, r"VacuumGripper01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")
                logging.debug(f"RELEASE DONE reached in {iterationDone} iterations")
                endCommandReached = True
            self.assertLess(iterationDone, 20, "RELEASE DONE not reached in less than 20 iterations" )


    def test_go_to_position_grip_off_Command(self):
        """Ensure that the go to position command is executed and does not change the state of the valve and pump.
           Case 1: Pump and valve are off
        """
        logging.debug(f'{inspect.stack()[0][3]} start')

        self.fakeSetupDoneAndSetPos()

        # initial feedback
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"VacuumGripper01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")

        # controller is idle
        self.controller.mainLoopIteration()
        self.assertEqual(ctHelper.readMachineFeedbackNotification(self.controller), "")

        # send a grip command
        message = MachineCommand("COMMAND", "VACUUM", 1, "GO_TO_POSITION", [
            Position("END", 500, 0, 1200)
        ])
        ctHelper.sendMessage(self.controller, "VacuumGripper01", message)


        endCommandReached = False
        iterationDone = 0
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readCommandFeedbackNotification(self.controller)
            if notification == "":
                iterationDone += 1
            elif re.match(r"VacuumGripper01 \d+\.\d+ COMMAND_FEEDBACK 1 MUST_CONTINUE .*", notification):
                pass
            else:
                self.assertRegex(notification, r"VacuumGripper01 \d+\.\d+ COMMAND_FEEDBACK 1 DONE")
                notification = ctHelper.readMachineFeedbackNotification(self.controller)
                self.assertRegex(notification, r"VacuumGripper01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")
                logging.debug(f"GO_TO_POSITION reached in {iterationDone} iterations")
                endCommandReached = True
                vgr: VacuumGripper = self.controller.machines[0]
                self.assertFalse(vgr.vacuumActValve, "valve value changed from GO_TO_POSITION!")
                self.assertFalse(vgr.vacuumActCompressorOn, "compressor value changed from GO_TO_POSITION!")

            self.assertLess(iterationDone, 30, "GO_TO_POSITION not reached in less than 30 iterations" )


    def test_go_to_position_grip_on_Command(self):
        """Ensure that the go to position command is executed and does not change the state of the valve and pump.
           Case 2: Pump and valve are on
        """
        logging.debug(f'{inspect.stack()[0][3]} start')

        self.fakeSetupDoneAndSetPos()

        # initial feedback
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"VacuumGripper01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")

        # controller is idle
        self.controller.mainLoopIteration()
        self.assertEqual(ctHelper.readMachineFeedbackNotification(self.controller), "")

        # mock: valve and pump are active
        vgr: VacuumGripper = self.controller.machines[0]
        vgr.vacuumActValve = True
        vgr.vacuumActCompressorOn = True

        # send a grip command
        message = MachineCommand("COMMAND", "VACUUM", 1, "GO_TO_POSITION", [
            Position("END", 500, 0, 1200)
        ])
        ctHelper.sendMessage(self.controller, "VacuumGripper01", message)


        endCommandReached = False
        iterationDone = 0
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readCommandFeedbackNotification(self.controller)
            if notification == "":
                iterationDone += 1
            elif re.match(r"VacuumGripper01 \d+\.\d+ COMMAND_FEEDBACK 1 MUST_CONTINUE .*", notification):
                pass
            else:
                self.assertRegex(notification, r"VacuumGripper01 \d+\.\d+ COMMAND_FEEDBACK 1 DONE")
                notification = ctHelper.readMachineFeedbackNotification(self.controller)
                logging.debug(f"GO_TO_POSITION reached in {iterationDone} iterations")
                endCommandReached = True
                self.assertTrue(vgr.vacuumActValve, "valve value changed from GO_TO_POSITION!")
                self.assertTrue(vgr.vacuumActCompressorOn, "compressor value changed from GO_TO_POSITION!")

            self.assertLess(iterationDone, 30, "GO_TO_POSITION not reached in less than 30 iterations" )


    # TODO move to a test helper module
    def checkVGRPosition(self, expectedVerticalEncoder : int , expectedRotEncoder : int, expectedArmEncoder :int) -> None:
        """verifies that the Vacuum Gripper encoder values are close enought to the expected values taking into account the simulation increment"""
        vgr = self.controller.machines[0]
        assert isinstance(vgr,VacuumGripper)
        delta = self.controller.vaccumGripperSimulator.encoderIncrement - round(self.controller.vaccumGripperSimulator.encoderIncrement/3)
        self.assertAlmostEqual(vgr.vacuumSensVerticalEncoderCounter, expectedVerticalEncoder, delta=delta)
        self.assertAlmostEqual(vgr.vacuumSensRotEncoderCounter, expectedRotEncoder, delta=delta)
        self.assertAlmostEqual(vgr.vacuumSensArmEncoderCounter, expectedArmEncoder, delta=delta)


    def fakeSetupDoneAndSetPos(self, vacuumSensVerticalEncoderCounter: int = 0 , vacuumSensRotEncoderCounter : int = 0, vacuumSensArmEncoderCounter :int = 0,
                            vacuumSensArmEndIn : bool = True, vacuumSensRotEnd : bool = True , vacuumSensVerticalEndUp : bool = True) -> None:
        vgr = self.controller.machines[0]
        assert isinstance(vgr,VacuumGripper)
        vgr.isInitialized = True
        vgr.vacuumSensVerticalEncoderCounter = vacuumSensVerticalEncoderCounter
        vgr.vacuumSensRotEncoderCounter = vacuumSensRotEncoderCounter
        vgr.vacuumSensArmEncoderCounter =  vacuumSensArmEncoderCounter
        vgr.vacuumSensArmEndIn = vacuumSensArmEndIn
        vgr.vacuumSensRotEnd= vacuumSensRotEnd
        vgr.vacuumSensVerticalEndUp = vacuumSensVerticalEndUp


if __name__ == '__main__':
    logging.basicConfig(format='[%(levelname)-5s] %(module)-25s,%(lineno)-3s| %(message)s', level=logging.DEBUG)

    unittest.main()
