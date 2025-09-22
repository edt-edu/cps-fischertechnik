import inspect
import logging
import os
import re
import unittest
from unittest.mock import patch, Mock

from rppmcontroller.example.SimulatedHighBayController import SimulatedHighBayController
from rppmcontroller.machine.RequestedParameter import RequestedParameter
from rppmcontroller.machine.Direction import Direction
from rppmcontroller.protocol.MachineCommand import MachineCommand

import tests.controllerTestHelper as ctHelper


class SimulatedHighBayControllerIntegrationTestCase(unittest.TestCase):

    def setUp(self):
        script_path = os.path.abspath(__file__)
        logging.info(f'script path : {script_path}')
        dir_path = os.path.dirname(__file__)
        config_path = os.path.join(dir_path, "config.yml")
        logging.info(f'config file path : {config_path}')

        logging.debug("setup called")
        self.controller = SimulatedHighBayController(config_path)
        self.controller.mainLoopDelay = 0.1


    ### ________ SETUP ___________
    def test_setup(self):
        '''
            Test the setup_Command
        '''
        logging.debug(f'{inspect.stack()[0][3]} start')

        # initial feedback
        self.controller.mainLoopIteration()
        self.assertRegex(ctHelper.readNotification(self.controller), r"HighBay01 \d+\.\d+ MACHINE_FEEDBACK UNINITIALIZED_IDLE")

        # controller is idle
        self.controller.mainLoopIteration()
        self.assertEqual(ctHelper.readNotification(self.controller), "")

        # send a move command
        message = MachineCommand("COMMAND", "WAREHOUSE", 1, "SETUP", [])

        ctHelper.sendMessage(self.controller, "HighBay01", message)

        self.controller.mainLoopIteration()
        self.controller.mainLoopIteration()

        self.assertRegex(ctHelper.readMachineFeedbackNotification(self.controller), r"HighBay01 \d+\.\d+ MACHINE_FEEDBACK UNINITIALIZED_ACTIVE")

        endCommandReached = False
        iterationDone = 0
        while not endCommandReached:
            self.controller.mainLoopIteration()
            notification = ctHelper.readCommandFeedbackNotification(self.controller)
            '''Simulate sensor changes for testing all the functionnalities of the command'''
            if iterationDone == 4:
                # retract cantilever
                self.controller.highBaySimulator.fakeSensor(RequestedParameter.REFERENCESWITCHCANTILEVERBACK,True)
            elif iterationDone == 8:
                # arm moved to reference switches
                self.controller.highBaySimulator.fakeSensor(RequestedParameter.REFERENCESWITCHVERTICALAXIS,True)
                self.controller.highBaySimulator.fakeSensor(RequestedParameter.REFERENCESWITCHHORIZONTALAXIS,True)

            if notification == "" :
                iterationDone += 1
                logging.debug("Next iteration is " + str(iterationDone))
            elif re.match(r"HighBay01 \d+\.\d+ COMMAND_FEEDBACK 1 MUST_CONTINUE .*", notification):
                pass
            else:
                self.assertRegex(notification, r"HighBay01 \d+\.\d+ COMMAND_FEEDBACK 1 DONE")
                notification = ctHelper.readMachineFeedbackNotification(self.controller)
                self.assertRegex(notification, r"HighBay01 \d+\.\d+ MACHINE_FEEDBACK INITIALIZED_IDLE")
                logging.debug(f"COMMAND DONE reached in {iterationDone} iterations")
                endCommandReached = True
            self.assertLess(iterationDone, 100, "COMMAND not reached in less than 100 iterations" )





if __name__ == '__main__':
    logging.basicConfig(format='[%(levelname)-5s] %(module)-25s,%(lineno)-3s| %(message)s', level=logging.DEBUG)

    unittest.main()
