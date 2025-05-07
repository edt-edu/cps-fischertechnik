import logging
from typing import Any, Callable, Dict

from typing_extensions import override

from rppmcontroller.behavior.CycleStepResult import CycleStepResult
from rppmcontroller.behavior.CycleStepResultEnum import CycleStepResultEnum
from rppmcontroller.machine.Direction import Direction
from rppmcontroller.machine.Machine import Machine
from rppmcontroller.machine.RequestedParameter import RequestedParameter
from rppmcontroller.machine.Runner import TransitioningMachine, Runner
from rppmcontroller.machine.TurnTableDirection import TurnTableDirection
from rppmcontroller.machine.multiprocessing.MultiProcessingConfig import \
    MultiProcessingConfig
from rppmcontroller.machine.multiprocessing.TurnTablePosition import \
    TurnTablePosition


class MultiProcessing(Machine, TransitioningMachine):
    """
    Class implementing the MultiProcessingStation Machine

    Attributes:
        __multiProcessingSensTurntablePosVacuum (bool) : Turn Table button sensor when the turn table is in front of the VacuumGripper,
            True when in front of Vacuum Gripper
        __multiProcessingSensTurntablePosSaw (bool) : Turn Table button sensor when the turn table is in front of the Saw,
            True when in front of Saw
        __multiProcessingSensTurntablePosBelt (bool) :  Turn Table button sensor when the turn table is in front of the conveyor belt,
            True when in front of Conveyor
        __multiProcessingSensEndConveyor (bool) : Light sensor on the conveyor.
            False when a token is detected
        __multiProcessingSensOven (bool) : Light sensor in front of the Oven.
            False when a token is detected
        __multiProcessingSensOvenFeederIn (bool) : Button sensor inside the oven, True when the feeder is fully retracted
        __multiProcessingSensOvenFeederOut (bool) : Button sensor outside the oven, True when the feeder is fully extended
        __multiProcessingSensVacuumGripperAtTurntable (bool) : Button sensor on the Vacuum gripper , True when the gripper is in front of the TurnTable
        __multiProcessingSensVacuumGripperAtOven (bool) : Button sensor on the Vacuum gripper , True when the gripper is in front of the Oven
        __multiProcessingActRotClockwise (bool) : Rotate TurnTable clockwise (ie. toward the converyor belt).
            must not be true at the same time as __multiProcessingActRotCounterclockwise
        __multiProcessingActRotCounterclockwise (bool) :  Rotate TurnTable counterclockwise (ie. toward the VacuumGRipper).
            must not be true at the same time as __multiProcessingActRotClockwise
        __multiProcessingActConveyorForward (bool) :
        __multiProcessingActSaw (bool) :
        __multiProcessingActOvenInward (bool) :
        __multiProcessingActOvenOutward (bool) :
        __multiProcessingActGripperToOven (bool) :
        __multiProcessingActGripperToTurntable (bool) :
        __multiProcessingOvenLight (bool) :
        __multiProcessingCompressor (bool) :
        __multiProcessingValveVacuum (bool) :
        __multiProcessingActLowerValve (bool) :
        __multiProcessingValveOvenDoor (bool) :
        __multiProcessingValveFeeder (bool) :
    """

    @property
    def isInitialized(self) -> bool:
        return True # technically always initialized, since there are no encoder actuators

    @Machine.isExecuting.getter
    def isExecuting(self) -> bool:
        res = (self.isProcessingSequence() or
               self.processing or
               self.__multiProcessingActRotClockwise or
               self.__multiProcessingActRotCounterclockwise or
               self.__multiProcessingActConveyorForward or
               self.__multiProcessingActSaw or
               self.__multiProcessingActOvenInward or
               self.__multiProcessingActOvenOutward or
               self.__multiProcessingActGripperToOven or
               self.__multiProcessingActGripperToTurntable or
               self.__multiProcessingOvenLight or
               self.__multiProcessingCompressor or
               self.__multiProcessingActLowerValve or
               self.__multiProcessingValveFeeder or
               self.is_executing_runner)
        # log isexecuting and debug info only if message has changed
        isExecuting_log = f'isExecuting({self.id})={res} | Sensors={self.sensorStatusString()} | Actuators= {self.actuatorStatusString()}'
        if isExecuting_log != self.previous_isExecuting_log :
            logging.debug(isExecuting_log)
            self.previous_isExecuting_log = isExecuting_log

        return res

    def __init__(self, id1):
        #  inputs
        self.__multiProcessingSensTurntablePosVacuum = False
        self.__multiProcessingSensTurntablePosBelt = False
        self.__multiProcessingSensTurntablePosSaw = False
        self.__multiProcessingSensEndConveyor = True
        self.__multiProcessingSensOven = True
        self.__multiProcessingSensVacuumGripperAtTurntable = False
        self.__multiProcessingSensVacuumGripperAtOven = False
        self.__multiProcessingSensOvenFeederIn = False
        self.__multiProcessingSensOvenFeederOut = False

        #  outputs
        self.__multiProcessingActRotClockwise = False
        self.__multiProcessingActRotCounterclockwise = False
        self.__multiProcessingActConveyorForward = False
        self.__multiProcessingActSaw = False
        self.__multiProcessingActOvenInward = False
        self.__multiProcessingActOvenOutward = False
        self.__multiProcessingActGripperToOven = False
        self.__multiProcessingActGripperToTurntable = False
        self.__multiProcessingOvenLight = False
        self.__multiProcessingCompressor = False
        self.__multiProcessingValveVacuum = False
        self.__multiProcessingActLowerValve = False
        self.__multiProcessingValveOvenDoor = False
        self.__multiProcessingValveFeeder = False
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
        Machine.__init__(self, id1, dictMap)
        TransitioningMachine.__init__(self)

        # helper variables
        self.sawCount = 0
        self.ovenCount = 0
        self.vacuumCount = 0
        self.ejectorCount = 0
        self.heated = False
        self.delivered = False
        self.processing = False
        self.turnTableDirection: Direction = Direction.NONE
        self.previous_isExecuting_log = None
        self.__last_target_config = None
        self.__turn_table_direction = TurnTableDirection.NONE

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

    @property
    def turn_table_direction(self) -> TurnTableDirection:
        return self.__turn_table_direction

    @turn_table_direction.setter
    def turn_table_direction(self, turn_table_direction: TurnTableDirection) -> None:
        self.__turn_table_direction = turn_table_direction
        if turn_table_direction is TurnTableDirection.NONE:
            self.multiProcessingActRotClockwise = False
            self.multiProcessingActRotCounterclockwise = False
        elif turn_table_direction is TurnTableDirection.CLOCKWISE:
            self.multiProcessingActRotClockwise = True
            self.multiProcessingActRotCounterclockwise = False
        elif turn_table_direction is TurnTableDirection.COUNTER_CLOCKWISE:
            self.multiProcessingActRotClockwise = False
            self.multiProcessingActRotCounterclockwise = True

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

    @override
    def goto_config(self, config: MultiProcessingConfig = MultiProcessingConfig()) -> CycleStepResult:
        """
        Transfer the machine into another configuration
        :param config: The new configuration to transfer the machine to
        :return: A CycleStepResult
        """

        res = CycleStepResult(CycleStepResultEnum.DONE)

        # conveyor
        self.multiProcessingActConveyorForward = config.conveyor_active
        # saw
        self.multiProcessingActSaw = config.saw_active
        # valve
        self.multiProcessingValveVacuum = config.vacuum_valve_active
        # oven light
        self.multiProcessingOvenLight = config.oven_lamp_on

        # oven door; keep open if feeder is moving
        oven_feeder_moving = not (self.multiProcessingSensOvenFeederIn or self.multiProcessingSensOvenFeederOut)
        self.multiProcessingValveOvenDoor = config.oven_door_open or oven_feeder_moving

        # conveyor feeder; only activate if tt is at belt
        self.multiProcessingValveFeeder = config.conveyor_feeder_active and self.multiProcessingSensTurntablePosBelt

        # vacuum arm lowered
        arm_idle = self.multiProcessingSensVacuumGripperAtOven or self.__multiProcessingSensVacuumGripperAtTurntable
        self.multiProcessingActLowerValve = config.vacuum_arm_lowered and arm_idle

        # vacuum arm
        self.multiProcessingActGripperToOven = False
        self.multiProcessingActGripperToTurntable = False
        if config.vacuum_arm_at_oven and not self.multiProcessingSensVacuumGripperAtOven:
            self.multiProcessingActGripperToOven = True
            res = CycleStepResult(CycleStepResultEnum.MUST_CONTINUE, "moving arm to oven")
        elif not config.vacuum_arm_at_oven and not self.multiProcessingSensVacuumGripperAtTurntable:
            self.multiProcessingActGripperToTurntable = True
            res = CycleStepResult(CycleStepResultEnum.MUST_CONTINUE, "moving arm to turn table")

        # oven feeder
        self.multiProcessingActOvenInward = False
        self.multiProcessingActOvenOutward = False
        if config.oven_feeder_expanded and not self.multiProcessingSensOvenFeederOut:
            self.multiProcessingActOvenOutward = True
            res = CycleStepResult(CycleStepResultEnum.MUST_CONTINUE, "moving oven feeder out")
        elif not config.oven_feeder_expanded and not self.multiProcessingSensOvenFeederIn:
            self.multiProcessingActOvenInward = True
            res = CycleStepResult(CycleStepResultEnum.MUST_CONTINUE, "moving oven feeder in")

        # turn table
        next_tt_dir = TurnTableDirection.NONE
        tt_pos = config.turn_table_position
        if tt_pos is TurnTablePosition.VACUUM and not self.multiProcessingSensTurntablePosVacuum:
            next_tt_dir = TurnTableDirection.COUNTER_CLOCKWISE
        elif tt_pos is TurnTablePosition.SAW and not self.multiProcessingSensTurntablePosSaw:
            if self.multiProcessingSensTurntablePosVacuum:
                next_tt_dir = TurnTableDirection.CLOCKWISE
            elif self.multiProcessingSensTurntablePosBelt:
                next_tt_dir = TurnTableDirection.COUNTER_CLOCKWISE
            elif self.turn_table_direction is not TurnTableDirection.NONE:
                next_tt_dir = self.turn_table_direction
            else:
                next_tt_dir = TurnTableDirection.COUNTER_CLOCKWISE
        elif tt_pos is TurnTablePosition.CONVEYOR and not self.multiProcessingSensTurntablePosBelt:
            next_tt_dir = TurnTableDirection.CLOCKWISE

        if next_tt_dir is not TurnTableDirection.NONE:
            res = CycleStepResult(CycleStepResultEnum.MUST_CONTINUE, f"moving turn table to {tt_pos}")
        self.turn_table_direction = next_tt_dir

        # compressor; active if any valve is active
        self.multiProcessingCompressor = (
                self.multiProcessingValveVacuum or
                self.multiProcessingValveOvenDoor or
                self.multiProcessingActLowerValve or
                self.multiProcessingValveFeeder)

        return res

    ###____________ Turntable and Saw_______________
    def moveTurntableToSaw(self) -> CycleStepResult:
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
            return CycleStepResult(CycleStepResultEnum.MUST_CONTINUE)
        else:
            self.turnTableDirection = Direction.NONE
            self.__multiProcessingActRotClockwise = False
            self.__multiProcessingActRotCounterclockwise = False
            return CycleStepResult(CycleStepResultEnum.DONE)


    def moveTurntableToConveyor(self) -> CycleStepResult:
        """Rotate the turntable to conveyor"""
        if not self.__multiProcessingSensTurntablePosBelt:
            self.__multiProcessingActRotClockwise = True
            self.__multiProcessingActRotCounterclockwise = False
            return CycleStepResult(CycleStepResultEnum.MUST_CONTINUE)
        else:
            self.__multiProcessingActRotClockwise = False
            return CycleStepResult(CycleStepResultEnum.DONE)


    def moveTurntableToVacuum(self) -> CycleStepResult:
        """Rotate the turntable to the vacuum"""
        if not self.__multiProcessingSensTurntablePosVacuum:
            self.__multiProcessingActRotCounterclockwise = True
            self.__multiProcessingActRotClockwise = False
            return CycleStepResult(CycleStepResultEnum.MUST_CONTINUE)
        else:
            self.__multiProcessingActRotCounterclockwise = False
            return CycleStepResult(CycleStepResultEnum.DONE)


    def useSaw(self) -> CycleStepResult:
        """Use the saw on the package for a specific number of iteration who can be determined here with maxCount"""
        maxCount = 5
        if self.__multiProcessingSensTurntablePosSaw and self.sawCount < maxCount:
            self.__multiProcessingActSaw  = True
            self.sawCount += 1
            return CycleStepResult(CycleStepResultEnum.MUST_CONTINUE)
        else:
            self.__multiProcessingActSaw  = False
            return CycleStepResult(CycleStepResultEnum.DONE)


    def ejectProductToConveyor(self) -> CycleStepResult:
        """Eject the package from the turtable to the conveyor"""
        if not self.__multiProcessingSensTurntablePosBelt:
            self.moveTurntableToConveyor()
        if self.ejectorCount > 1:
            self.__multiProcessingCompressor = False
            self.__multiProcessingValveFeeder = False
            self.ejectorCount = 0
            return CycleStepResult(CycleStepResultEnum.DONE)
        else :
            self.__multiProcessingCompressor = True
            self.__multiProcessingValveFeeder = True
            self.ejectorCount += 1
            return CycleStepResult(CycleStepResultEnum.MUST_CONTINUE)


    ###____________ Conveyor belt ______________
    def moveConveyorToEnd(self) -> CycleStepResult:
        """Moves the package with the conveyor unitl it reached the light barrier"""
        if self.__multiProcessingSensEndConveyor:
            self.__multiProcessingActConveyorForward = True
            return CycleStepResult(CycleStepResultEnum.MUST_CONTINUE)
        else :
            self.__multiProcessingActConveyorForward = False
            return CycleStepResult(CycleStepResultEnum.DONE)


    ###____________ Oven _______________
    def heatProduct(self) -> CycleStepResult:
        """Simulate heating by blicking a led."""
        maxCount = 30
        if not self.heated:
            if not self.__multiProcessingSensOvenFeederIn:
                self.moveFeederIn()
            self.ovenCount += 1

            if self.ovenCount % 2 == 1:
                self.__multiProcessingOvenLight = True
            else:
                self.__multiProcessingOvenLight = False

            if self.ovenCount > maxCount:
                self.__multiProcessingOvenLight = False
                self.ovenCount = 0
                self.heated = True
                self.moveFeederOut()
                return CycleStepResult(CycleStepResultEnum.DONE)
        return CycleStepResult(CycleStepResultEnum.MUST_CONTINUE)


    def moveFeederIn(self) -> CycleStepResult:
        """Move the feeder inside the oven."""
        if not self.__multiProcessingSensOvenFeederIn:
            self.__multiProcessingCompressor = True
            self.__multiProcessingValveOvenDoor = True
            self.__multiProcessingActOvenInward = True
            return CycleStepResult(CycleStepResultEnum.MUST_CONTINUE)
        else:
            self.__multiProcessingActOvenInward = False
            self.__multiProcessingCompressor = False
            self.__multiProcessingValveOvenDoor = False
            return CycleStepResult(CycleStepResultEnum.DONE)


    def moveFeederOut(self) -> CycleStepResult:
        """Movs the feeder outside of the oven."""
        if not self.__multiProcessingSensOvenFeederOut:
            self.__multiProcessingCompressor = True
            self.__multiProcessingValveOvenDoor = True
            self.__multiProcessingActOvenOutward = True
            return CycleStepResult(CycleStepResultEnum.MUST_CONTINUE)
        else:
            self.__multiProcessingActOvenOutward = False
            self.__multiProcessingCompressor = False
            self.__multiProcessingValveOvenDoor = False
            return CycleStepResult(CycleStepResultEnum.DONE)

    ###____________ Vacuum gripper _______________
    def moveVacuumToOven(self) -> CycleStepResult:
        """Move the vacuum gripper to the oven."""
        if not self.__multiProcessingSensVacuumGripperAtOven:
            self.__multiProcessingActGripperToOven = True
            return CycleStepResult(CycleStepResultEnum.MUST_CONTINUE)
        else:
            self.__multiProcessingActGripperToOven = False
            return CycleStepResult(CycleStepResultEnum.DONE)


    def moveVacuumToTurntable(self) -> CycleStepResult:
        """Move the vacuum gripper to the turntable."""
        if not self.__multiProcessingSensVacuumGripperAtTurntable:
            self.__multiProcessingActGripperToTurntable = True
            return CycleStepResult(CycleStepResultEnum.MUST_CONTINUE)
        else:
            self.__multiProcessingActGripperToTurntable = False
            return CycleStepResult(CycleStepResultEnum.DONE)


    def gripProduct(self) -> CycleStepResult:
        """Takes the product with the vacuum gripper"""
        if self.__multiProcessingSensVacuumGripperAtTurntable:
            if not self.__multiProcessingSensTurntablePosVacuum:
                self.moveTurntableToVacuum()
        elif self.__multiProcessingSensVacuumGripperAtOven:
            if not self.__multiProcessingSensOvenFeederOut:
                self.moveFeederOut()
        else:
            logging.error("Vacuum gripper is not at a valid position")
            return CycleStepResult(CycleStepResultEnum.ABORTED_ERROR, "Vacuum gripper is not at a valid position")
        if self.vacuumCount > 6:
            self.__multiProcessingActLowerValve = False
            self.__multiProcessingCompressor = False
            self.vacuumCount = 0
            return CycleStepResult(CycleStepResultEnum.DONE)
        elif self.vacuumCount > 4:
            self.__multiProcessingValveVacuum = True
            self.vacuumCount += 1
            return CycleStepResult(CycleStepResultEnum.MUST_CONTINUE)
        else:
            self.__multiProcessingCompressor = True
            self.__multiProcessingActLowerValve = True
            self.vacuumCount += 1
            return CycleStepResult(CycleStepResultEnum.MUST_CONTINUE)


    def releaseProduct(self) -> CycleStepResult:
        """Release the product with the vacuum gripper"""
        if self.__multiProcessingSensVacuumGripperAtTurntable:
            if not self.__multiProcessingSensTurntablePosVacuum:
                self.moveTurntableToVacuum()
        elif self.__multiProcessingSensVacuumGripperAtOven:
            if not self.__multiProcessingSensOvenFeederOut:
                self.moveFeederOut()
        else:
            logging.error("Vacuum gripper is not at a valid position")
            return CycleStepResult(CycleStepResultEnum.ABORTED_ERROR, "Vacuum gripper is not at a valid position")

        if self.vacuumCount > 5:
            self.__multiProcessingValveVacuum = False
            self.__multiProcessingActLowerValve = False
            self.__multiProcessingCompressor = False
            self.vacuumCount = 0
            return CycleStepResult(CycleStepResultEnum.DONE)
        else:
            self.__multiProcessingCompressor = True
            self.__multiProcessingActLowerValve = True
            self.vacuumCount += 1
            return CycleStepResult(CycleStepResultEnum.MUST_CONTINUE)


    ### __________ Other functions ______________
    def resetStation(self) -> CycleStepResult:
        """Set all valuables and the ovenReady flag to the starting values."""
        self.sawCount = 0
        self.ovenCount = 0
        self.vacuumCount = 0
        self.ejectorCount = 0
        self.toVac = False
        self.heated = False
        self.delivered = False
        return CycleStepResult(CycleStepResultEnum.DONE)


    ### ____________ Functions intended to be called in the exLoop function of the RevPiPyMachineController ________________

    @override
    def stop_CycleStep(self) -> CycleStepResult:
        self.processing = False
        self.__multiProcessingActRotClockwise = False
        self.__multiProcessingActRotCounterclockwise = False
        self.__multiProcessingActConveyorForward = False
        self.__multiProcessingActSaw = False
        self.__multiProcessingActOvenInward = False
        self.__multiProcessingActOvenOutward = False
        self.__multiProcessingActGripperToOven = False
        self.__multiProcessingActGripperToTurntable = False
        self.__multiProcessingOvenLight = False
        self.__multiProcessingCompressor = False
        self.__multiProcessingValveVacuum = False
        self.__multiProcessingActLowerValve = False
        self.__multiProcessingValveOvenDoor = False
        self.__multiProcessingValveFeeder = False
        return CycleStepResult(CycleStepResultEnum.DONE)

    ### ____________ Functions callable from orchestrator ________________
    #   function name must be lowercase and finish with '_Command' postfix (cf. RevPiPyMachineController)

    def setup_Command(self) -> Callable[[], CycleStepResult]:
        """Reset the station and move some parts to the initial postion."""
        return self.goto_config

    def process1_Command(self) -> Runner:
        """
        Execute process 1 : The package is on the feeder at setup and will be delivered at the conveyor end
        """

        runner = self.create_runner()
        config = MultiProcessingConfig(turn_table_position=TurnTablePosition.from_actuators(
            self.multiProcessingSensTurntablePosVacuum,
            self.multiProcessingSensTurntablePosSaw,
            self.multiProcessingSensTurntablePosBelt))

        # wait until payload is present
        runner.then_goto(config,
                         until=lambda: not self.multiProcessingSensOven, and_stay_for=0.5, info="waiting for payload")

        # open oven door
        config.oven_door_open = True
        runner.then_goto(config, and_stay_for=0.2, info="open oven door")

        # move payload into oven
        config.oven_feeder_expanded = False
        runner.then_goto(config, info="move payload into oven")

        # activate oven for a few seconds
        config.oven_door_open = False
        config.oven_lamp_on = True
        runner.then_goto(config, and_stay_for=2.0, info="heat payload")

        # deactivate oven
        config.oven_lamp_on = False
        config.oven_feeder_expanded = True
        config.vacuum_arm_at_oven = True
        config.turn_table_position = TurnTablePosition.VACUUM
        runner.then_goto(config, info="move out of oven")

        # lower arm
        config.vacuum_arm_lowered = True
        runner.then_goto(config, and_stay_for=0.5, info="lower arm")

        # pickup
        config.vacuum_valve_active = True
        runner.then_goto(config, and_stay_for=0.5, info="pickup payload")

        # raise arm
        config.vacuum_arm_lowered = False
        runner.then_goto(config, and_stay_for=0.5, info="raise arm")

        # move payload to turn table
        config.vacuum_arm_at_oven = False
        runner.then_goto(config, info="go to turn table")

        # drop of payload carefully
        config.vacuum_valve_active = False
        runner.then_goto(config, and_stay_for=1.0, info="drop of payload")

        # turn to saw
        config.turn_table_position = TurnTablePosition.SAW
        runner.then_goto(config, info="turn to saw")

        # saw for a few seconds
        config.saw_active = True
        runner.then_goto(config, and_stay_for=2.0, info="saw payload")

        # drop at conveyor
        config.saw_active = False
        config.turn_table_position = TurnTablePosition.CONVEYOR
        config.conveyor_active = True
        config.conveyor_feeder_active = True  # will only activate once turntable has turned
        runner.then_goto(config, until=lambda: not self.multiProcessingSensEndConveyor, info="eject payload")

        # stop conveyor and feeder
        config.conveyor_active = False
        config.conveyor_feeder_active = False
        runner.then_goto(config, info="stopping feeder and conveyor")

        return runner.run()

    def stop_Command(self) -> Callable[[], CycleStepResult]:
        """ Stop the machine """
        return self.stop_CycleStep
