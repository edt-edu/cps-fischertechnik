from rppmcontroller.behavior.CycleStepResult import CycleStepResult
from rppmcontroller.behavior.CycleStepResultEnum import CycleStepResultEnum
from rppmcontroller.behavior.CycleStepCommand import CycleStepCommand
from rppmcontroller.machine.Machine import Machine
from rppmcontroller.machine.MovingMachine import MovingMachine
from rppmcontroller.machine.Axis import AxisType, Axis
from rppmcontroller.machine.vacuumgripper.VacuumGripperConfig import VacuumGripperConfig
from rppmcontroller.utils.CyclicWaiter import CyclicWaiter
from rppmcontroller.machine.Position import Position
from rppmcontroller.machine.RequestedParameter import RequestedParameter
from math import isclose
import logging
from typing import Any, Callable, Dict, List, Optional, Tuple
from typing_extensions import override
import traceback


class VacuumGripper(MovingMachine):

    def generateTransferMoveListold(self, numPickup: Position, numPlace: Position):
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
    
    def generateTransferMoveList(self, numPickup: Position, numPlace: Position) -> List[CycleStepCommand]:
        offset = 250
        # Enter/retract arm
        robotPickConf0 = VacuumGripperConfig(self.vacuumSensVerticalEncoderCounter, self.vacuumSensRotEncoderCounter, 0, False)
        # move
        robotPickConf1 = VacuumGripperConfig(numPickup.vertical-offset, numPickup.rot, 0, False)
        # Take out/extend arm
        robotPickConf2 = VacuumGripperConfig(numPickup.vertical-offset, numPickup.rot, numPickup.horizontal, False)
        if isclose(self.vacuumSensVerticalEncoderCounter, numPickup.vertical-offset, abs_tol=20) and isclose(self.vacuumSensRotEncoderCounter, numPickup.rot, abs_tol=20):
            robotPickConf0 = robotPickConf2
            robotPickConf1 = robotPickConf2
        # from above
        robotPickConf3 = VacuumGripperConfig(numPickup.vertical+offset, numPickup.rot, numPickup.horizontal, False)
        # grab
        robotPickConf4 = VacuumGripperConfig(numPickup.vertical+offset, numPickup.rot, numPickup.horizontal, True)
        # up
        robotPickConf5 = VacuumGripperConfig(numPickup.vertical-offset, numPickup.rot, numPickup.horizontal, True)
        # Enter/retract arm
        robotPlaceConf0 = VacuumGripperConfig(numPickup.vertical-offset, numPickup.rot, 0, True)
        # move
        robotPlaceConf1 = VacuumGripperConfig(numPlace.vertical-offset, numPlace.rot, 0, True)
        # Take out/extend arm
        robotPlaceConf2 = VacuumGripperConfig(numPlace.vertical-offset, numPlace.rot, numPlace.horizontal, True)
        # from above
        robotPlaceConf3 = VacuumGripperConfig(numPlace.vertical+offset, numPlace.rot, numPlace.horizontal, True)
        # release/let go
        robotPlaceConf4 = VacuumGripperConfig(numPlace.vertical+offset, numPlace.rot, numPlace.horizontal, False)
        # up
        robotPlaceConf5 = VacuumGripperConfig(numPlace.vertical-offset, numPlace.rot, numPlace.horizontal, False)

        return [CycleStepCommand(lambda : self.gotoconfig( robotPickConf0), f"retract arm \n {robotPickConf0}"),
                CycleStepCommand(lambda : self.gotoconfig(robotPickConf1), f"move \n {robotPickConf1}"),
                CycleStepCommand(lambda : self.gotoconfig(robotPickConf2), f"extend arm \n {robotPickConf2}"),
                CycleStepCommand(lambda : self.gotoconfig(robotPickConf3), f"from above \n {robotPickConf3}"),
                CycleStepCommand(lambda : self.gotoconfig(robotPickConf4), f"grab \n {robotPickConf4}"),
                CycleStepCommand(lambda : self.gotoconfig(robotPickConf5), f"up \n {robotPickConf5}"),
                CycleStepCommand(lambda : self.gotoconfig(robotPlaceConf0), f"retract arm \n {robotPlaceConf0}"),
                CycleStepCommand(lambda : self.gotoconfig(robotPlaceConf1), f"move \n {robotPlaceConf1}"),
                CycleStepCommand(lambda : self.gotoconfig(robotPlaceConf2), f"extend arm \n {robotPlaceConf2}"),
                CycleStepCommand(lambda : self.gotoconfig(robotPlaceConf3), f"from above \n {robotPlaceConf3}"),
                CycleStepCommand(lambda : self.gotoconfig(robotPlaceConf4), f"release \n {robotPlaceConf4}"),
                CycleStepCommand(lambda : self.gotoconfig(robotPlaceConf5), f"up \n {robotPlaceConf5}")
                ]


    @Machine.isExecuting.getter
    def isExecuting(self) -> bool:

        res = self.isProcessingSequence() or \
            self.__vacuumActVerticalUp or self.__vacuumActVerticalDown or self.__vacuumActRotRight or self.__vacuumActRotLeft or \
            self.__vacuumActArmOut or self.__vacuumActArmIn or \
            self.nbMinimumRequiredExecutionCycles != 0 or \
            self.isProcessingSequence()
        
        # log isexecuting and debug info only if message has changed
        psContext = self.processSequenceContext
        if psContext is not None:
            processSequencContextStatus = f'| {psContext.currentSubCycleStepIndex+1}/{len(psContext.subCycleStepList)} '
        else:
            processSequencContextStatus = ''
        isExecuting_log = f'isExecuting({self.id})={res} {processSequencContextStatus}| Sensors={self.sensorStatusString()} | Actuators= {self.actuatorStatusString()} | nbMinimumRequiredExecutionCycles={self.nbMinimumRequiredExecutionCycles} '
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
            "isExecuting": self.isExecuting,
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

    @override
    def gotoconfig(self, config) -> CycleStepResult:
        """Activate the different engines and actuator in order to reach the given VGR configuration
        All axis are moved simultaneously
        It supposes that the encoder has been initialized
        As a security, if the encoder indicates that it can stil be moved toward its reference switch, but the switch is active, then it stops with an error
        """
        logging.info(f"gotoconfig_CycleSubStep {config}")
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
        if (self.vacuumActArmIn and self.vacuumSensArmEndIn) or \
           (self.vacuumActRotRight and self.vacuumSensRotEnd) or \
           (self.vacuumActVerticalUp and self.vacuumSensVerticalEndUp):
            self.vacuumActArmIn = self.vacuumActRotRight = self.vacuumActVerticalUp =False
            return CycleStepResult(CycleStepResultEnum.ABORTED_ERROR, 
                                    f"cannot move beyond reference sensor, machine is probably not initialized", 
                                    None)
        
        if (t1 and t2 and t3 and t4):
            return CycleStepResult(CycleStepResultEnum.DONE, 
                                    f"gotoconfig {config}", 
                                    None)
        else:
            return CycleStepResult(CycleStepResultEnum.MUST_CONTINUE, 
                                    f"gotoconfig {config}", 
                                    None)


    ### ____________ Functions intended to be called in the exLoop function of the RevPiPyMachineController ________________

    @override
    def setup_CycleStep(self) -> CycleStepResult: 
        """
        Used to move the engine to a reference point (ie. a point with a reference switch) so we can reset the counters or encoders

        :return: as a CycleStep, this function must return True when it is finished so it can be removed from the currentlyExecuting map
        """  
        logging.debug("VGR setup")    
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
        if (t1 and t2 and t3 and self.nbMinimumRequiredExecutionCycles == 0):
            return CycleStepResult(CycleStepResultEnum.DONE)
        else:
            return CycleStepResult(CycleStepResultEnum.MUST_CONTINUE)
    
    @override
    def stop_CycleStep(self) -> CycleStepResult:
        self.__vacuumActArmOut = self.__vacuumActArmIn = False
        self.__vacuumActVerticalDown = self.__vacuumActVerticalUp = False
        self.__vacuumActRotRight = self.__vacuumActRotLeft = False
        self.__vacuumActCompressorOn = self.__vacuumActValve = False
        return CycleStepResult(CycleStepResultEnum.DONE)

    ### ____________ Functions callable from orchestrator ________________
    #   function name must be lowercase and finish with '_Command' postfix (cf. RevPiPyMachineController)
    # they must return a lambda to a CycleStep function

    def setup_Command(self) -> Optional[Callable[[], CycleStepResult]]:
        """
        Command to triggering a setup. Used to move the engines to a reference point (ie. a point with a reference switch) so we can reset the counters or encoders

        :return: as a _Command, this function returns a lamba to a CycleStep method applying the setup
        """ 
        return lambda: self.setup_CycleStep()

    def gotopos_Command(self, tartgetPos : Position) -> Optional[Callable[[], CycleStepResult]]:
        """
        Command triggering a gotopos action. Ie. it moves the gripper to the position without changing the valve or compressor status.
        It may trigger a setup first if the machine is not initialized
        :return: as a _Command, this function returns a lamba to a CycleStep method applying the move
        """ 
        moveList : List[CycleStepCommand] =  []
        if not self.setupFinished:
            moveList.append(CycleStepCommand(lambda: self.setup_CycleStep(), "setup"))
        config = VacuumGripperConfig(tartgetPos.vertical, tartgetPos.rot, tartgetPos.horizontal, False)
        moveList.append(CycleStepCommand(lambda: self.gotoconfig(config), 
                                         "gotopos \n {config}"))
        return lambda: self.process_sequence_CycleStep(moveList)
      
    def move_Command(self, startPos, endPos) -> Optional[Callable[[], CycleStepResult]]:
        """
        Command triggering a move token action. Ie. it picks a token on the startPos and drop it on the endPos
        :return: as a _Command, this function returns a lamba to a CycleStep method applying the move
        """ 
        
        print("move")

        moveList : List[CycleStepCommand] =  []
        if not self.setupFinished:
            moveList.append(CycleStepCommand(lambda: self.setup_CycleStep(), "setup"))

        moveList.extend(self.generateTransferMoveList(startPos, endPos))
        return lambda: self.process_sequence_CycleStep(moveList)


    def pick_Command(self, startPos) -> Optional[Callable[[], CycleStepResult]]:
        """
        Command triggering a pick token action. Ie. it move the arm to the startPos and grips a token on that posiotn
        :return: as a _Command, this function returns a lamba to a CycleStep method applying the pick
        """ 
        moveList : List[CycleStepCommand] =  []
        if not self.setupFinished:
            moveList.append(CycleStepCommand(lambda: self.setup_CycleStep(), "setup"))

        moveList.extend(self.generateTransferMoveList(startPos, startPos)[:6]) #extract the first 6 commands from the generateTransferMoveList
        return lambda: self.process_sequence_CycleStep(moveList)

    def place_Command(self, endPos) -> Optional[Callable[[], CycleStepResult]]:
        """
        Command triggering a place token action. Ie. it move the arm to the endPos and release the token on that posiotn
        :return: as a _Command, this function returns a lamba to a CycleStep method applying the place
        """ 
        moveList : List[CycleStepCommand] =  []
        if not self.setupFinished:
            moveList.append(CycleStepCommand(lambda: self.setup_CycleStep(), "setup"))

        moveList.extend(self.generateTransferMoveList(endPos, endPos)[6:]) #extract the last 6 commands from the generateTransferMoveList
        return lambda: self.process_sequence_CycleStep(moveList)

    def stop_Command(self) -> Optional[Callable[[], CycleStepResult]]:
        return lambda: self.stop_CycleStep()
