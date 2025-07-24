import logging
from typing import Any, Callable, Dict, Union

from typing_extensions import deprecated, override

from rppmcontroller.behavior.CycleStepResult import CycleStepResult
from rppmcontroller.behavior.CycleStepResultEnum import CycleStepResultEnum
from rppmcontroller.machine.Direction import Direction
from rppmcontroller.machine.Machine import Machine
from rppmcontroller.machine.MPSOutput import MPSOutput
from rppmcontroller.machine.RequestedParameter import RequestedParameter
from rppmcontroller.machine.Runner import TransitioningMachine, Runner
from rppmcontroller.machine.TurnTableDirection import TurnTableDirection
from rppmcontroller.machine.multiprocessing.MultiProcessingConfig import MultiProcessingConfig
from rppmcontroller.machine.multiprocessing.TurnTablePosition import TurnTablePosition
from rppmcontroller.protocol.function_decorators import protocol_command_function


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
        
        if (self.executing_runner == None):
            routine = "None"
        else:
            routine = str(self.executing_runner)
        
        # log isexecuting and debug info only if message has changed
        isExecuting_log = f'\n\tisExecuting({self.id})={res}\n\tRoutine : {routine}\n\tSensors={self.sensorStatusString()}\n\tActuators= {self.actuatorStatusString()}'
        if isExecuting_log != self.previous_isExecuting_log :
            logging.debug(isExecuting_log)
            self.previous_isExecuting_log = isExecuting_log

        return res

    def __init__(self, id1, safetyPos : dict = {}):
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
        dictMap = {RequestedParameter.REFERENCESWITCHTURNTABLEPOSITIONVACUUM: self.__multiProcessingSensTurntablePosVacuum,
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

        # safety position
        self.safeToOven = safetyPos.get('toOven', None)

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
        s = lambda bool: "T" if bool else "F"

        return f"TurnTab[{s(self.multiProcessingSensTurntablePosVacuum)}, {s(self.multiProcessingSensTurntablePosBelt)}, {s(self.multiProcessingSensTurntablePosSaw)}], " + \
               f"Sensors[{s(self.multiProcessingSensEndConveyor)}, {s(self.multiProcessingSensOven)}], " + \
               f"VacGrip[{s(self.multiProcessingSensVacuumGripperAtTurntable)}, {s(self.multiProcessingSensVacuumGripperAtOven)}], " + \
               f"OvenFeed[{s(self.multiProcessingSensOvenFeederIn)}, {s(self.multiProcessingSensOvenFeederOut)}]"

    def actuatorStatusString(self) -> str:
        s = lambda bool: "T" if bool else "F"

        return f"TurnTab[{s(self.multiProcessingActRotClockwise)}, {s(self.multiProcessingActRotCounterclockwise)}], " + \
               f"ConvSaw[{s(self.multiProcessingActConveyorForward)}, {s(self.multiProcessingActSaw)}], " + \
               f"Oven[{s(self.multiProcessingActOvenInward)}, {s(self.multiProcessingActOvenOutward)}, {s(self.__multiProcessingOvenLight)}], " + \
               f"VacGrip[{s(self.__multiProcessingActGripperToOven)}, {s(self.__multiProcessingActGripperToTurntable)}], " + \
               f"CompValv[{s(self.__multiProcessingCompressor)}, {s(self.__multiProcessingValveVacuum)}, {s(self.__multiProcessingActLowerValve)}, {s(self.__multiProcessingValveOvenDoor)}, {s(self.__multiProcessingValveFeeder)}]"

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
        :param config: The new configuration to transfer the machine to, the default config is the reference config (ie. setup)
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
    
    def get_current_config(self) -> MultiProcessingConfig:
        """
        Get the config describing the state in which the machine currently
        resides
        :return: The current config
        """
        return MultiProcessingConfig(
                turn_table_position=TurnTablePosition.from_actuators(
                    self.multiProcessingSensTurntablePosVacuum,
                    self.multiProcessingSensTurntablePosSaw,
                    self.multiProcessingSensTurntablePosBelt),
                saw_active=self.multiProcessingActSaw,
                conveyor_feeder_active=self.multiProcessingValveFeeder,
                conveyor_active=self.multiProcessingActConveyorForward,
                oven_feeder_expanded=not self.multiProcessingActOvenOutward,
                oven_lamp_on=self.multiProcessingOvenLight,
                oven_door_open=self.multiProcessingValveOvenDoor,
                vacuum_arm_at_oven=self.multiProcessingSensVacuumGripperAtOven,
                vacuum_arm_lowered=self.multiProcessingActLowerValve,
                vacuum_valve_active=self.multiProcessingValveVacuum
                )


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
    
    def heat_in_oven_CycleStep(self, time: int, config: MultiProcessingConfig, runner: Runner) -> None:
        """
        Move payload into oven, heat for time secs, and then get the payload out.
        :return: A Runner performing the heating
        """
        config.oven_door_open = True
        runner.then_goto(config, and_stay_for=0.2, info="open oven door")

        config.oven_feeder_expanded = False
        runner.then_goto(config, info="move payload into oven")

        config.oven_door_open = False
        config.oven_lamp_on = True
        runner.then_goto(config, and_stay_for=time, info="heat payload")

        config.oven_lamp_on = False
        config.oven_feeder_expanded = True
        runner.then_goto(config, info="move out of oven")
        
        return
    
    def saw_on_turntable_CycleStep(self, time: int, config: MultiProcessingConfig, runner: Runner) -> None:
        """
        Saw for time secs.
        :return: A Runner performing the sawing
        """
        config.saw_active = True
        runner.then_goto(config, and_stay_for=time, info="saw payload")

        config.saw_active = False
        runner.then_goto(config, info="sSawing finished")
        
        return
    
    def arm_to_oven_CycleStep(self, config: MultiProcessingConfig, runner: Runner) -> None:
        """
        Move the vacuum arm to oven
        :return: A Runner moving the arm to the oven
        """
        config.vacuum_arm_at_oven = True
        runner.then_goto(config, info="go to oven")

        return
    
    def arm_to_turntable_CycleStep(self, config: MultiProcessingConfig, runner: Runner) -> None:
        """
        Move the vacuum arm to turntable
        :return: A Runner moving the arm to the turntable
        """
        config.vacuum_arm_at_oven = False
        runner.then_goto(config, info="go to turntable")

        return
    
    def pick_up_CycleStep(self, config: MultiProcessingConfig, runner: Runner) -> None:
        """
        Make the vacuum arm pick the payload
        :return: A Runner picking the payload
        """
        config.vacuum_arm_lowered = True
        runner.then_goto(config, and_stay_for=0.5, info="lower arm")

        config.vacuum_valve_active = True
        runner.then_goto(config, and_stay_for=0.5, info="pickup payload")

        config.vacuum_arm_lowered = False
        runner.then_goto(config, and_stay_for=0.5, info="raise arm")

        return
    
    def place_CycleStep(self, config: MultiProcessingConfig, runner: Runner) -> None:
        """
        Make the vacuum arm place the payload
        :return: A Runner placing the payload
        """
        config.vacuum_arm_lowered = True
        runner.then_goto(config, and_stay_for=0.5, info="lower arm")

        config.vacuum_valve_active = False
        runner.then_goto(config, and_stay_for=0.5, info="placing payload")

        config.vacuum_arm_lowered = False
        runner.then_goto(config, and_stay_for=0.5, info="raise arm")

        return
    
    def go_to_conveyor_CycleStep(self, config: MultiProcessingConfig, runner: Runner) -> None:
        """
        Move the turntable to the conveyor
        :return: A Runner moving the turntable to the conveyor
        """
        config.turn_table_position = TurnTablePosition.CONVEYOR
        runner.then_goto(config, info="turn to conveyor")

        return
    
    def go_to_arm_CycleStep(self, config: MultiProcessingConfig, runner: Runner) -> None:
        """
        Move the turntable to the vacuum arm
        :return: A Runner moving the turntable to the vacuum arm
        """
        config.turn_table_position = TurnTablePosition.VACUUM
        runner.then_goto(config, info="turn to vacuum arm")

        return
    
    def go_to_saw_CycleStep(self, config: MultiProcessingConfig, runner: Runner) -> None:
        """
        Move the turntable to the saw
        :return: A Runner moving the turntable to the saw
        """
        config.turn_table_position = TurnTablePosition.SAW
        runner.then_goto(config, info="turn to saw")

        return
    
    def eject_from_turntable_CycleStep(self, config: MultiProcessingConfig, runner: Runner) -> None:
        """
        Eject the payload from the turntable on the conveyor
        :return: A Runner ejecting the payload
        """
        config.turn_table_position = TurnTablePosition.CONVEYOR
        config.conveyor_active = True
        config.conveyor_feeder_active = True
        runner.then_goto(config, until=lambda: not self.multiProcessingSensEndConveyor, and_stay_for=0.1, info="eject payload")

        config.conveyor_active = False
        config.conveyor_feeder_active = False
        runner.then_goto(config, info="stopping feeder and conveyor")

        return

    ### ____________ Functions callable from orchestrator ________________
    #   function name must be lowercase and finish with '_Command' postfix (cf. RevPiPyMachineController)

    @protocol_command_function(description="Move the engines to a reference point.")
    def setup_Command(self) -> Callable[[], CycleStepResult]:
        """Reset the station and move some parts to the initial postion."""
        return self.goto_config
    
    @protocol_command_function(description="Move payload into oven, heat for time secs, and then get the payload out.")
    def heat_in_oven_Command(self, time: int) -> Runner:
        """
        Move payload into oven, heat for time secs, and then get the payload out.

        :return: A Runner performing the heating
        """
        runner = self.create_runner()
        config = self.get_current_config()

        self.heat_in_oven_CycleStep(time, config, runner)

        return runner.run()
    
    @protocol_command_function(description="Saw for time secs.")
    def saw_on_turntable_Command(self, time: int) -> Runner:
        """
        Saw for time secs.

        :return: A Runner performing the sawing
        """
        runner = self.create_runner()
        config = self.get_current_config()
        
        self.saw_on_turntable_CycleStep(time, config, runner)
        return runner.run()
    
    @protocol_command_function(description="Move the vacuum arm in front of the oven.")
    def arm_to_oven_Command(self) -> Runner:
        """
        Move the vacuum arm in front of the oven.

        :return: A Runner moving the arm to the oven
        """
        runner = self.create_runner()
        config = self.get_current_config()
        
        self.arm_to_oven_CycleStep(config, runner)
        return runner.run()
    
    @protocol_command_function(description="Move the vacuum arm in front of the turntable.")
    def arm_to_turntable_Command(self) -> Runner:
        """
        Move the vacuum arm in front of the turntable

        :return: A Runner moving the arm to the turntable
        """
        runner = self.create_runner()
        config = self.get_current_config()
        
        self.arm_to_turntable_CycleStep(config, runner)

        return runner.run()
    
    @protocol_command_function(description="Make the vacuum arm pick the payload.")
    def pick_up_Command(self) -> Runner:
        """
        Make the vacuum arm pick the payload
        
        :return: A Runner picking the payload
        """
        runner = self.create_runner()
        config = self.get_current_config()
        
        self.pick_up_CycleStep(config, runner)
        return runner.run()
    
    @protocol_command_function(description="Make the vacuum arm place the payload.")
    def place_Command(self) -> Runner:
        """
        Make the vacuum arm place the payload
        
        :return: A Runner placing the payload
        """
        runner = self.create_runner()
        config = self.get_current_config()
        
        self.place_CycleStep(config, runner)
        return runner.run()
    
    @protocol_command_function(description="Move the turntable in front of the conveyor.")
    def go_to_conveyor_Command(self) -> Runner:
        """
        Move the turntable to the conveyor

        :return: A Runner moving the turntable to the conveyor
        """
        runner = self.create_runner()
        config = self.get_current_config()
        
        self.go_to_conveyor_CycleStep(config, runner)
        return runner.run()
    
    @protocol_command_function(description="Move the turntable in front of the vacuum arm.")
    def go_to_arm_Command(self) -> Runner:
        """
        Move the turntable to the vacuum arm
        
        :return: A Runner moving the turntable to the vacuum arm
        """
        runner = self.create_runner()
        config = self.get_current_config()
        
        
        self.go_to_arm_CycleStep(config, runner)
        return runner.run()
    
    @protocol_command_function(description="Move the turntable in front of the saw.")
    def go_to_saw_Command(self) -> Runner:
        """
        Move the turntable to the saw
        
        :return: A Runner moving the turntable to the saw
        """
        runner = self.create_runner()
        config = self.get_current_config()
        
        self.go_to_saw_CycleStep(config, runner)
        return runner.run()

    @protocol_command_function(description="Eject the payload from the turntable on the conveyor.")
    def eject_from_turntable_Command(self) -> Runner:
        """
        Eject the payload from the turntable on the conveyor
        
        :return: A Runner ejecting the payload
        """
        runner = self.create_runner()
        config = self.get_current_config()
        
        self.eject_from_turntable_CycleStep(config, runner)
        return runner.run()
    
    @protocol_command_function(description="Move payload from turntable to oven.")
    def move_to_oven_Command(self) -> Runner:
        """
        Move payload from turntable to oven
        
        :return: A Runner moving the payload
        """
        runner = self.create_runner()
        config = self.get_current_config()

        self.arm_to_turntable_CycleStep(config, runner)
        self.pick_up_CycleStep(config, runner)
        self.arm_to_oven_CycleStep(config, runner)
        self.place_CycleStep(config, runner)

        return runner.run()

    @deprecated("command not very clear, preferred command: process_Command  with arguments")
    @protocol_command_function(description="")
    def process1_Command(self) -> Runner:
        """
        Execute process 1 : The package is on the feeder at setup and will be delivered at the conveyor end
        """

        runner = self.create_runner()
        config = self.get_current_config()

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

        # lower arm
        config.vacuum_arm_lowered = True
        runner.then_goto(config, and_stay_for=0.5, info="lower arm")

        # drop of payload carefully
        config.vacuum_valve_active = False
        runner.then_goto(config, and_stay_for=1.0, info="drop of payload")

        # raise arm
        config.vacuum_arm_lowered = False
        runner.then_goto(config, and_stay_for=0.5, info="raise arm")

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
        runner.then_goto(config, until=lambda: not self.multiProcessingSensEndConveyor, and_stay_for=0.1, info="eject payload")

        # stop conveyor and feeder
        config.conveyor_active = False
        config.conveyor_feeder_active = False
        runner.then_goto(config, info="stopping feeder and conveyor")

        return runner.run()

    
    @protocol_command_function(description="Command stopping all engines (incl. compressor).")
    def stop_Command(self) -> Callable[[], CycleStepResult]:
        """ Stop the machine """
        return self.stop_CycleStep
    
    @protocol_command_function(description="Command moving the machine parts in a safe place if defined or to the setup position.")
    def move_to_safe_position_Command(self) -> Union[Runner, Callable[[], CycleStepResult]]:
        """ Set the machine in a safe position """
        runner = self.create_runner()
        config = self.get_current_config()
        
        if self.safeToOven != None:
            if self.safeToOven:
                config.vacuum_arm_at_oven = True
                runner.then_goto(config, info="go to oven")
            else :
                config.vacuum_arm_at_oven = False
                runner.then_goto(config, info="go to turn table")
            return runner.run()
        else :
            return self.setup_Command()
        
    @protocol_command_function(description="Execute process according to oven_time (time in oven in sec), saw_time (time in saw in sec),\
        and output the product on the specified output")
    def process_Command(self, oven_time: int, saw_time: int, output: MPSOutput) -> Runner:
        """
        Execute process according to oven_time (time in oven in sec), saw_time (time in saw in sec),
        and output the product on the specified output
        :return: A Runner performing the process
        """
        runner = self.create_runner()
        config = MultiProcessingConfig()

        runner.then_goto(config, info="initializing")

        if (oven_time == 0 and saw_time == 0 and output == MPSOutput.OVEN):
            return runner.run()

        
        #Oven process
        if (oven_time > 0):
            self.heat_in_oven_CycleStep(oven_time, config, runner)

            #End the process if no sawing time and output at oven
            if (saw_time == 0 and output == MPSOutput.OVEN):
                return runner.run()
        
        self.arm_to_oven_CycleStep(config, runner)

        self.pick_up_CycleStep(config, runner)

        self.arm_to_turntable_CycleStep(config, runner)

        self.place_CycleStep(config, runner)
        
        #Sawing process
        if (saw_time > 0):
            self.go_to_saw_CycleStep(config, runner)

            self.saw_on_turntable_CycleStep(saw_time, config, runner)

        #If output on conveyor, move payload to conveyor
        if (output == MPSOutput.CONVEYOR):
            self.go_to_conveyor_CycleStep(config, runner)

            self.eject_from_turntable_CycleStep(config, runner)

            return runner.run()
        
        self.go_to_arm_CycleStep(config, runner)

        self.pick_up_CycleStep(config, runner)
    
        self.arm_to_oven_CycleStep(config, runner)

        self.place_CycleStep(config, runner)

        self.arm_to_turntable_CycleStep(config, runner)

        return runner.run()