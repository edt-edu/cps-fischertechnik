import logging

from rppmcontroller.machine.Machine import Machine
from rppmcontroller.machine.Direction import Direction
from rppmcontroller.machine.RequestedParameter import RequestedParameter
from typing import Any, Callable, Dict, List, Optional
from typing_extensions import override

class MultiProcessing(Machine):

    @Machine.isExecuting.getter
    def isExecuting(self) -> bool:
        res = self.processing or self.__multiProcessingActRotClockwise or self.__multiProcessingActRotCounterclockwise or self.__multiProcessingActConveyorForward or self.__multiProcessingActSaw or self.__multiProcessingActOvenInward or self.__multiProcessingActOvenOutward or self.__multiProcessingActGripperToOven or self.__multiProcessingActGripperToTurntable or self.__multiProcessingOvenLight or self.__multiProcessingCompressor or self.__multiProcessingActLowerValve or self.__multiProcessingValveFeeder
        # log isexecuting and debug info only if message has changed
        isExecuting_log = f'isExecuting({self.id})={res} | Sensors={self.sensorStatusString()} | Actuators= {self.actuatorStatusString()}'
        if isExecuting_log != self.previous_isExecuting_log :
            logging.debug(isExecuting_log)
            self.previous_isExecuting_log = isExecuting_log

        return res

    @Machine.isCommandSuccessed.getter
    def isCommandSuccessed(self) -> bool:
        return self.__isCommandSuccessed

    @Machine.isCommandRunning.getter
    def isCommandRunning(self) -> bool:
        return self.__isCommandRunning
    
    @Machine.isCommandTimedOut.getter
    def isCommandTimedOut(self) -> bool:
        return self.__isCommandTimedOut

    def __init__(self, id1):
        self.__multiProcessingSensTurntablePosVacuum = self.__multiProcessingSensTurntablePosBelt = False
        self.__multiProcessingSensEndConveyor = self.__multiProcessingSensOven = True #Light barrier sensors are True by default
        self.__multiProcessingSensTurntablePosSaw = self.__multiProcessingSensVacuumGripperAtTurntable = False
        self.__multiProcessingSensOvenFeederIn = self.__multiProcessingSensOvenFeederOut = False
        self.__multiProcessingSensVacuumGripperAtOven = False
        self.__multiProcessingActRotClockwise = self.__multiProcessingActRotCounterclockwise = False
        self.__multiProcessingActConveyorForward = False
        self.__multiProcessingActSaw = False
        self.__multiProcessingActOvenInward = self.__multiProcessingActOvenOutward = False
        self.__multiProcessingActGripperToOven = self.__multiProcessingActGripperToTurntable = False
        self.__multiProcessingOvenLight = False
        self.__multiProcessingCompressor = False
        self.__multiProcessingValveVacuum = False
        self.__multiProcessingActLowerValve = False
        self.__multiProcessingValveOvenDoor = False
        self.__multiProcessingValveFeeder = False
        self.__isCommandSuccessed = False
        self.__isCommandRunning = False
        self.__isCommandTimedOut = False
        dictMap = {RequestedParameter.REFERENCESWITCHTURNTABLEPOSITOINVACUUM: self.__multiProcessingSensTurntablePosVacuum,
                   RequestedParameter.REFERENCESWITCHTURNTABLEPOSITIONBELT: self.__multiProcessingSensTurntablePosBelt,
                   RequestedParameter.LIGHTBARRIERENDOFCONVEYORBELT: self.__multiProcessingSensEndConveyor,
                   RequestedParameter.REFERENCESWITCHTURNTABLEPOSITIONSAW: self.__multiProcessingSensTurntablePosSaw,
                   RequestedParameter.REFERENCESWITCHVACUUMPOSITIONTURNTABLE: self.__multiProcessingSensVacuumGripperAtTurntable,
                   RequestedParameter.REFERENCESWITCHOVENFEEDERINSIDE: self.__multiProcessingSensOvenFeederIn,
                   RequestedParameter.REFERENCESWITCHOVENFEEDEROUTSIDE: self.__multiProcessingSensOvenFeederOut,
                   RequestedParameter.REFERENCESWITCHVACUUMPOSITIONOVEN: self.__multiProcessingSensVacuumGripperAtOven,
                   RequestedParameter.LIGHTBARRIEROVEN: self.__multiProcessingSensOven,
                   RequestedParameter.MOTORTURNTABLECLOCKWISE: self.__multiProcessingActRotClockwise,
                   RequestedParameter.MOTORROTATECOUNTERCLOCKWISE: self.__multiProcessingActRotCounterclockwise,
                   RequestedParameter.MOTORCONVEYORBELTFORWARD: self.__multiProcessingActConveyorForward,
                   RequestedParameter.MOTORSAW: self.__multiProcessingActSaw,
                   RequestedParameter.MOTOROVENFEEDERRETRACT: self.__multiProcessingSensOvenFeederIn,
                   RequestedParameter.MOTOROVENFEEDEREXTEND: self.__multiProcessingSensOvenFeederOut,
                   RequestedParameter.MOTORVACUUMTOWARDSOVEN: self.__multiProcessingActGripperToOven,
                   RequestedParameter.MOTORVACUUMTOWARDSTURNTABLE: self.__multiProcessingActGripperToTurntable,
                   RequestedParameter.LIGHTOVEN: self.__multiProcessingOvenLight,
                   RequestedParameter.VALVEVACUUM: self.__multiProcessingValveVacuum,
                   RequestedParameter.VALVELOWERING: self.__multiProcessingActLowerValve,
                   RequestedParameter.VALVEOVENDOOR: self.__multiProcessingValveOvenDoor,
                   RequestedParameter.VALVEFEEDER: self.__multiProcessingValveFeeder}
        super().__init__(id1, dictMap)
        self.setupFinished = False
        self.sawCount = 0
        self.ovenCount = 0
        self.vacuumCount = 0
        self.ejectorCount = 0
        self.actionDone = 0
        self.heated = False
        self.delivered = False
        self.processing = False
        self.turnTableDirection: Direction = Direction.NONE
        self.previous_isExecuting_log = None

    @property
    def multiProcessingSensTurntablePosVacuum(self) -> bool:
        return self.__multiProcessingSensTurntablePosVacuum

    @multiProcessingSensTurntablePosVacuum.setter
    def multiProcessingSensTurntablePosVacuum(self, value: bool):
        self.__multiProcessingSensTurntablePosVacuum = value

    @property
    def multiProcessingSensTurntablePosSaw(self) -> bool:
        return self.__multiProcessingSensTurntablePosSaw

    @multiProcessingSensTurntablePosSaw.setter
    def multiProcessingSensTurntablePosSaw(self, value: bool):
        self.__multiProcessingSensTurntablePosSaw = value

    @property
    def multiProcessingSensTurntablePosBelt(self) -> bool:
        return self.__multiProcessingSensTurntablePosBelt

    @multiProcessingSensTurntablePosBelt.setter
    def multiProcessingSensTurntablePosBelt(self, value: bool):
        self.__multiProcessingSensTurntablePosBelt = value

    @property
    def multiProcessingSensEndConveyor(self) -> bool:
        return self.__multiProcessingSensEndConveyor

    @multiProcessingSensEndConveyor.setter
    def multiProcessingSensEndConveyor(self, value: bool):
        self.__multiProcessingSensEndConveyor = value

    @property
    def multiProcessingSensOven(self) -> bool:
        return self.__multiProcessingSensOven

    @multiProcessingSensOven.setter
    def multiProcessingSensOven(self, value: bool):
        self.__multiProcessingSensOven = value

    @property
    def multiProcessingSensVacuumGripperAtOven(self) -> bool:
        return self.__multiProcessingSensVacuumGripperAtOven

    @multiProcessingSensVacuumGripperAtOven.setter
    def multiProcessingSensVacuumGripperAtOven(self, value: bool):
        self.__multiProcessingSensVacuumGripperAtOven = value

    @property
    def multiProcessingSensVacuumGripperAtTurntable(self) -> bool:
        return self.__multiProcessingSensVacuumGripperAtTurntable

    @multiProcessingSensVacuumGripperAtTurntable.setter
    def multiProcessingSensVacuumGripperAtTurntable(self, value: bool):
        self.__multiProcessingSensVacuumGripperAtTurntable = value

    @property
    def multiProcessingSensOvenFeederOut(self) -> bool:
        return self.__multiProcessingSensOvenFeederOut

    @multiProcessingSensOvenFeederOut.setter
    def multiProcessingSensOvenFeederOut(self, value: bool):
        self.__multiProcessingSensOvenFeederOut = value

    @property
    def multiProcessingSensOvenFeederIn(self) -> bool:
        return self.__multiProcessingSensOvenFeederIn

    @multiProcessingSensOvenFeederIn.setter
    def multiProcessingSensOvenFeederIn(self, value: bool):
        self.__multiProcessingSensOvenFeederIn = value

    @property
    def multiProcessingActRotClockwise(self) -> bool:
        return self.__multiProcessingActRotClockwise

    @multiProcessingActRotClockwise.setter
    def multiProcessingActRotClockwise(self, value: bool):
        self.__multiProcessingActRotClockwise = value

    @property
    def multiProcessingActRotCounterclockwise(self) -> bool:
        return self.__multiProcessingActRotCounterclockwise

    @multiProcessingActRotCounterclockwise.setter
    def multiProcessingActRotCounterclockwise(self, value: bool):
        self.__multiProcessingActRotCounterclockwise = value

    @property
    def multiProcessingActConveyorForward(self) -> bool:
        return self.__multiProcessingActConveyorForward

    @multiProcessingActConveyorForward.setter
    def multiProcessingActConveyorForward(self, value: bool):
        self.__multiProcessingActConveyorForward = value

    @property
    def multiProcessingActSaw(self) -> bool:
        return self.__multiProcessingActSaw

    @multiProcessingActSaw.setter
    def multiProcessingActSaw(self, value: bool):
        self.__multiProcessingActSaw = value

    @property
    def multiProcessingActOvenInward(self) -> bool:
        return self.__multiProcessingActOvenInward

    @multiProcessingActOvenInward.setter
    def multiProcessingActOvenInward(self, value: bool):
        self.__multiProcessingActOvenInward = value

    @property
    def multiProcessingActOvenOutward(self) -> bool:
        return self.__multiProcessingActOvenOutward

    @multiProcessingActOvenOutward.setter
    def multiProcessingActOvenOutward(self, value: bool):
        self.__multiProcessingActOvenOutward = value

    @property
    def multiProcessingActGripperToOven(self) -> bool:
        return self.__multiProcessingActGripperToOven

    @multiProcessingActGripperToOven.setter
    def multiProcessingActGripperToOven(self, value: bool):
        self.__multiProcessingActGripperToOven = value

    @property
    def multiProcessingActGripperToTurntable(self) -> bool:
        return self.__multiProcessingActGripperToTurntable

    @multiProcessingActGripperToTurntable.setter
    def multiProcessingActGripperToTurntable(self, value: bool):
        self.__multiProcessingActGripperToTurntable = value

    @property
    def multiProcessingOvenLight(self) -> bool:
        return self.__multiProcessingOvenLight

    @multiProcessingOvenLight.setter
    def multiProcessingOvenLight(self, value: bool):
        self.__multiProcessingOvenLight = value

    @property
    def multiProcessingCompressor(self) -> bool:
        return self.__multiProcessingCompressor

    @multiProcessingCompressor.setter
    def multiProcessingCompressor(self, value: bool):
        self.__multiProcessingCompressor = value

    @property
    def multiProcessingValveVacuum(self) -> bool:
        return self.__multiProcessingValveVacuum

    @multiProcessingValveVacuum.setter
    def multiProcessingValveVacuum(self, value: bool):
        self.__multiProcessingValveVacuum = value

    @property
    def multiProcessingActLowerValve(self) -> bool:
        return self.__multiProcessingActLowerValve

    @multiProcessingActLowerValve.setter
    def multiProcessingActLowerValve(self, value: bool):
        self.__multiProcessingActLowerValve = value

    @property
    def multiProcessingValveOvenDoor(self) -> bool:
        return self.__multiProcessingValveOvenDoor

    @multiProcessingValveOvenDoor.setter
    def multiProcessingValveOvenDoor(self, value: bool):
        self.__multiProcessingValveOvenDoor = value

    @property
    def multiProcessingValveFeeder(self) -> bool:
        return self.__multiProcessingValveFeeder

    @multiProcessingValveFeeder.setter
    def multiProcessingValveFeeder(self, value: bool):
        self.__multiProcessingValveFeeder = value


    def sensorStatusString(self) -> str:
        return f"TT[{self.multiProcessingSensTurntablePosVacuum}, {self.multiProcessingSensTurntablePosBelt}, {self.multiProcessingSensTurntablePosSaw}], " + \
            f"LB[{self.multiProcessingSensEndConveyor}, {self.multiProcessingSensOven}], " + \
            f"VG[{self.multiProcessingSensVacuumGripperAtTurntable}, {self.multiProcessingSensVacuumGripperAtOven}], " + \
            f"OF[{self.multiProcessingSensOvenFeederIn}, {self.multiProcessingSensOvenFeederOut}]" 

    def actuatorStatusString(self) -> str:
        return f"TT[{self.multiProcessingActRotClockwise}, {self.multiProcessingActRotCounterclockwise}], " + \
            f"{self.multiProcessingActConveyorForward}, {self.multiProcessingActSaw}, " + \
            f"O[{self.multiProcessingActOvenInward}, {self.multiProcessingActOvenOutward}], " + \
            f"VG[{self.__multiProcessingActGripperToOven}, {self.__multiProcessingActGripperToTurntable}], " + \
            f"{self.__multiProcessingOvenLight}, {self.__multiProcessingCompressor}, {self.__multiProcessingValveVacuum}, {self.__multiProcessingActLowerValve},{self.__multiProcessingValveOvenDoor}, {self.__multiProcessingValveFeeder}"
 
    def inputStatus(self) -> Dict[str, Any]:
        status = {
            "multiProcessingSensTurntablePosVacuum": self.multiProcessingSensTurntablePosVacuum,
            "multiProcessingSensTurntablePosBelt": self.multiProcessingSensTurntablePosBelt,
            "multiProcessingSensTurntablePosSaw": self.multiProcessingSensTurntablePosSaw,
            "multiProcessingSensEndConveyor": self.multiProcessingSensEndConveyor,
            "multiProcessingSensOven": self.multiProcessingSensOven,
            "multiProcessingSensVacuumGripperAtTurntable": self.multiProcessingSensVacuumGripperAtTurntable,
            "multiProcessingSensVacuumGripperAtOven": self.multiProcessingSensVacuumGripperAtOven,
            "multiProcessingSensOvenFeederIn": self.multiProcessingSensOvenFeederIn,
            "multiProcessingSensOvenFeederOut": self.multiProcessingSensOvenFeederOut,
        }
        return status

    def outputStatus(self) -> Dict[str, Any]:
        
        status = {
            "multiProcessingActRotClockwise": self.multiProcessingActRotClockwise,
            "multiProcessingActRotCounterclockwise": self.multiProcessingActRotCounterclockwise,
            "multiProcessingActConveyorForward": self.multiProcessingActConveyorForward,
            "multiProcessingActSaw": self.multiProcessingActSaw,
            "multiProcessingActOvenInward": self.multiProcessingActOvenInward,
            "multiProcessingActOvenOutward": self.multiProcessingActOvenOutward,
            "multiProcessingActGripperToOven": self.__multiProcessingActGripperToOven,
            "multiProcessingActGripperToTurntable": self.__multiProcessingActGripperToTurntable,
            "multiProcessingOvenLight": self.__multiProcessingOvenLight,
            "multiProcessingCompressor": self.__multiProcessingCompressor,
            "multiProcessingValveVacuum": self.__multiProcessingValveVacuum,
            "multiProcessingActLowerValve": self.__multiProcessingActLowerValve,
            "multiProcessingValveOvenDoor": self.__multiProcessingValveOvenDoor,
            "multiProcessingValveFeeder": self.__multiProcessingValveFeeder,
        }
        return status
    
    def internalStatus(self) -> Dict[str, Any]:
        status = {
            "isExecuting": self.isExecuting,
        }
        return status

    ###____________ Turntable and Saw_______________
    def moveTurntableToSaw(self):
        """Rotate the turntable to the saw"""
        if not self.__multiProcessingSensTurntablePosSaw:
            if self.turnTableDirection is Direction.NONE:
                #If it is the fisrt movement, get direction
                if self.__multiProcessingSensTurntablePosVacuum:
                    self.turnTableDirection = Direction.CLOCKWISE
                elif self.__multiProcessingSensTurntablePosBelt:
                    self.turnTableDirection = Direction.COUNTERCLOKWISE
                else:
                    logging.error("Could not determine position of turntable")
            if self.turnTableDirection is Direction.CLOCKWISE:
                self.__multiProcessingActRotClockwise = True
                self.__multiProcessingActRotCounterclockwise = False
            if self.turnTableDirection is Direction.COUNTERCLOKWISE:
                self.__multiProcessingActRotClockwise = False
                self.__multiProcessingActRotCounterclockwise = True
        else:
            self.turnTableDirection = Direction.NONE
            self.__multiProcessingActRotClockwise = False
            self.__multiProcessingActRotCounterclockwise = False
            self.actionDone += 1


    def moveTurntableToConveyor(self):
        """Rotate the turntable to conveyor"""
        if not self.__multiProcessingSensTurntablePosBelt:
            self.__multiProcessingActRotClockwise = True
            self.__multiProcessingActRotCounterclockwise = False
        else:
            self.__multiProcessingActRotClockwise = False
            self.actionDone += 1


    def moveTurntableToVacuum(self):
        """Rotate the turntable to the vacuum"""
        if not self.__multiProcessingSensTurntablePosVacuum:
            self.__multiProcessingActRotCounterclockwise = True
            self.__multiProcessingActRotClockwise = False
        else:
            self.__multiProcessingActRotCounterclockwise = False
            self.actionDone += 1


    def useSaw(self):
        """Use the saw on the package for a specific number of iteration who can be determined here with maxCount"""
        maxCount = 5
        if self.__multiProcessingSensTurntablePosSaw and self.sawCount < maxCount:
            self.__multiProcessingActSaw  = True
            self.sawCount += 1
        else:
            self.__multiProcessingActSaw  = False
            self.actionDone += 1


    def ejectProductToConveyor(self):
        """Eject the package from the turtable to the conveyor"""
        if not self.__multiProcessingSensTurntablePosBelt:
            self.moveTurntableToConveyor
        if self.ejectorCount > 1:
            self.__multiProcessingCompressor = False
            self.__multiProcessingValveFeeder = False               
            self.ejectorCount = 0
            self.actionDone += 1
        else :
            self.__multiProcessingCompressor = True
            self.__multiProcessingValveFeeder = True
            self.ejectorCount += 1


    ###____________ Conveyor belt ______________
    def moveConveyorToEnd(self):
        """Moves the package with the conveyor unitl it reached the light barrier"""
        if self.__multiProcessingSensEndConveyor:
            self.__multiProcessingActConveyorForward = True
        else :
            self.__multiProcessingActConveyorForward = False
            self.actionDone += 1


    ###____________ Oven _______________
    def heatProduct(self):
        """Simulate heating by blicking a led."""
        maxCount = 30
        if not self.heated:
            if not self.__multiProcessingSensOvenFeederIn:
                self.moveFeederIn
            self.ovenCount += 1

            if self.ovenCount % 2 == 1:
                self.__multiProcessingOvenLight = True
            else:
                self.__multiProcessingOvenLight = False
            
            if self.ovenCount > maxCount:
                self.__multiProcessingOvenLight = False
                self.ovenCount = 0
                self.heated = True
                self.moveFeederOut
                self.actionDone += 1


    def moveFeederIn(self):
        """Move the feeder inside the oven."""
        if not self.__multiProcessingSensOvenFeederIn:
            self.__multiProcessingCompressor = True
            self.__multiProcessingValveOvenDoor = True
            self.__multiProcessingActOvenInward = True
        else:
            self.__multiProcessingActOvenInward = False
            self.__multiProcessingCompressor = False
            self.__multiProcessingValveOvenDoor = False
            self.actionDone += 1


    def moveFeederOut(self):
        """Movs the feeder outside of the oven."""
        if not self.__multiProcessingSensOvenFeederOut:
            self.__multiProcessingCompressor = True
            self.__multiProcessingValveOvenDoor = True
            self.__multiProcessingActOvenOutward = True
        else:
            self.__multiProcessingActOvenOutward = False
            self.__multiProcessingCompressor = False
            self.__multiProcessingValveOvenDoor = False
            self.actionDone += 1

    ###____________ Vacuum gripper _______________
    def moveVacuumToOven(self):
        """Move the vacuum gripper to the oven."""
        if not self.__multiProcessingSensVacuumGripperAtOven:
            self.__multiProcessingActGripperToOven = True
        else:
            self.__multiProcessingActGripperToOven = False
            self.actionDone += 1


    def moveVacuumToTurntable(self):
        """Move the vacuum gripper to the turntable."""
        if not self.__multiProcessingSensVacuumGripperAtTurntable:
            self.__multiProcessingActGripperToTurntable = True
        else:
            self.__multiProcessingActGripperToTurntable = False
            self.actionDone += 1


    def gripProduct(self):
        """Takes the product with the vacuum gripper"""
        if self.__multiProcessingSensVacuumGripperAtTurntable:
            if not self.__multiProcessingSensTurntablePosVacuum:
                self.moveTurntableToVacuum
        elif self.__multiProcessingSensVacuumGripperAtOven:
            if not self.__multiProcessingSensOvenFeederOut:
                self.moveFeederOut
        else:
            logging.error("Vacuum gripper is not at a valid position")
            return
        
        if self.vacuumCount > 6:
            self.__multiProcessingActLowerValve = False
            self.__multiProcessingCompressor = False
            self.vacuumCount = 0
            self.actionDone += 1
        elif self.vacuumCount > 4:
            self.__multiProcessingValveVacuum = True
            self.vacuumCount += 1
        else:
            self.__multiProcessingCompressor = True
            self.__multiProcessingActLowerValve = True
            self.vacuumCount += 1


    def releaseProduct(self):
        """Release the product with the vacuum gripper"""
        if self.__multiProcessingSensVacuumGripperAtTurntable:
            if not self.__multiProcessingSensTurntablePosVacuum:
                self.moveTurntableToVacuum
        elif self.__multiProcessingSensVacuumGripperAtOven:
            if not self.__multiProcessingSensOvenFeederOut:
                self.moveFeederOut
        else:
            logging.error("Vacuum gripper is not at a valid position")
            return

        if self.vacuumCount > 5:
            self.__multiProcessingValveVacuum = False
            self.__multiProcessingActLowerValve = False
            self.__multiProcessingCompressor = False
            self.vacuumCount = 0
            self.actionDone += 1
        else:
            self.__multiProcessingCompressor = True
            self.__multiProcessingActLowerValve = True
            self.vacuumCount += 1


    ### __________ Other functions ______________
    def resetStation(self):
        """Set all valuables and the ovenReady flag to the starting values."""
        self.sawCount = 0
        self.ovenCount = 0
        self.vacuumCount = 0
        self.ejectorCount = 0
        self.toVac = False
        self.heated = False
        self.delivered = False
        self.actionDone +=1


    ### ____________ Functions intended to be called in the exLoop function of the RevPiPyMachineController ________________

    def setup_CycleStep(self) -> bool:
        """Reset the station and move some parts to the initial postion.
        :return: as a CycleStep, this function must return True when it is finished so it can be removed from the currentlyExecuting map"""
        actions = [
            self.resetStation,
            self.moveTurntableToVacuum,
            self.moveVacuumToOven,
            self.moveFeederOut,
        ]
        self.processing = True
        if len(actions) <= self.actionDone:
            self.processing = False
            self.actionDone = 0
            return True
        else:
            logging.debug(f"ACTION n° {self.actionDone} : {actions[self.actionDone]}")
            actions[self.actionDone]()
            return False

    def process_CycleStep(self, actions: List) -> bool:
        """call the actions in the actions list, each action must increment self.actionDone in order to proceed to next action of the list
        
        :return: as a CycleStep, this function must return True when it is finished so it can be removed from the currentlyExecuting map
        """
        self.processing = True
        if len(actions) <= self.actionDone:
            self.__isCommandSuccessed = True
            self.__isCommandRunning = False
            self.processing = False
            self.actionDone = 0
            return True
        else:
            self.__isCommandSuccessed = False
            self.__isCommandRunning = True
            logging.debug(f"ACTION n° {self.actionDone} : {actions[self.actionDone]}")
            actions[self.actionDone]()
            return False

    @override
    def stop_CycleStep(self) -> bool:
        self.processing = self.__multiProcessingActRotClockwise = self.__multiProcessingActRotCounterclockwise = self.__multiProcessingActConveyorForward = self.__multiProcessingActSaw = self.__multiProcessingActOvenInward = self.__multiProcessingActOvenOutward = self.__multiProcessingActGripperToOven = self.__multiProcessingActGripperToTurntable = self.__multiProcessingOvenLight = self.__multiProcessingCompressor = self.__multiProcessingValveVacuum = self._multiProcessingActLowerValve = self.__multiProcessingValveOvenDoor = self.__multiProcessingValveFeeder = False
        self.__isCommandSuccessed = True
        self.__isCommandRunning = False
        return True

    ### ____________ Functions callable from orchestrator ________________
    #   function name must be lowercase and finish with '_Command' postfix (cf. RevPiPyMachineController)
    def setup_Command(self) -> Optional[Callable[[], bool]]:
        return lambda: self.setup_CycleStep()  

    def process1_Command(self) -> Optional[Callable[[], bool]]:
        """Execute process 1 : The package is on the feeder at setup and will be delivered at the conveyor end """
        actions = [
            self.moveFeederIn,
            self.heatProduct,
            self.moveFeederOut,
            self.moveVacuumToOven,
            self.gripProduct,
            self.moveVacuumToTurntable,
            self.moveTurntableToVacuum,
            self.releaseProduct,
            self.moveTurntableToSaw,
            self.useSaw,
            self.moveTurntableToConveyor,
            self.ejectProductToConveyor,
            self.moveConveyorToEnd
        ]
        return lambda:self.process_CycleStep(actions)    
    
    def stop_Command(self):
        """ Stop the machine """
        return lambda:self.stop_CycleStep() 