import inspect
import logging
import unittest
from unittest.mock import patch, Mock

from rppmcontroller.example.SimulatedMultiProcessingController import SimulatedMultiProcessingController
from rppmcontroller.machine.multiprocessing.MultiProcessing import MultiProcessing
from rppmcontroller.machine.RequestedParameter import RequestedParameter
from rppmcontroller.machine.Direction import Direction
from rppmcontroller.protocol.MachineCommand import MachineCommand

import tests.controllerTestHelper as ctHelper


class SimulatedVacuumGripperControllerIntegrationTestCase(unittest.TestCase):

    def setUp(self):
        logging.debug("setup called")
        self.controller = SimulatedMultiProcessingController()
        self.controller.mainLoopDelay = 0.1

    
    ### ________ PROCESS 1 ___________
    def test_process1(self):
        '''
            Test the process1 command
        '''
        logging.debug(f'{inspect.stack()[0][3]} start')

        # initial feedback
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readNotification(self.controller), r"MultiProcessing01 \d+\.\d+ FEEDBACK 0 FINISHED")

        # controller is idle
        self.controller.mainLoopIteration()
        self.assertEqual(ctHelper.readNotification(self.controller), "")

        # send a move command
        message = MachineCommand("COMMAND", "MULTIPROCESSING", 1, "PROCESS1", [])
        
        ctHelper.sendMessage(self.controller, "MultiProcessing01", message)
        
        self.controller.mainLoopIteration()
        self.controller.mainLoopIteration()

        self.assertRegex(ctHelper.readNotification(self.controller), r"MultiProcessing01 \d+\.\d+ FEEDBACK 1 INACTION")

        endCommandReached = False
        iterationDone = 0
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readNotification(self.controller)
            '''Simulate sensor changes for testing all the functionnalities of the command'''
            if iterationDone == 0:
                #initial state
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHOVENFEEDEROUTSIDE,True)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHVACUUMPOSITIONTURNTABLE,True)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHTURNTABLEPOSITOINVACUUM,True)
            elif iterationDone == 2:
                #simulate package in oven
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHOVENFEEDEROUTSIDE,False)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHOVENFEEDERINSIDE,True)
            elif iterationDone == 36:
                #simulate eating completed and package outside oven
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHOVENFEEDEROUTSIDE,True)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHOVENFEEDERINSIDE,False)
            elif iterationDone == 38:
                #simulate vacuum at oven
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHVACUUMPOSITIONOVEN, True)
            elif iterationDone == 48:
                #simulate object gripped and moved to turntable
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHVACUUMPOSITIONOVEN, False)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHVACUUMPOSITIONTURNTABLE, True)
            elif iterationDone == 58:
                #simulate object released and moved to saw
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHTURNTABLEPOSITOINVACUUM, False)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHTURNTABLEPOSITIONSAW, True)
            elif iterationDone == 68:
                #simulate tuntable moved to conveyor belt
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHTURNTABLEPOSITIONSAW, False)
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.REFERENCESWITCHTURNTABLEPOSITIONBELT, True)
            elif iterationDone == 72:
                #simulate package ejected to conveyor belt and came at the end of the belt
                self.controller.multiProcessingSimulator.fakeSensor(RequestedParameter.LIGHTBARRIERENDOFCONVEYORBELT, False)
            
            if (notification == "") :
                iterationDone += 1
            else:
                self.assertRegex(notification, r"MultiProcessing01 \d+\.\d+ FEEDBACK 1 FINISHED")
                logging.debug(f"COMMAND FINISHED reached in {iterationDone} iterations")
                endCommandReached = True
            self.assertLess(iterationDone, 75, "COMMAND not reached in less than 75 iterations" )
    
 
if __name__ == '__main__':
    logging.basicConfig(format='[%(levelname)-5s] %(module)-25s,%(lineno)-3s| %(message)s', level=logging.DEBUG)

    unittest.main()