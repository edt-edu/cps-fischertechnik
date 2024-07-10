
import inspect
import logging
import time
import unittest

from rppmcontroller.machine.vacuumgripper.VacuumGripper import VacuumGripper
from rppmcontroller.machine.Position import Position
from rppmcontroller.machine.BoxNumber import BoxNumber
from rppmcontroller.machine.Direction import Direction
from rppmcontroller.machine.Color import Color
from rppmcontroller.protocol.JSONReader import JSONReader
from rppmcontroller.protocol.JSONOutput import JSONOutput
from rppmcontroller.protocol.MachineCommand import MachineCommand
from rppmcontroller.RevPiPyMachineController import RevPiPyMachineController


class RevPiPyControllerTestCase(unittest.TestCase):

    def setUp(self):
        logging.debug("setup called")
        # pickupRobot1 = [2600,3550,25]
        # placeConveyorRobot1 = [2000,100,100]
        # placeRand = [2,3,4,5]
        # placeListrobot1 = [pickupRobot1, placeConveyorRobot1, placeRand]
        self.controller = RevPiPyMachineController()

        self.gripperMachine = VacuumGripper("VacuumGripper01")
        self.controller.machines = [self.gripperMachine]
        self.controller.currentlyExecuting = {
                self.gripperMachine: [None, None]
            }
        logging.debug(self.controller.currentlyExecuting.get(self.gripperMachine))
        

    def test_processJson_with_jsonTxtMsg(self):
        logging.debug(f'{inspect.stack()[0][3]} start')

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
        currentlyExecutting  = self.controller.currentlyExecuting.get(self.controller.machines[0]) 
        assert currentlyExecutting is not None
        logging.debug("currently executing="+'|'.join(str(e) for e in currentlyExecutting))
        self.assertIsNotNone(currentlyExecutting[0])
        self.assertEqual(currentlyExecutting[1],77)
        
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

      currentlyExecutting  = self.controller.currentlyExecuting.get(self.controller.machines[0]) 
      assert currentlyExecutting is not None
      self.assertIsNotNone(currentlyExecutting[0])

if __name__ == '__main__':
    logging.basicConfig(format='%(levelname)-5s: %(module)-20s,%(lineno)-3s: %(message)s', level=logging.DEBUG)

    unittest.main()
