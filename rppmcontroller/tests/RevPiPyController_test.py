
import inspect
import logging
import os
import time
import unittest

from rppmcontroller.RevPiPyMachineController import RevPiPyMachineController
from rppmcontroller.machine.Position import Position
from rppmcontroller.machine.vacuumgripper.VacuumGripper import VacuumGripper
from rppmcontroller.protocol.JSONOutput import JSONOutput
from rppmcontroller.protocol.JSONReader import JSONReader
from rppmcontroller.protocol.MachineCommand import MachineCommand


class RevPiPyControllerTestCase(unittest.TestCase):

    def setUp(self):
        script_path = os.path.abspath(__file__)
        logging.warning(f'script path : {script_path}')
        dir_path = str(os.path.dirname(__file__))
        config_path = os.path.join(dir_path, "example/config.yml")
        logging.warning(f'config file path : {config_path}')

        logging.debug("setup called")

        # RevPiPyMachineController is abstract, so we need to instantiate a
        # child class
        class InstantiableController(RevPiPyMachineController):
            def __init__(self):
                super().__init__(config_path)

            def read(self) -> None:
                raise Exception("not implemented")

            def write(self) -> None:
                raise Exception("not implemented")

            def reset(self) -> None:
                raise Exception("not implemented")

        self.controller = InstantiableController()

        self.gripperMachine = VacuumGripper("VacuumGripper01")
        self.controller.machines = [self.gripperMachine]
        self.controller.currentlyExecuting = {
                self.gripperMachine: None
            }
        logging.debug(self.controller.currentlyExecuting.get(self.gripperMachine))


    def test_processJson_with_jsonTxtMsg(self):
        logging.debug(f'{inspect.stack()[0][3]} start')

        # noinspection SpellCheckingInspection
        jsonMessage = JSONReader.read("""{
          "topicName" : "VacuumGripper01",
          "timestamp" : 1677144787.891000000,
          "message" : {
            "jsonType" : "COMMAND",
            "type" : "VACUUM",
            "outputId" : 77,
            "name" : "MOVE",
            "parameters" : [ {
              "passableType" : "POSITIONPARAMETERTHREED",
              "passable" : {
                "meaning" : "START",
                "vertical" : 100,
                "rot" : 200,
                "horizontal" : 400
              }
            }, {
              "passableType" : "POSITIONPARAMETERTHREED",
              "passable" : {
                "meaning" : "END",
                "vertical" : 1000,
                "rot" : 2000,
                "horizontal" : 4000
              }
            } ]
          }
        }""")
        self.controller.inputBuffer.put(jsonMessage)
        # we use a multithread Queue in a mono thread, makes sure the message is queued
        time.sleep(0.1)
        #logging.debug(self.controller.inputBuffer.qsize())

        logging.debug(""+self.controller.machines[0].id)
        logging.debug(self.controller.currentlyExecuting.get(self.controller.machines[0]))

        logging.debug(self.controller.currentlyExecuting.get(self.gripperMachine))

        #logging.debug(self.controller.inputBuffer.qsize())
        self.controller.processJson(self.controller.inputBuffer)
        currentlyExecuting  = self.controller.currentlyExecuting.get(self.controller.machines[0])
        assert currentlyExecuting is not None
        logging.debug(f"currently executing={currentlyExecuting.displayName}")
        self.assertIsNotNone(currentlyExecuting.cycleStep)
        self.assertEqual(currentlyExecuting.commandId, 77)

    def test_processJson_with_JSONOutputMsg(self):

      # Create the equivalent JSONOutput command
      parameterList = [
          Position("START", 100, 200, 400),
          Position("END", 1000, 2000, 4000)
      ]
      message = MachineCommand("COMMAND", "VACUUM", 77, "MOVE", parameterList)
      jsonOutput = JSONOutput("VacuumGripper01", 1677144787.891000000, message)

      self.controller.inputBuffer.put(jsonOutput)

      # we use a multithread Queue in a mono thread, makes sure the message is queued
      time.sleep(0.1)

      self.controller.processJson(self.controller.inputBuffer)

      currentlyExecuting  = self.controller.currentlyExecuting.get(self.controller.machines[0])
      assert currentlyExecuting is not None
      self.assertIsNotNone(currentlyExecuting.cycleStep)

if __name__ == '__main__':
    logging.basicConfig(format='%(levelname)-5s: %(module)-20s,%(lineno)-3s: %(message)s', level=logging.DEBUG)

    unittest.main()
