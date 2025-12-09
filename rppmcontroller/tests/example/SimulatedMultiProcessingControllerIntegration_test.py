import inspect
import logging
import os
import re
import unittest

import tests.controllerTestHelper as ctHelper
from rppmcontroller.example.SimulatedMultiProcessingController import \
    SimulatedMultiProcessingController
from rppmcontroller.machine.MPSOutput import MPSOutput
from rppmcontroller.machine.RequestedParameter import RequestedParameter
from rppmcontroller.machine.enum.MPSArmPosition import MPSArmPosition
from rppmcontroller.machine.multiprocessing.MultiProcessing import \
    MultiProcessing
from rppmcontroller.machine.multiprocessing.TurnTablePosition import \
    TurnTablePosition
from rppmcontroller.protocol.MachineCommand import MachineCommand


class SimulatedMultiProcessingControllerIntegrationTestCase(unittest.TestCase):

    def setUp(self):
        script_path = os.path.abspath(__file__)
        logging.info(f'script path : {script_path}')
        dir_path = os.path.dirname(__file__)
        config_path = os.path.join(dir_path, "config.yml")
        logging.info(f'config file path : {config_path}')

        logging.debug("setup called")
        self.controller = SimulatedMultiProcessingController(config_path)
        self.controller.mainLoopDelay = 0.4


    ### ________ PROCESS 1 ___________
    def test_process1(self):
        '''
            Test the process1 command
        '''
        logging.debug(f'{inspect.stack()[0][3]} start')

        # initial feedback
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readNotification(self.controller), r"MultiProcessing01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")

        # controller is idle
        self.controller.mainLoopIteration()
        self.assertEqual(ctHelper.readNotification(self.controller), "")

        # send a move command
        message = MachineCommand("COMMAND", "MULTIPROCESSING", 1, "PROCESS1", [])

        ctHelper.sendMessage(self.controller, "MultiProcessing01", message)

        self.controller.mainLoopIteration()
        self.controller.mainLoopIteration()

        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"MultiProcessing01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_ACTIVE")

        endCommandReached = False
        iterationDone = 0
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readCommandFeedbackNotification(self.controller)
            '''Simulate sensor changes for testing all the functionnalities of the command'''
            if iterationDone == 0:
                #initial state
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.LIGHTBARRIEROVEN,False)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHOVENFEEDEROUTSIDE,True)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHVACUUMPOSITIONTURNTABLE,True)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHTURNTABLEPOSITIONVACUUM,True)
            elif iterationDone == 4:
                #simulate package in oven
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHOVENFEEDEROUTSIDE,False)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHOVENFEEDERINSIDE,True)
            elif iterationDone == 10:
                #simulate heating completed and package outside oven
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHOVENFEEDEROUTSIDE,True)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHOVENFEEDERINSIDE,False)
            elif iterationDone == 12:
                #simulate vacuum at oven
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHVACUUMPOSITIONOVEN, True)
            elif iterationDone == 22:
                #simulate object gripped and moved to turntable
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHVACUUMPOSITIONOVEN, False)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHVACUUMPOSITIONTURNTABLE, True)
            elif iterationDone == 32:
                #simulate object released and moved to saw
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHTURNTABLEPOSITIONVACUUM, False)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHTURNTABLEPOSITIONSAW, True)
            elif iterationDone == 42:
                #simulate tuntable moved to conveyor belt
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHTURNTABLEPOSITIONSAW, False)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHTURNTABLEPOSITIONBELT, True)
            elif iterationDone == 52:
                #simulate package ejected to conveyor belt and came at the end of the belt
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERENDOFCONVEYORBELT, False)

            if notification == "" :
                iterationDone += 1
                logging.debug("Next iteration is " + str(iterationDone))
            elif re.match(r"MultiProcessing01 \d+\.\d+ COMMAND_FEEDBACK 1 MUST_CONTINUE .*", notification):
                pass
            else:
                self.assertRegex(notification, r"MultiProcessing01 \d+\.\d+ COMMAND_FEEDBACK 1 DONE")
                notification = ctHelper.readMachineFeedbackNotification(self.controller)
                self.assertRegex(notification, r"MultiProcessing01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")
                logging.debug(f"COMMAND DONE reached in {iterationDone} iterations")
                endCommandReached = True
            self.assertLess(iterationDone, 100, "COMMAND not reached in less than 100 iterations" )


    ### ________ PROCESS ___________
    def test_processNoSawNoOvenOutOven(self):
        '''
            Test the process command with no oven nor saw time and output at oven
        '''
        logging.debug(f'{inspect.stack()[0][3]} start')

        # initial feedback
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readNotification(self.controller), r"MultiProcessing01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")

        # controller is idle
        self.controller.mainLoopIteration()
        self.assertEqual(ctHelper.readNotification(self.controller), "")

        # send a move command
        message = MachineCommand("COMMAND", "MULTIPROCESSING", 1, "PROCESS", [
            0, 0, MPSOutput.OVEN
        ])

        ctHelper.sendMessage(self.controller, "MultiProcessing01", message)

        self.controller.mainLoopIteration()
        self.controller.mainLoopIteration()

        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"MultiProcessing01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_ACTIVE")

        endCommandReached = False
        iterationDone = 0
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readCommandFeedbackNotification(self.controller)

            if iterationDone == 2:
                #Simulate the setup
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHOVENFEEDEROUTSIDE,True)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHVACUUMPOSITIONTURNTABLE,True)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHTURNTABLEPOSITIONVACUUM,True)

            if notification == "" :
                iterationDone += 1
                logging.debug("Next iteration is " + str(iterationDone))
            elif re.match(r"MultiProcessing01 \d+\.\d+ COMMAND_FEEDBACK 1 MUST_CONTINUE .*", notification):
                pass
            else:
                self.assertRegex(notification, r"MultiProcessing01 \d+\.\d+ COMMAND_FEEDBACK 1 DONE")
                notification = ctHelper.readMachineFeedbackNotification(self.controller)
                self.assertRegex(notification, r"MultiProcessing01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")
                logging.debug(f"COMMAND DONE reached in {iterationDone} iterations")
                endCommandReached = True
            self.assertLess(iterationDone, 10, "COMMAND not reached in less than 10 iterations" )


    def test_processNoSawNoOvenOutConv(self):
        '''
            Test the process command with no oven nor saw time and output at conveyor
        '''
        logging.debug(f'{inspect.stack()[0][3]} start')

        # initial feedback
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readNotification(self.controller), r"MultiProcessing01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")

        # controller is idle
        self.controller.mainLoopIteration()
        self.assertEqual(ctHelper.readNotification(self.controller), "")

        # send a move command
        message = MachineCommand("COMMAND", "MULTIPROCESSING", 1, "PROCESS", [
            0, 0, MPSOutput.CONVEYOR
        ])

        ctHelper.sendMessage(self.controller, "MultiProcessing01", message)

        self.controller.mainLoopIteration()
        self.controller.mainLoopIteration()

        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"MultiProcessing01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_ACTIVE")

        endCommandReached = False
        iterationDone = 0
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readCommandFeedbackNotification(self.controller)

            if iterationDone == 2:
                #Simulate the setup
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHOVENFEEDEROUTSIDE,True)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHVACUUMPOSITIONTURNTABLE,True)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHTURNTABLEPOSITIONVACUUM,True)
            elif iterationDone == 4:
                #Simulate the arm to oven
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHVACUUMPOSITIONTURNTABLE,False)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHVACUUMPOSITIONOVEN,True)
            elif iterationDone == 12:
                #Simulate the arm to turntable
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHVACUUMPOSITIONTURNTABLE,True)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHVACUUMPOSITIONOVEN,False)
            elif iterationDone == 20:
                #Simulate the turntable to conveyor
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHTURNTABLEPOSITIONVACUUM,False)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHTURNTABLEPOSITIONBELT,True)
            elif iterationDone == 22:
                #Simulate the payload to the sensor
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERENDOFCONVEYORBELT,False)

            if notification == "" :
                iterationDone += 1
                logging.debug("Next iteration is " + str(iterationDone))
            elif re.match(r"MultiProcessing01 \d+\.\d+ COMMAND_FEEDBACK 1 MUST_CONTINUE .*", notification):
                pass
            else:
                self.assertRegex(notification, r"MultiProcessing01 \d+\.\d+ COMMAND_FEEDBACK 1 DONE")
                notification = ctHelper.readMachineFeedbackNotification(self.controller)
                self.assertRegex(notification, r"MultiProcessing01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")
                logging.debug(f"COMMAND DONE reached in {iterationDone} iterations")
                endCommandReached = True
            self.assertLess(iterationDone, 30, "COMMAND not reached in less than 30 iterations" )


    def test_processSawNoOvenOutOven(self):
        '''
            Test the process command with saw time and output at oven
        '''
        logging.debug(f'{inspect.stack()[0][3]} start')

        # initial feedback
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readNotification(self.controller), r"MultiProcessing01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")

        # controller is idle
        self.controller.mainLoopIteration()
        self.assertEqual(ctHelper.readNotification(self.controller), "")

        # send a move command
        message = MachineCommand("COMMAND", "MULTIPROCESSING", 1, "PROCESS", [
            0, 0.5, MPSOutput.OVEN
        ])

        ctHelper.sendMessage(self.controller, "MultiProcessing01", message)

        self.controller.mainLoopIteration()
        self.controller.mainLoopIteration()

        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"MultiProcessing01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_ACTIVE")

        endCommandReached = False
        iterationDone = 0
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readCommandFeedbackNotification(self.controller)

            if iterationDone == 2:
                #Simulate the setup
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHOVENFEEDEROUTSIDE,True)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHVACUUMPOSITIONTURNTABLE,True)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHTURNTABLEPOSITIONVACUUM,True)
            elif iterationDone == 4:
                #Simulate the arm to oven
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHVACUUMPOSITIONTURNTABLE,False)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHVACUUMPOSITIONOVEN,True)
            elif iterationDone == 12:
                #Simulate the arm to turntable
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHVACUUMPOSITIONTURNTABLE,True)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHVACUUMPOSITIONOVEN,False)
            elif iterationDone == 20:
                #Simulate the turntable to saw
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHTURNTABLEPOSITIONVACUUM,False)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHTURNTABLEPOSITIONSAW,True)
            elif iterationDone == 24:
                #Simulate the turntable to vacuum
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHTURNTABLEPOSITIONVACUUM,True)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHTURNTABLEPOSITIONSAW,False)
            elif iterationDone == 32:
                #Simulate the arm to oven
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHVACUUMPOSITIONTURNTABLE,False)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHVACUUMPOSITIONOVEN,True)
            elif iterationDone == 40:
                #Simulate the arm to turntable
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHVACUUMPOSITIONTURNTABLE,True)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHVACUUMPOSITIONOVEN,False)

            if notification == "" :
                iterationDone += 1
                logging.debug("Next iteration is " + str(iterationDone))
            elif re.match(r"MultiProcessing01 \d+\.\d+ COMMAND_FEEDBACK 1 MUST_CONTINUE .*", notification):
                pass
            else:
                self.assertRegex(notification, r"MultiProcessing01 \d+\.\d+ COMMAND_FEEDBACK 1 DONE")
                notification = ctHelper.readMachineFeedbackNotification(self.controller)
                self.assertRegex(notification, r"MultiProcessing01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")
                logging.debug(f"COMMAND DONE reached in {iterationDone} iterations")
                endCommandReached = True
            self.assertLess(iterationDone, 50, "COMMAND not reached in less than 50 iterations" )


    def test_processNoSawOvenOutConv(self):
        '''
            Test the process command with oven time and output at conveyor
        '''
        logging.debug(f'{inspect.stack()[0][3]} start')

        # initial feedback
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readNotification(self.controller), r"MultiProcessing01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")

        # controller is idle
        self.controller.mainLoopIteration()
        self.assertEqual(ctHelper.readNotification(self.controller), "")

        # send a move command
        message = MachineCommand("COMMAND", "MULTIPROCESSING", 1, "PROCESS", [
            0.5, 0, MPSOutput.CONVEYOR
        ])

        ctHelper.sendMessage(self.controller, "MultiProcessing01", message)

        self.controller.mainLoopIteration()
        self.controller.mainLoopIteration()

        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"MultiProcessing01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_ACTIVE")

        endCommandReached = False
        iterationDone = 0
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readCommandFeedbackNotification(self.controller)

            if iterationDone == 2:
                #Simulate the setup
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHOVENFEEDEROUTSIDE,True)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHVACUUMPOSITIONTURNTABLE,True)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHTURNTABLEPOSITIONVACUUM,True)
            elif iterationDone == 4:
                #Simulate payload into oven
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHOVENFEEDERINSIDE, True)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHOVENFEEDEROUTSIDE, False)
            elif iterationDone == 8:
                #Simulate payload out of oven
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHOVENFEEDERINSIDE, False)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHOVENFEEDEROUTSIDE, True)
            elif iterationDone == 10:
                #Simulate the arm to oven
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHVACUUMPOSITIONTURNTABLE,False)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHVACUUMPOSITIONOVEN,True)
            elif iterationDone == 18:
                #Simulate the arm to turntable
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHVACUUMPOSITIONTURNTABLE,True)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHVACUUMPOSITIONOVEN,False)
            elif iterationDone == 26:
                #Simulate the turntable to conveyor
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHTURNTABLEPOSITIONVACUUM,False)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHTURNTABLEPOSITIONBELT,True)
            elif iterationDone == 28:
                #Simulate the payload to the sensor
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERENDOFCONVEYORBELT,False)

            if notification == "" :
                iterationDone += 1
                logging.debug("Next iteration is " + str(iterationDone))
            elif re.match(r"MultiProcessing01 \d+\.\d+ COMMAND_FEEDBACK 1 MUST_CONTINUE .*", notification):
                pass
            else:
                self.assertRegex(notification, r"MultiProcessing01 \d+\.\d+ COMMAND_FEEDBACK 1 DONE")
                notification = ctHelper.readMachineFeedbackNotification(self.controller)
                self.assertRegex(notification, r"MultiProcessing01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")
                logging.debug(f"COMMAND DONE reached in {iterationDone} iterations")
                endCommandReached = True
            self.assertLess(iterationDone, 40, "COMMAND not reached in less than 40 iterations" )


    def test_heatInOven(self):
        '''
            Test the oven_process command
        '''
        logging.debug(f'{inspect.stack()[0][3]} start')

        # initial feedback
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readNotification(self.controller), r"MultiProcessing01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")

        # controller is idle
        self.controller.mainLoopIteration()
        self.assertEqual(ctHelper.readNotification(self.controller), "")

        # send a move command
        message = MachineCommand("COMMAND", "MULTIPROCESSING", 1, "OVEN_PROCESS", [0.5])
        ctHelper.sendMessage(self.controller, "MultiProcessing01", message)

        self.controller.mainLoopIteration()
        self.controller.mainLoopIteration()

        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"MultiProcessing01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_ACTIVE")

        endCommandReached = False
        iterationDone = 0
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readCommandFeedbackNotification(self.controller)

            if iterationDone == 2:
                #Simulate the setup
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHOVENFEEDEROUTSIDE,True)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHVACUUMPOSITIONTURNTABLE,True)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHTURNTABLEPOSITIONVACUUM,True)
            elif iterationDone == 4:
                #Simulate payload into oven
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHOVENFEEDERINSIDE, True)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHOVENFEEDEROUTSIDE, False)
            elif iterationDone == 8:
                #Simulate payload out of oven
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHOVENFEEDERINSIDE, False)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHOVENFEEDEROUTSIDE, True)

            if notification == "" :
                iterationDone += 1
                logging.debug("Next iteration is " + str(iterationDone))
            elif re.match(r"MultiProcessing01 \d+\.\d+ COMMAND_FEEDBACK 1 MUST_CONTINUE .*", notification):
                pass
            else:
                self.assertRegex(notification, r"MultiProcessing01 \d+\.\d+ COMMAND_FEEDBACK 1 DONE")
                notification = ctHelper.readMachineFeedbackNotification(self.controller)
                self.assertRegex(notification, r"MultiProcessing01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")
                logging.debug(f"COMMAND DONE reached in {iterationDone} iterations")
                endCommandReached = True
            self.assertLess(iterationDone, 10, "COMMAND not reached in less than 10 iterations" )


    def test_sawOnTurntable(self):
        '''
            Test the saw_cut command
        '''
        logging.debug(f'{inspect.stack()[0][3]} start')

        # initial feedback
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readNotification(self.controller), r"MultiProcessing01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")

        # controller is idle
        self.controller.mainLoopIteration()
        self.assertEqual(ctHelper.readNotification(self.controller), "")

        # send a move command
        message = MachineCommand("COMMAND", "MULTIPROCESSING", 1, "SAW_CUT", [0.5])
        ctHelper.sendMessage(self.controller, "MultiProcessing01", message)

        self.controller.mainLoopIteration()
        self.controller.mainLoopIteration()

        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"MultiProcessing01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_ACTIVE")

        endCommandReached = False
        iterationDone = 0
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readCommandFeedbackNotification(self.controller)

            if iterationDone == 2:
                #Simulate the setup
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHOVENFEEDEROUTSIDE,True)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHVACUUMPOSITIONTURNTABLE,True)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHTURNTABLEPOSITIONSAW,True)

            if notification == "" :
                iterationDone += 1
                logging.debug("Next iteration is " + str(iterationDone))
            elif re.match(r"MultiProcessing01 \d+\.\d+ COMMAND_FEEDBACK 1 MUST_CONTINUE .*", notification):
                pass
            else:
                self.assertRegex(notification, r"MultiProcessing01 \d+\.\d+ COMMAND_FEEDBACK 1 DONE")
                notification = ctHelper.readMachineFeedbackNotification(self.controller)
                self.assertRegex(notification, r"MultiProcessing01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")
                logging.debug(f"COMMAND DONE reached in {iterationDone} iterations")
                endCommandReached = True
            self.assertLess(iterationDone, 10, "COMMAND not reached in less than 10 iterations" )


    def test_armToOven(self):
        '''
            Test the arm_move command
        '''
        logging.debug(f'{inspect.stack()[0][3]} start')

        # initial feedback
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readNotification(self.controller), r"MultiProcessing01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")

        # controller is idle
        self.controller.mainLoopIteration()
        self.assertEqual(ctHelper.readNotification(self.controller), "")

        # send a move command
        message = MachineCommand("COMMAND", "MULTIPROCESSING", 1, "ARM_MOVE", [
            MPSArmPosition.OVEN
        ])
        ctHelper.sendMessage(self.controller, "MultiProcessing01", message)

        self.controller.mainLoopIteration()
        self.controller.mainLoopIteration()

        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"MultiProcessing01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_ACTIVE")

        endCommandReached = False
        iterationDone = 0
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readCommandFeedbackNotification(self.controller)

            if iterationDone == 2:
                #Simulate the setup
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHOVENFEEDEROUTSIDE,True)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHVACUUMPOSITIONTURNTABLE,True)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHTURNTABLEPOSITIONVACUUM,True)
            elif iterationDone == 4:
                #simulate arm to oven
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHVACUUMPOSITIONOVEN,True)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHVACUUMPOSITIONTURNTABLE,False)

            if notification == "" :
                iterationDone += 1
                logging.debug("Next iteration is " + str(iterationDone))
            elif re.match(r"MultiProcessing01 \d+\.\d+ COMMAND_FEEDBACK 1 MUST_CONTINUE .*", notification):
                pass
            else:
                self.assertRegex(notification, r"MultiProcessing01 \d+\.\d+ COMMAND_FEEDBACK 1 DONE")
                notification = ctHelper.readMachineFeedbackNotification(self.controller)
                self.assertRegex(notification, r"MultiProcessing01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")
                logging.debug(f"COMMAND DONE reached in {iterationDone} iterations")
                endCommandReached = True
            self.assertLess(iterationDone, 10, "COMMAND not reached in less than 10 iterations" )


    def test_armToTurntable(self):
        '''
            Test the arm_move command
        '''
        logging.debug(f'{inspect.stack()[0][3]} start')

        # initial feedback
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readNotification(self.controller), r"MultiProcessing01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")

        # controller is idle
        self.controller.mainLoopIteration()
        self.assertEqual(ctHelper.readNotification(self.controller), "")

        # send a move command
        message = MachineCommand("COMMAND", "MULTIPROCESSING", 1, "ARM_MOVE", [
            MPSArmPosition.TURNTABLE
        ])
        ctHelper.sendMessage(self.controller, "MultiProcessing01", message)

        self.controller.mainLoopIteration()
        self.controller.mainLoopIteration()

        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"MultiProcessing01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_ACTIVE")

        endCommandReached = False
        iterationDone = 0
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readCommandFeedbackNotification(self.controller)

            if iterationDone == 2:
                #Simulate the setup
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHOVENFEEDEROUTSIDE,True)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHVACUUMPOSITIONOVEN,True)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHTURNTABLEPOSITIONVACUUM,True)
            elif iterationDone == 4:
                #simulate arm to turntable
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHVACUUMPOSITIONOVEN,False)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHVACUUMPOSITIONTURNTABLE,True)

            if notification == "" :
                iterationDone += 1
                logging.debug("Next iteration is " + str(iterationDone))
            elif re.match(r"MultiProcessing01 \d+\.\d+ COMMAND_FEEDBACK 1 MUST_CONTINUE .*", notification):
                pass
            else:
                self.assertRegex(notification, r"MultiProcessing01 \d+\.\d+ COMMAND_FEEDBACK 1 DONE")
                notification = ctHelper.readMachineFeedbackNotification(self.controller)
                self.assertRegex(notification, r"MultiProcessing01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")
                logging.debug(f"COMMAND DONE reached in {iterationDone} iterations")
                endCommandReached = True
            self.assertLess(iterationDone, 10, "COMMAND not reached in less than 10 iterations" )


    def test_pickUp(self):
        '''
            Test the arm_pick command
        '''
        logging.debug(f'{inspect.stack()[0][3]} start')

        # initial feedback
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readNotification(self.controller), r"MultiProcessing01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")

        # controller is idle
        self.controller.mainLoopIteration()
        self.assertEqual(ctHelper.readNotification(self.controller), "")

        # send a move command
        message = MachineCommand("COMMAND", "MULTIPROCESSING", 1, "ARM_PICK", [])
        ctHelper.sendMessage(self.controller, "MultiProcessing01", message)

        self.controller.mainLoopIteration()
        self.controller.mainLoopIteration()

        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"MultiProcessing01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_ACTIVE")

        endCommandReached = False
        iterationDone = 0
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readCommandFeedbackNotification(self.controller)

            if iterationDone == 2:
                #Simulate the setup
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHOVENFEEDEROUTSIDE,True)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHVACUUMPOSITIONTURNTABLE,True)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHTURNTABLEPOSITIONVACUUM,True)

            if notification == "" :
                iterationDone += 1
                logging.debug("Next iteration is " + str(iterationDone))
            elif re.match(r"MultiProcessing01 \d+\.\d+ COMMAND_FEEDBACK 1 MUST_CONTINUE .*", notification):
                pass
            else:
                self.assertRegex(notification, r"MultiProcessing01 \d+\.\d+ COMMAND_FEEDBACK 1 DONE")
                notification = ctHelper.readMachineFeedbackNotification(self.controller)
                logging.debug(f"COMMAND DONE reached in {iterationDone} iterations")
                endCommandReached = True
            self.assertLess(iterationDone, 10, "COMMAND not reached in less than 10 iterations" )


    def test_place(self):
        '''
            Test the arm_place command
        '''
        logging.debug(f'{inspect.stack()[0][3]} start')

        # initial feedback
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readNotification(self.controller), r"MultiProcessing01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")

        # controller is idle
        self.controller.mainLoopIteration()
        self.assertEqual(ctHelper.readNotification(self.controller), "")

        # send a move command
        message = MachineCommand("COMMAND", "MULTIPROCESSING", 1, "ARM_PLACE", [])
        ctHelper.sendMessage(self.controller, "MultiProcessing01", message)

        self.controller.mainLoopIteration()
        self.controller.mainLoopIteration()

        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"MultiProcessing01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_ACTIVE")

        endCommandReached = False
        iterationDone = 0
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readCommandFeedbackNotification(self.controller)

            if iterationDone == 2:
                #Simulate the setup
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHOVENFEEDEROUTSIDE,True)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHVACUUMPOSITIONTURNTABLE,True)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHTURNTABLEPOSITIONVACUUM,True)

            if notification == "" :
                iterationDone += 1
                logging.debug("Next iteration is " + str(iterationDone))
            elif re.match(r"MultiProcessing01 \d+\.\d+ COMMAND_FEEDBACK 1 MUST_CONTINUE .*", notification):
                pass
            else:
                self.assertRegex(notification, r"MultiProcessing01 \d+\.\d+ COMMAND_FEEDBACK 1 DONE")
                notification = ctHelper.readMachineFeedbackNotification(self.controller)
                self.assertRegex(notification, r"MultiProcessing01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")
                logging.debug(f"COMMAND DONE reached in {iterationDone} iterations")
                endCommandReached = True
            self.assertLess(iterationDone, 10, "COMMAND not reached in less than 10 iterations" )


    def test_turntableToArm(self):
        '''
            Test the turntable_rotate command
        '''
        logging.debug(f'{inspect.stack()[0][3]} start')

        # initial feedback
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readNotification(self.controller), r"MultiProcessing01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")

        # controller is idle
        self.controller.mainLoopIteration()
        self.assertEqual(ctHelper.readNotification(self.controller), "")

        # send a move command
        message = MachineCommand("COMMAND", "MULTIPROCESSING", 1, "TURNTABLE_ROTATE", [
            TurnTablePosition.VACUUM
        ])
        ctHelper.sendMessage(self.controller, "MultiProcessing01", message)

        self.controller.mainLoopIteration()
        self.controller.mainLoopIteration()

        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"MultiProcessing01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_ACTIVE")

        endCommandReached = False
        iterationDone = 0
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readCommandFeedbackNotification(self.controller)

            if iterationDone == 2:
                #Simulate the setup
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHOVENFEEDEROUTSIDE,True)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHVACUUMPOSITIONTURNTABLE,True)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHTURNTABLEPOSITIONBELT,True)
            elif iterationDone == 6:
                #simulate turntable to arm
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHTURNTABLEPOSITIONVACUUM,True)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHTURNTABLEPOSITIONBELT,False)

            if notification == "" :
                iterationDone += 1
                logging.debug("Next iteration is " + str(iterationDone))
            elif re.match(r"MultiProcessing01 \d+\.\d+ COMMAND_FEEDBACK 1 MUST_CONTINUE .*", notification):
                pass
            else:
                self.assertRegex(notification, r"MultiProcessing01 \d+\.\d+ COMMAND_FEEDBACK 1 DONE")
                notification = ctHelper.readMachineFeedbackNotification(self.controller)
                self.assertRegex(notification, r"MultiProcessing01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")
                logging.debug(f"COMMAND DONE reached in {iterationDone} iterations")
                endCommandReached = True
            self.assertLess(iterationDone, 10, "COMMAND not reached in less than 10 iterations" )


    def test_turntableToSaw(self):
        '''
            Test the turntable_rotate command
        '''
        logging.debug(f'{inspect.stack()[0][3]} start')

        # initial feedback
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readNotification(self.controller), r"MultiProcessing01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")

        # controller is idle
        self.controller.mainLoopIteration()
        self.assertEqual(ctHelper.readNotification(self.controller), "")

        # send a move command
        message = MachineCommand("COMMAND", "MULTIPROCESSING", 1, "TURNTABLE_ROTATE", [
            TurnTablePosition.SAW
        ])
        ctHelper.sendMessage(self.controller, "MultiProcessing01", message)

        self.controller.mainLoopIteration()
        self.controller.mainLoopIteration()

        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"MultiProcessing01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_ACTIVE")

        endCommandReached = False
        iterationDone = 0
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readCommandFeedbackNotification(self.controller)

            if iterationDone == 2:
                #Simulate the setup
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHOVENFEEDEROUTSIDE,True)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHVACUUMPOSITIONTURNTABLE,True)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHTURNTABLEPOSITIONVACUUM,True)
            elif iterationDone == 6:
                #simulate turntable to arm
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHTURNTABLEPOSITIONSAW,True)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHTURNTABLEPOSITIONVACUUM,False)

            if notification == "" :
                iterationDone += 1
                logging.debug("Next iteration is " + str(iterationDone))
            elif re.match(r"MultiProcessing01 \d+\.\d+ COMMAND_FEEDBACK 1 MUST_CONTINUE .*", notification):
                pass
            else:
                self.assertRegex(notification, r"MultiProcessing01 \d+\.\d+ COMMAND_FEEDBACK 1 DONE")
                notification = ctHelper.readMachineFeedbackNotification(self.controller)
                self.assertRegex(notification, r"MultiProcessing01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")
                logging.debug(f"COMMAND DONE reached in {iterationDone} iterations")
                endCommandReached = True
            self.assertLess(iterationDone, 10, "COMMAND not reached in less than 10 iterations" )


    def test_turntableToConveyor(self):
        '''
            Test the turntable_rotate command
        '''
        logging.debug(f'{inspect.stack()[0][3]} start')

        # initial feedback
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readNotification(self.controller), r"MultiProcessing01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")

        # controller is idle
        self.controller.mainLoopIteration()
        self.assertEqual(ctHelper.readNotification(self.controller), "")

        # send a move command
        message = MachineCommand("COMMAND", "MULTIPROCESSING", 1, "TURNTABLE_ROTATE", [
            TurnTablePosition.CONVEYOR
        ])
        ctHelper.sendMessage(self.controller, "MultiProcessing01", message)

        self.controller.mainLoopIteration()
        self.controller.mainLoopIteration()

        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"MultiProcessing01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_ACTIVE")

        endCommandReached = False
        iterationDone = 0
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readCommandFeedbackNotification(self.controller)

            if iterationDone == 2:
                #Simulate the setup
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHOVENFEEDEROUTSIDE,True)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHVACUUMPOSITIONTURNTABLE,True)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHTURNTABLEPOSITIONVACUUM,True)
            elif iterationDone == 6:
                #simulate turntable to arm
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHTURNTABLEPOSITIONBELT,True)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHTURNTABLEPOSITIONVACUUM,False)

            if notification == "" :
                iterationDone += 1
                logging.debug("Next iteration is " + str(iterationDone))
            elif re.match(r"MultiProcessing01 \d+\.\d+ COMMAND_FEEDBACK 1 MUST_CONTINUE .*", notification):
                pass
            else:
                self.assertRegex(notification, r"MultiProcessing01 \d+\.\d+ COMMAND_FEEDBACK 1 DONE")
                notification = ctHelper.readMachineFeedbackNotification(self.controller)
                self.assertRegex(notification, r"MultiProcessing01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")
                logging.debug(f"COMMAND DONE reached in {iterationDone} iterations")
                endCommandReached = True
            self.assertLess(iterationDone, 10, "COMMAND not reached in less than 10 iterations" )


    def test_ejectFromTurntable(self):
        '''
            Test the turntable_eject command
        '''
        logging.debug(f'{inspect.stack()[0][3]} start')

        # initial feedback
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readNotification(self.controller), r"MultiProcessing01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")

        # controller is idle
        self.controller.mainLoopIteration()
        self.assertEqual(ctHelper.readNotification(self.controller), "")

        # send a move command
        message = MachineCommand("COMMAND", "MULTIPROCESSING", 1, "TURNTABLE_EJECT", [])
        ctHelper.sendMessage(self.controller, "MultiProcessing01", message)

        self.controller.mainLoopIteration()
        self.controller.mainLoopIteration()

        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"MultiProcessing01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_ACTIVE")

        endCommandReached = False
        iterationDone = 0
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readCommandFeedbackNotification(self.controller)

            if iterationDone == 2:
                #Simulate the setup
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHOVENFEEDEROUTSIDE,True)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHVACUUMPOSITIONTURNTABLE,True)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHTURNTABLEPOSITIONVACUUM,True)
            elif iterationDone == 6:
                #simulate turntable to conveyor
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHTURNTABLEPOSITIONBELT,True)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHTURNTABLEPOSITIONVACUUM,False)
            elif iterationDone == 8:
                #simulate turntable to conveyor
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERENDOFCONVEYORBELT,False)

            if notification == "" :
                iterationDone += 1
                logging.debug("Next iteration is " + str(iterationDone))
            elif re.match(r"MultiProcessing01 \d+\.\d+ COMMAND_FEEDBACK 1 MUST_CONTINUE .*", notification):
                pass
            else:
                self.assertRegex(notification, r"MultiProcessing01 \d+\.\d+ COMMAND_FEEDBACK 1 DONE")
                notification = ctHelper.readMachineFeedbackNotification(self.controller)
                self.assertRegex(notification, r"MultiProcessing01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")
                logging.debug(f"COMMAND DONE reached in {iterationDone} iterations")
                endCommandReached = True
            self.assertLess(iterationDone, 20, "COMMAND not reached in less than 20 iterations" )


    def test_moveToSafetyOven(self):
        '''
            Test the move_to_safe_position command
        '''
        logging.debug(f'{inspect.stack()[0][3]} start')

        #setup the MPS with the arm on turntable
        mps = self.controller.machines[0]
        assert isinstance(mps, MultiProcessing)
        mps.safeToOven = True
        mps.multiProcessingSensOvenFeederOut = True
        mps.multiProcessingSensTurntablePosVacuum = True
        mps.multiProcessingSensVacuumGripperAtTurntable = True

        # initial feedback
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readNotification(self.controller), r"MultiProcessing01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")

        # controller is idle
        self.controller.mainLoopIteration()
        self.assertEqual(ctHelper.readNotification(self.controller), "")

        # send a move command
        message = MachineCommand("COMMAND", "MULTIPROCESSING", 1, "MOVE_TO_SAFE_POSITION", [])
        ctHelper.sendMessage(self.controller, "MultiProcessing01", message)

        self.controller.mainLoopIteration()
        self.controller.mainLoopIteration()

        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"MultiProcessing01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_ACTIVE")

        endCommandReached = False
        iterationDone = 0
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readCommandFeedbackNotification(self.controller)

            if iterationDone == 2:
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHVACUUMPOSITIONOVEN,True)

            if notification == "" :
                iterationDone += 1
                logging.debug("Next iteration is " + str(iterationDone))
            elif re.match(r"MultiProcessing01 \d+\.\d+ COMMAND_FEEDBACK 1 MUST_CONTINUE .*", notification):
                pass
            else:
                self.assertRegex(notification, r"MultiProcessing01 \d+\.\d+ COMMAND_FEEDBACK 1 DONE")
                notification = ctHelper.readMachineFeedbackNotification(self.controller)
                self.assertRegex(notification, r"MultiProcessing01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")
                logging.debug(f"COMMAND DONE reached in {iterationDone} iterations")
                endCommandReached = True
            self.assertLess(iterationDone, 10, "COMMAND not reached in less than 10 iterations" )


    def test_moveToSafetyTurntable(self):
        '''
            Test the move_to_safe_position command
        '''
        logging.debug(f'{inspect.stack()[0][3]} start')

        #setup the MPS with the arm on turntable
        mps = self.controller.machines[0]
        assert isinstance(mps, MultiProcessing)
        mps.safeToOven = False
        mps.multiProcessingSensOvenFeederOut = True
        mps.multiProcessingSensTurntablePosVacuum = True
        mps.multiProcessingSensVacuumGripperAtOven = True

        # initial feedback
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readNotification(self.controller), r"MultiProcessing01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")

        # controller is idle
        self.controller.mainLoopIteration()
        self.assertEqual(ctHelper.readNotification(self.controller), "")

        # send a move command
        message = MachineCommand("COMMAND", "MULTIPROCESSING", 1, "MOVE_TO_SAFE_POSITION", [])
        ctHelper.sendMessage(self.controller, "MultiProcessing01", message)

        self.controller.mainLoopIteration()
        self.controller.mainLoopIteration()

        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"MultiProcessing01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_ACTIVE")

        endCommandReached = False
        iterationDone = 0
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readCommandFeedbackNotification(self.controller)

            if iterationDone == 2:
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHVACUUMPOSITIONTURNTABLE,True)

            if notification == "" :
                iterationDone += 1
                logging.debug("Next iteration is " + str(iterationDone))
            elif re.match(r"MultiProcessing01 \d+\.\d+ COMMAND_FEEDBACK 1 MUST_CONTINUE .*", notification):
                pass
            else:
                self.assertRegex(notification, r"MultiProcessing01 \d+\.\d+ COMMAND_FEEDBACK 1 DONE")
                notification = ctHelper.readMachineFeedbackNotification(self.controller)
                self.assertRegex(notification, r"MultiProcessing01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")
                logging.debug(f"COMMAND DONE reached in {iterationDone} iterations")
                endCommandReached = True
            self.assertLess(iterationDone, 10, "COMMAND not reached in less than 10 iterations" )



if __name__ == '__main__':
    logging.basicConfig(format='[%(levelname)-5s] %(module)-25s,%(lineno)-3s| %(message)s', level=logging.DEBUG)

    unittest.main()
