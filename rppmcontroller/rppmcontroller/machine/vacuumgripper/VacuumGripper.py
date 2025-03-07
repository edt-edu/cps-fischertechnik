from rppmcontroller.machine.MovingMachine import MovingMachine
from rppmcontroller.machine.Axis import AxisType, Axis
from rppmcontroller.machine.vacuumgripper.VacuumGripperConfig import VacuumGripperConfig
from rppmcontroller.utils.CyclicWaiter import CyclicWaiter
from rppmcontroller.machine.Position import Position
from rppmcontroller.machine.RequestedParameter import RequestedParameter
from math import isclose
import logging
from typing import Any, Dict, Tuple
import traceback


class VacuumGripper(MovingMachine):

    def generateTransferMoveList(self, numPickup: Position, numPlace: Position):
        offset = 250
        # Enter/retract arm / Arm einfahren
        robotPickConf0 = VacuumGripperConfig(self.vacuumSensVerticalEncoderCounter, self.vacuumSensRotEncoderCounter, 0, False)
        # move / bewegen
        robotPickConf1 = VacuumGripperConfig(numPickup.vertical-offset, numPickup.rot, 0, False)
        # Take out/extend arm / Arm ausfahen
        robotPickConf2 = VacuumGripperConfig(numPickup.vertical-offset, numPickup.rot, numPickup.horizontal, False)
        if isclose(self.vacuumSensVerticalEncoderCounter, numPickup.vertical-offset, abs_tol=20) and isclose(self.vacuumSensRotEncoderCounter, numPickup.rot, abs_tol=20):
            robotPickConf0 = robotPickConf2
            robotPickConf1 = robotPickConf2
        # from above / von oben heran
        robotPickConf3 = VacuumGripperConfig(numPickup.vertical+offset, numPickup.rot, numPickup.horizontal, False)
        # grab / greifen
        robotPickConf4 = VacuumGripperConfig(numPickup.vertical+offset, numPickup.rot, numPickup.horizontal, True)
        # up / nach oben
        robotPickConf5 = VacuumGripperConfig(numPickup.vertical-offset, numPickup.rot, numPickup.horizontal, True)
        # Enter/retract arm / Arm einfahren
        robotPlaceConf0 = VacuumGripperConfig(numPickup.vertical-offset, numPickup.rot, 0, True)
        # move / bewegen
        robotPlaceConf1 = VacuumGripperConfig(numPlace.vertical-offset, numPlace.rot, 0, True)
        # Take out/extend arm / Arm ausfahren
        robotPlaceConf2 = VacuumGripperConfig(numPlace.vertical-offset, numPlace.rot, numPlace.horizontal, True)
        # from above / von oben heran
        robotPlaceConf3 = VacuumGripperConfig(numPlace.vertical+offset, numPlace.rot, numPlace.horizontal, True)
        # release/let go / loslassen
        robotPlaceConf4 = VacuumGripperConfig(numPlace.vertical+offset, numPlace.rot, numPlace.horizontal, False)
        # up / nach oben
        robotPlaceConf5 = VacuumGripperConfig(numPlace.vertical-offset, numPlace.rot, numPlace.horizontal, False)

        return [robotPickConf0,
                robotPickConf1,
                robotPickConf2,
                robotPickConf3,
                robotPickConf4,
                robotPickConf5,
                robotPlaceConf0,
                robotPlaceConf1,
                robotPlaceConf2,
                robotPlaceConf3,
                robotPlaceConf4,
                robotPlaceConf5]

    def __isExecuting(self) -> bool:
        return self.__vacuumActVerticalUp or self.__vacuumActVerticalDown or self.__vacuumActRotRight or self.__vacuumActRotLeft or \
            self.__vacuumActArmOut or self.__vacuumActArmIn or \
            self.nbMinimumRequiredExecutionCycles != 0 or \
            self.hasRemainingMove()
    @property
    def isExecuting(self) -> bool:

        res = self.__isExecuting()
            
            # self.__vacuumActCompressorOn or self.__vacuumActValve or  we have pick command that doesn't finish with an iddle machine
        
        # log isexecuting and debug info only if message has changed
        isExecuting_log = f'isExecuting({self.id})={res} | pc={self.pc}/nbMove={self.nbMove()} | Sensors={self.sensorStatusString()} | Actuators= {self.actuatorStatusString()}'
        if isExecuting_log != self.previous_isExecuting_log :
            logging.debug(isExecuting_log)
            self.previous_isExecuting_log = isExecuting_log
        
        return res


    def __init__(self, id1):
        self.__isExecutingCount = 0
        self.__vacuumSensArmEndIn = self.__vacuumSensVerticalEndUp = self.__vacuumSensRotEnd = False
        self.__vacuumActArmOut = self.__vacuumActArmIn = self.__vacuumActVerticalDown = self.__vacuumActVerticalUp = self.__vacuumActRotRight = self.__vacuumActRotLeft = self.__vacuumActCompressorOn = self.__vacuumActValve = False
        self.__vacuumSensRotEncoderCounter = self.__vacuumSensVerticalEncoderCounter = self.__vacuumSensArmEncoderCounter = 0
        self.__axisArm = Axis(AxisType.Encoder, 20)
        self.__axisVertical = Axis(AxisType.Encoder, 20)
        self.__axisRot = Axis(AxisType.Encoder, 20)
        self.__gripperWaiter = CyclicWaiter(10)
        dictMap = {RequestedParameter.REFERENCESWITCHVERTICALAXIS: self.__vacuumSensVerticalEndUp,
                   RequestedParameter.REFERENCESWITCHHORIZONTALAXIS: self.__vacuumSensArmEndIn,
                   RequestedParameter.REFERENCESWITCHROTATE: self.__vacuumSensRotEnd,
                   RequestedParameter.VERTICALAXISSTEP: self.__axisVertical.counterValueCurrent,
                   RequestedParameter.HORIZONTALAXISSTEP: self.__axisArm.counterValueCurrent,
                   RequestedParameter.ROTATESTEP: self.__axisRot.counterValueCurrent,
                   RequestedParameter.MOTORVERTICALAXISUP: self.__vacuumActVerticalUp,
                   RequestedParameter.MOTORVERTICALAXISDOWN: self.__vacuumActVerticalDown,
                   RequestedParameter.MOTORHORIZONTALAXISBACKWARD: self.__vacuumActArmIn,
                   RequestedParameter.MOTORHORIZONTALAXISFORWARD: self.__vacuumActArmOut,
                   RequestedParameter.MOTORROTATECLOCKWISE: self.__vacuumActRotRight,
                   RequestedParameter.MOTORROTATECOUNTERCLOCKWISE: self.__vacuumActRotLeft,
                   RequestedParameter.COMPRESSOR: self.__vacuumActCompressorOn,
                   RequestedParameter.VALVEVACUUM: self.__vacuumActValve}
        super().__init__(id1, dictMap)

        self.configReached = False
        self.setupFinished = self.setupFinishedHelper = False
        self.__moveList = None
        self.__pc = 0
        self.configGoal = None
        self.previous_isExecuting_log = None


    @property
    def vacuumActCompressorOn(self):
        return self.__vacuumActCompressorOn

    @vacuumActCompressorOn.setter
    def vacuumActCompressorOn(self, value):
        self.__vacuumActCompressorOn = value

    @property
    def vacuumActValve(self):
        return self.__vacuumActValve

    @vacuumActValve.setter
    def vacuumActValve(self, value):
        self.__vacuumActValve = value

    @property
    def vacuumSensVerticalEndUp(self) -> bool:
        """The Vacuum Gripper sensor for vertical axis

        True if arm is up at maximum position  so that it touches the sensor"""
        return self.__vacuumSensVerticalEndUp

    @vacuumSensVerticalEndUp.setter
    def vacuumSensVerticalEndUp(self, value):
        self.__vacuumSensVerticalEndUp = value

    @property
    def vacuumSensVerticalEncoderCounter(self):
        return self.__vacuumSensVerticalEncoderCounter

    @vacuumSensVerticalEncoderCounter.setter
    def vacuumSensVerticalEncoderCounter(self, value):
        self.__vacuumSensVerticalEncoderCounter = value

    @property
    def vacuumSensArmEndIn(self) -> bool:
        """The Vacuum Gripper sensor for horizontal axis

        True if arm is retracted at maximum position  so that it touches the sensor"""
        return self.__vacuumSensArmEndIn

    @vacuumSensArmEndIn.setter
    def vacuumSensArmEndIn(self, value):
        self.__vacuumSensArmEndIn = value

    @property
    def vacuumSensArmEncoderCounter(self):
        return self.__vacuumSensArmEncoderCounter

    @vacuumSensArmEncoderCounter.setter
    def vacuumSensArmEncoderCounter(self, value):
        self.__vacuumSensArmEncoderCounter = value

    @property
    def vacuumSensRotEnd(self):
        """The Vacuum Gripper sensor for rotation

        True if arm is rotated clockwise at maximum position so that it touches the sensor"""
        return self.__vacuumSensRotEnd

    @vacuumSensRotEnd.setter
    def vacuumSensRotEnd(self, value):
        self.__vacuumSensRotEnd = value

    @property
    def vacuumSensRotEncoderCounter(self):
        return self.__vacuumSensRotEncoderCounter

    @vacuumSensRotEncoderCounter.setter
    def vacuumSensRotEncoderCounter(self, value):
        self.__vacuumSensRotEncoderCounter = value

    @property
    def vacuumActArmOut(self):
        return self.__vacuumActArmOut

    @vacuumActArmOut.setter
    def vacuumActArmOut(self, value):
        self.__vacuumActArmOut = value

    @property
    def vacuumActArmIn(self):
        return self.__vacuumActArmIn

    @vacuumActArmIn.setter
    def vacuumActArmIn(self, value):
        self.__vacuumActArmIn = value

    @property
    def vacuumActVerticalDown(self):
        return self.__vacuumActVerticalDown

    @vacuumActVerticalDown.setter
    def vacuumActVerticalDown(self, value):
        self.__vacuumActVerticalDown = value

    @property
    def vacuumActVerticalUp(self):
        return self.__vacuumActVerticalUp

    @vacuumActVerticalUp.setter
    def vacuumActVerticalUp(self, value):
        self.__vacuumActVerticalUp = value

    @property
    def vacuumActRotRight(self):
        return self.__vacuumActRotRight

    @vacuumActRotRight.setter
    def vacuumActRotRight(self, value):
        self.__vacuumActRotRight = value

    @property
    def vacuumActRotLeft(self):
        return self.__vacuumActRotLeft

    @vacuumActRotLeft.setter
    def vacuumActRotLeft(self, value):
        self.__vacuumActRotLeft = value

    def sensorStatusString(self) -> str:
        return f"[{self.vacuumSensVerticalEncoderCounter}, {self.vacuumSensRotEncoderCounter}, {self.vacuumSensArmEncoderCounter}][{self.vacuumSensVerticalEndUp}, {self.vacuumSensRotEnd}, {self.vacuumSensArmEndIn}]"

    def actuatorStatusString(self) -> str:
        return f"[{self.__vacuumActVerticalUp}, {self.__vacuumActVerticalDown}], [{self.__vacuumActRotRight}, {self.__vacuumActRotLeft}], [{self.__vacuumActArmOut}, {self.__vacuumActArmIn}], [{self.__vacuumActCompressorOn}, {self.__vacuumActValve}]"


    def inputStatus(self) -> Dict[str, Any]:
        status = {
            "vacuumSensVerticalEncoderCounter": self.vacuumSensVerticalEncoderCounter,
            "vacuumSensRotEncoderCounter": self.vacuumSensRotEncoderCounter,
            "vacuumSensArmEncoderCounter": self.vacuumSensArmEncoderCounter,
            "vacuumSensVerticalEndUp": self.vacuumSensVerticalEndUp,
            "vacuumSensRotEnd": self.vacuumSensRotEnd,
            "vacuumSensArmEndIn": self.vacuumSensArmEndIn,
        }
        return status

    def outputStatus(self) -> Dict[str, Any]:
        
        status = {
            "vacuumActVerticalUp": self.__vacuumActVerticalUp,
            "vacuumActVerticalDown": self.__vacuumActVerticalDown,
            "vacuumActRotRight": self.__vacuumActRotRight,
            "vacuumActRotLeft": self.__vacuumActRotLeft,
            "vacuumActArmOut": self.__vacuumActArmOut,
            "vacuumActArmIn": self.__vacuumActArmIn,
            "vacuumActCompressorOn": self.__vacuumActCompressorOn,
            "vacuumActValve": self.__vacuumActValve,
        }
        return status
    
    def internalStatus(self) -> Dict[str, Any]:
        status = {
            "isExecuting": self.__isExecuting(),
        }
        return status

    def executeHelper(self) -> Tuple[bool, bool, bool]:
        """
        Returns whether each counter can be reset after the setup process

        :returns three boolean values
        :rtype tuple
        """
        if self.setupFinishedHelper:
            self.setupFinishedHelper = False    
            return True, True, True
        else:
            return False, False, True

    def gotoconfig(self, config):
        logging.info(f"gotoconfig {config}")
        iconfig = config
        if config is None:
            iconfig = VacuumGripperConfig(0,0,0,False)
        t1 = t2 = t3 = t4 = False
        d3 = None

        self.__axisVertical.update(self.vacuumSensVerticalEndUp, self.vacuumSensVerticalEncoderCounter)
        t1 = self.__axisVertical.gotoConfig(iconfig.endVertical, iconfig.counterVertical)
        self.vacuumActVerticalUp = self.__axisVertical.outputminus
        self.vacuumActVerticalDown = self.__axisVertical.outputplus

        self.__axisRot.update(self.vacuumSensRotEnd, self.vacuumSensRotEncoderCounter)
        t2 = self.__axisRot.gotoConfig(iconfig.endRot, iconfig.counterRot)
        self.vacuumActRotRight = self.__axisRot.outputminus
        self.vacuumActRotLeft = self.__axisRot.outputplus

        self.__axisArm.update(self.vacuumSensArmEndIn, self.vacuumSensArmEncoderCounter)
        t3 = self.__axisArm.gotoConfig(iconfig.endArm, iconfig.counterArm)
        self.vacuumActArmIn = self.__axisArm.outputminus
        self.vacuumActArmOut = self.__axisArm.outputplus

        if iconfig.gripperActive:
            self.__vacuumActCompressorOn = True
            self.__vacuumActValve = True
            t4 = self.__gripperWaiter.wait()
        else:
            self.__vacuumActCompressorOn = False
            self.__vacuumActValve = False
            t4 = True
        return (t1 and t2 and t3 and t4)


    #referenzfahrt des vacuumers um alle counter korrekt zu setzen - setzen der counter an anderer Stelle, hier aber in Referenzposition
    #von execute bereits berücksichtigt
    # reference journey of the vacuum to set all counters correctly - put the counter elsewhere, but here in reference position
    # already considered by execute
    def setup(self) -> bool:       
        # activate engines toward the sensors if necessary
        self.vacuumActArmOut = self.vacuumActRotLeft = self.vacuumActVerticalDown = False
        t1 = t2 = t3 = False
        if self.vacuumSensArmEndIn:
            self.vacuumActArmIn = False
            t3 = True
        else:
            self.vacuumActArmIn = True

        if self.vacuumSensVerticalEndUp:
            self.vacuumActVerticalUp = False
            t1 = True
        else:
            self.vacuumActVerticalUp = True

        if self.vacuumSensRotEnd:
            self.vacuumActRotRight = False
            t2 = True
        else:
            self.vacuumActRotRight = True

        self.vacuumActCompressorOn = False
        self.vacuumActValve = False

        self.setupFinished = (t1 and t2 and t3)
        
        if not self.setupFinished:
            self.setupFirst = True 
        else:
            self.setupFinishedHelper = True # ask for a counter reset in the main loop

        if self.setupFirst:
            logging.debug("setup first True")
            self.setupFirst = False
        return lambda: self.setup()

    def gotopos(self, endPos : Position) :
        """Go move gropper to reach the given position without changing the valve or compressor status
        """

        self.setupFirst = True
        moveList = [VacuumGripperConfig(counterVertical=endPos.vertical, counterRot = endPos.rot, counterArm=endPos.horizontal, gripperActive=self.vacuumActValve )]
        return lambda: self.execute(endPos, endPos, moveList)

    def move(self, startPos, endPos):
        self.setupFirst = True
        self.setupCount = 0
        self.__isExecutingCount = 0
        print("move")
        moveList = self.generateTransferMoveList(startPos, endPos)
        for move in moveList:
            logging.debug(f'    {move}')
        # TODO make this execution function be called multiple times in the background until the execution is finished
        # or do this in JSONProcessingIntegrationMain by keeping all currently executing functions in a list and call them from there (detecting change probably more easily)
        #self.execute(startPos, endPos, moveList)
        return lambda: self.execute(startPos, endPos, moveList)

    def pick(self, startPos):
        self.setupFirst = True
        moveList = self.generateTransferMoveList(startPos, startPos)
        moveList = moveList[:6]
        return lambda: self.execute(startPos, startPos, moveList)

    def place(self, endPos):
        self.setupFirst = True
        moveList = self.generateTransferMoveList(endPos, endPos)
        moveList = moveList[6:]
        return lambda: self.execute(endPos, endPos, moveList)

    def stop(self):
        self.__vacuumActArmOut = self.__vacuumActArmIn = self.__vacuumActVerticalDown = self.__vacuumActVerticalUp = self.__vacuumActRotRight = self.__vacuumActRotLeft = self.__vacuumActCompressorOn = self.__vacuumActValve = False
        return None
