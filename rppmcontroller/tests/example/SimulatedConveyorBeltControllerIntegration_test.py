import inspect
import logging
import unittest
from unittest.mock import patch, Mock

from rppmcontroller.example.SimulatedConveyorBeltController import SimulatedConveyorBeltController
from rppmcontroller.machine.conveyorbelt.ConveyorBelt import ConveyorBelt
from rppmcontroller.machine.RequestedParameter import RequestedParameter
from rppmcontroller.machine.Direction import Direction
from rppmcontroller.protocol.MachineCommand import MachineCommand

import tests.controllerTestHelper as ctHelper


class SimulatedVacuumGripperControllerIntegrationTestCase(unittest.TestCase):

    def setUp(self):
        logging.debug("setup called")
        self.controller = SimulatedConveyorBeltController()
        self.controller.mainLoopDelay = 0.1

    ### ______ MOVE ________    
    def test_moveForwardCommand(self):
        '''
            Test the MOVE forward command
        '''
        logging.debug(f'{inspect.stack()[0][3]} start')

        # initial feedback
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readNotification(self.controller), r"ConveyorBelt01 \d+\.\d+ FEEDBACK 0 FINISHED")

        # controller is idle
        self.controller.mainLoopIteration()
        self.assertEqual(ctHelper.readNotification(self.controller), "")

        # send a move command
        message = MachineCommand("COMMAND", "CONVEYOR", 1, "MOVE", [
            Direction.FORWARD
          ] )
        
        ctHelper.sendMessage(self.controller, "ConveyorBelt01", message)
        
        self.controller.mainLoopIteration()
        self.controller.mainLoopIteration()

        self.assertRegex(ctHelper.readNotification(self.controller), r"ConveyorBelt01 \d+\.\d+ FEEDBACK 1 INACTION")

        endCommandReached = False
        iterationDone = 0
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readNotification(self.controller)
            '''Simulate sensor changes for testing all the functionnalities of the command'''
            if iterationDone == 2:
                self.controller.conveyorBeltSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERSWAPSTATION,False)
            if iterationDone == 4:
                self.controller.conveyorBeltSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERSWAPSTATION,True)
            if iterationDone%2 == 0:
                self.controller.conveyorBeltSimulator.fakeSensor(RequestedParameter.PULSECOUNTER,True)
            else:
                self.controller.conveyorBeltSimulator.fakeSensor(RequestedParameter.PULSECOUNTER,False)

            if (notification == "") :
                iterationDone += 1
            else:
                self.assertRegex(notification, r"ConveyorBelt01 \d+\.\d+ FEEDBACK 1 FINISHED")
                logging.debug(f"COMMAND FINISHED reached in {iterationDone} iterations")
                endCommandReached = True
            self.assertLess(iterationDone, 30, "COMMAND not reached in less than 30 iterations" )

    def test_moveBackardCommand(self):
        '''
            Test the MOVE backward command
        '''
        logging.debug(f'{inspect.stack()[0][3]} start')

        # initial feedback
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readNotification(self.controller), r"ConveyorBelt01 \d+\.\d+ FEEDBACK 0 FINISHED")

        # controller is idle
        self.controller.mainLoopIteration()
        self.assertEqual(ctHelper.readNotification(self.controller), "")

        # send a move command
        message = MachineCommand("COMMAND", "CONVEYOR", 1, "MOVE", [
            Direction.BACKWARD
          ] )
        
        ctHelper.sendMessage(self.controller, "ConveyorBelt01", message)
        
        self.controller.mainLoopIteration()
        self.controller.mainLoopIteration()

        self.assertRegex(ctHelper.readNotification(self.controller), r"ConveyorBelt01 \d+\.\d+ FEEDBACK 1 INACTION")

        endCommandReached = False
        iterationDone = 0
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readNotification(self.controller)
            '''Simulate sensor changes for testing all the functionnalities of the command'''
            if iterationDone == 2:
                self.controller.conveyorBeltSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERFEEDSTATION,False)
            if iterationDone == 4:
                self.controller.conveyorBeltSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERFEEDSTATION,True)
            if iterationDone%2 == 0:
                self.controller.conveyorBeltSimulator.fakeSensor(RequestedParameter.PULSECOUNTER,True)
            else:
                self.controller.conveyorBeltSimulator.fakeSensor(RequestedParameter.PULSECOUNTER,False)

            if (notification == "") :
                iterationDone += 1
            else:
                self.assertRegex(notification, r"ConveyorBelt01 \d+\.\d+ FEEDBACK 1 FINISHED")
                logging.debug(f"COMMAND FINISHED reached in {iterationDone} iterations")
                endCommandReached = True
            self.assertLess(iterationDone, 30, "COMMAND not reached in less than 30 iterations" )

    ### ______ MOVE LIGHT BASED ________    
    def test_moveLbForwardCommand(self):
        '''
            Test the MOVE light based command
        '''
        logging.debug(f'{inspect.stack()[0][3]} start')

        # initial feedback
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readNotification(self.controller), r"ConveyorBelt01 \d+\.\d+ FEEDBACK 0 FINISHED")

        # controller is idle
        self.controller.mainLoopIteration()
        self.assertEqual(ctHelper.readNotification(self.controller), "")

        # send a move light based command
        message = MachineCommand("COMMAND", "CONVEYOR", 1, "MOVELB", [
            Direction.FORWARD
          ] )
        
        ctHelper.sendMessage(self.controller, "ConveyorBelt01", message)
        
        self.controller.mainLoopIteration()
        self.controller.mainLoopIteration()

        self.assertRegex(ctHelper.readNotification(self.controller), r"ConveyorBelt01 \d+\.\d+ FEEDBACK 1 INACTION")

        endCommandReached = False
        iterationDone = 0
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readNotification(self.controller)
            '''Simulate sensor changes for testing all the functionnalities of the command'''
            if iterationDone == 2:
                self.controller.conveyorBeltSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERSWAPSTATION,False)
            if iterationDone == 4:
                self.controller.conveyorBeltSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERSWAPSTATION,True)

            if (notification == "") :
                iterationDone += 1
            else:
                self.assertRegex(notification, r"ConveyorBelt01 \d+\.\d+ FEEDBACK 1 FINISHED")
                logging.debug(f"COMMAND FINISHED reached in {iterationDone} iterations")
                endCommandReached = True
            self.assertLess(iterationDone, 30, "COMMAND not reached in less than 30 iterations" )

    def test_moveLbBackardCommand(self):
        '''
            Test the MOVE light based
        '''
        logging.debug(f'{inspect.stack()[0][3]} start')

        # initial feedback
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readNotification(self.controller), r"ConveyorBelt01 \d+\.\d+ FEEDBACK 0 FINISHED")

        # controller is idle
        self.controller.mainLoopIteration()
        self.assertEqual(ctHelper.readNotification(self.controller), "")

        # send a move light based command
        message = MachineCommand("COMMAND", "CONVEYOR", 1, "MOVELB", [
            Direction.BACKWARD
          ] )
        
        ctHelper.sendMessage(self.controller, "ConveyorBelt01", message)
        
        self.controller.mainLoopIteration()
        self.controller.mainLoopIteration()

        self.assertRegex(ctHelper.readNotification(self.controller), r"ConveyorBelt01 \d+\.\d+ FEEDBACK 1 INACTION")

        endCommandReached = False
        iterationDone = 0
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readNotification(self.controller)
            '''Simulate sensor changes for testing all the functionnalities of the command'''
            if iterationDone == 2:
                self.controller.conveyorBeltSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERFEEDSTATION,False)
            if iterationDone == 4:
                self.controller.conveyorBeltSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERFEEDSTATION,True)

            if (notification == "") :
                iterationDone += 1
            else:
                self.assertRegex(notification, r"ConveyorBelt01 \d+\.\d+ FEEDBACK 1 FINISHED")
                logging.debug(f"COMMAND FINISHED reached in {iterationDone} iterations")
                endCommandReached = True
            self.assertLess(iterationDone, 30, "COMMAND not reached in less than 30 iterations" )

    ### ______ GOTOCONFIG ________    
    def test_GoToConfigForwardCommand(self):
        '''
            Test the GoToConfig forward command
        '''
        logging.debug(f'{inspect.stack()[0][3]} start')

        # initial feedback
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readNotification(self.controller), r"ConveyorBelt01 \d+\.\d+ FEEDBACK 0 FINISHED")

        # controller is idle
        self.controller.mainLoopIteration()
        self.assertEqual(ctHelper.readNotification(self.controller), "")

        # send a go to config command
        message = MachineCommand("COMMAND", "CONVEYOR", 1, "GOTOCONFIG", [
            Direction.FORWARD,
            3
          ] )
        
        ctHelper.sendMessage(self.controller, "ConveyorBelt01", message)
        
        self.controller.mainLoopIteration()
        self.controller.mainLoopIteration()

        self.assertRegex(ctHelper.readNotification(self.controller), r"ConveyorBelt01 \d+\.\d+ FEEDBACK 1 INACTION")

        endCommandReached = False
        iterationDone = 0
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readNotification(self.controller)
            '''Simulate sensor changes for testing all the functionnalities of the command'''
            if iterationDone == 2:
                self.controller.conveyorBeltSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERSWAPSTATION,False)
            if iterationDone == 4:
                self.controller.conveyorBeltSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERSWAPSTATION,True)
            if iterationDone%2 == 0:
                self.controller.conveyorBeltSimulator.fakeSensor(RequestedParameter.PULSECOUNTER,True)
            else:
                self.controller.conveyorBeltSimulator.fakeSensor(RequestedParameter.PULSECOUNTER,False)

            if (notification == "") :
                iterationDone += 1
            else:
                self.assertRegex(notification, r"ConveyorBelt01 \d+\.\d+ FEEDBACK 1 FINISHED")
                logging.debug(f"COMMAND FINISHED reached in {iterationDone} iterations")
                endCommandReached = True
            self.assertLess(iterationDone, 30, "COMMAND not reached in less than 30 iterations" )

    def test_GoToConfigBackardCommand(self):
        '''
            Test the GOTOCONFIG backward command
        '''
        logging.debug(f'{inspect.stack()[0][3]} start')

        # initial feedback
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readNotification(self.controller), r"ConveyorBelt01 \d+\.\d+ FEEDBACK 0 FINISHED")

        # controller is idle
        self.controller.mainLoopIteration()
        self.assertEqual(ctHelper.readNotification(self.controller), "")

        # send a go to config command
        message = MachineCommand("COMMAND", "CONVEYOR", 1, "GOTOCONFIG", [
            Direction.BACKWARD,
            3
          ] )
        
        ctHelper.sendMessage(self.controller, "ConveyorBelt01", message)
        
        self.controller.mainLoopIteration()
        self.controller.mainLoopIteration()

        self.assertRegex(ctHelper.readNotification(self.controller), r"ConveyorBelt01 \d+\.\d+ FEEDBACK 1 INACTION")

        endCommandReached = False
        iterationDone = 0
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readNotification(self.controller)
            '''Simulate sensor changes for testing all the functionnalities of the command'''
            if iterationDone == 2:
                self.controller.conveyorBeltSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERFEEDSTATION,False)
            if iterationDone == 4:
                self.controller.conveyorBeltSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERFEEDSTATION,True)
            if iterationDone%2 == 0:
                self.controller.conveyorBeltSimulator.fakeSensor(RequestedParameter.PULSECOUNTER,True)
            else:
                self.controller.conveyorBeltSimulator.fakeSensor(RequestedParameter.PULSECOUNTER,False)

            if (notification == "") :
                iterationDone += 1
            else:
                self.assertRegex(notification, r"ConveyorBelt01 \d+\.\d+ FEEDBACK 1 FINISHED")
                logging.debug(f"COMMAND FINISHED reached in {iterationDone} iterations")
                endCommandReached = True
            self.assertLess(iterationDone, 30, "COMMAND not reached in less than 30 iterations" )



 
if __name__ == '__main__':
    logging.basicConfig(format='[%(levelname)-5s] %(module)-25s,%(lineno)-3s| %(message)s', level=logging.DEBUG)

    unittest.main()
