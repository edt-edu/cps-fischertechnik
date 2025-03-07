
import inspect
import logging
import os
import time
import unittest
from unittest.mock import patch, Mock

from rppmcontroller.example.VacuumGripperController import VacuumGripperController
from rppmcontroller.machine.vacuumgripper.VacuumGripper import VacuumGripper
from rppmcontroller.protocol.JSONReader import JSONReader
from rppmcontroller.protocol.JSONOutput import JSONOutput
from rppmcontroller.machine.Position import Position
from rppmcontroller.protocol.MachineCommand import MachineCommand


class VacuumGripperControllerTestCase(unittest.TestCase):

    def setUp(self):
        script_path = os.path.abspath(__file__)
        logging.warning(f'script path : {script_path}')
        dir_path = os.path.dirname(__file__)
        config_path = os.path.join(dir_path, "config.yml")
        logging.warning(f'config file path : {config_path}')

        logging.debug("setup called")
        # pickupRobot1 = [2600,3550,25]
        # placeConveyorRobot1 = [2000,100,100]
        # placeRand = [2,3,4,5]
        # placeListrobot1 = [pickupRobot1, placeConveyorRobot1, placeRand]
        self.controller = VacuumGripperControllerMock(config_path)

        

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
        logging.debug(self.controller.currentlyExecuting.get(self.controller.vacuumGripperMachine))

        logging.debug(self.controller.currentlyExecuting.get(self.controller.vacuumGripperMachine))

        #logging.debug(self.controller.inputBuffer.qsize())
        self.controller.processJson(self.controller.inputBuffer)
        currentlyExecutting  = self.controller.currentlyExecuting.get(self.controller.vacuumGripperMachine) 
        assert currentlyExecutting is not None
        logging.debug("currently executing="+'|'.join(str(e) for e in currentlyExecutting))
        self.assertIsNotNone(currentlyExecutting[0])
        self.assertEqual(currentlyExecutting[1],77)
        
    def test_processJson_with_JSONOutputMsg(self):
        
        logging.debug(f'{inspect.stack()[0][3]} start')
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

        currentlyExecutting  = self.controller.currentlyExecuting.get(self.controller.vacuumGripperMachine) 
        assert currentlyExecutting is not None
        self.assertIsNotNone(currentlyExecutting[0])

  
    
    def test_tryMockRead(self):
        logging.debug(f'{inspect.stack()[0][3]} start')
        # self.assertEquals(self.controller.vacuumGripperMachine.vacuumSensVerticalEndUp, 0)
        self.controller.read()
        self.controller.write()
        self.assertEqual(self.controller.vacuumGripperMachine.vacuumSensVerticalEndUp, 0)

          

    def test_oneExLoop(self):
        logging.debug(f'{inspect.stack()[0][3]} start')
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
        currentlyExecutting  = self.controller.currentlyExecuting.get(self.controller.vacuumGripperMachine)
        assert currentlyExecutting is not None
        logging.debug("before loop: currently executing="+'|'.join(str(e) for e in currentlyExecutting))

        self.controller.read()
        self.controller.exLoop()
        self.controller.write()
        self.controller.createMachineFeedbackOnChange()
        self.controller.createCommandFeedbackOnChange()

        currentlyExecutting  = self.controller.currentlyExecuting.get(self.controller.vacuumGripperMachine)
        assert currentlyExecutting is not None
        logging.debug("after loop: currently executing="+'|'.join(str(e) for e in currentlyExecutting))
        self.assertIsNotNone(currentlyExecutting[0])

        # Second loop
        self.controller.processJson(self.controller.inputBuffer)

        currentlyExecutting  = self.controller.currentlyExecuting.get(self.controller.vacuumGripperMachine)
        assert currentlyExecutting is not None
        logging.debug("before loop: currently executing="+'|'.join(str(e) for e in currentlyExecutting))

        self.controller.read()
        self.controller.exLoop()
        self.controller.write()
        self.controller.createMachineFeedbackOnChange()
        self.controller.createCommandFeedbackOnChange()

        currentlyExecutting  = self.controller.currentlyExecuting.get(self.controller.vacuumGripperMachine)
        assert currentlyExecutting is not None
        logging.debug("after loop: currently executing="+'|'.join(str(e) for e in currentlyExecutting))
        self.assertIsNotNone(currentlyExecutting[0])
    

class VacuumGripperControllerMock(VacuumGripperController):
  def __init__(self, configurationFile : str = ""):
     super().__init__(simulatedRevPiModIO=True, configurationFile=configurationFile)

  def read(self):
    logging.debug("mocked VacuumGripperController read called")
    self.vacuumGripperMachine.vacuumSensVerticalEndUp = 0
    self.vacuumGripperMachine.vacuumSensArmEndIn = 0
    self.vacuumGripperMachine.vacuumSensRotEnd = 0
    self.vacuumGripperMachine.vacuumSensVerticalEncoderCounter = 0
    self.vacuumGripperMachine.vacuumSensArmEncoderCounter = 0
    self.vacuumGripperMachine.vacuumSensRotEncoderCounter = 0

  def write(self):
    logging.debug("mocked VacuumGripperController write called")
    pass

if __name__ == '__main__':
    logging.basicConfig(format='[%(levelname)-5s] %(module)-25s,%(lineno)-3s| %(message)s', level=logging.DEBUG)

    unittest.main()
