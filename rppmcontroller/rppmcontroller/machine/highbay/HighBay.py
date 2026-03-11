import logging
from enum import Enum
from typing import Dict, Any, Union, Callable, Optional, List

from typing_extensions import override

from rppmcontroller.behavior.CycleStepResult import CycleStepResult
from rppmcontroller.behavior.CycleStepResultEnum import CycleStepResultEnum
from rppmcontroller.behavior.decoratorFunctions import (cycle_step_function,
                                                        runner_augment_function)
from rppmcontroller.machine.Axis import AxisType, Axis
from rppmcontroller.machine.AxisConfig import AxisConfig
from rppmcontroller.machine.ConveyorState import ConveyorState
from rppmcontroller.machine.Machine import Machine
from rppmcontroller.machine.RequestedParameter import RequestedParameter
from rppmcontroller.machine.ResetHelper import ResetHelper
from rppmcontroller.machine.Runner import TransitioningMachine, Runner
from rppmcontroller.machine.highbay.HighBayConfig import HighBayConfig
from rppmcontroller.machine.highbay.HighBayParameters import HighBayParameters
from rppmcontroller.protocol.decoratorFunctions import \
    protocol_command_function


class Column(Enum):
    CONVEYOR = 0
    RIGHT = 1
    MIDDLE = 2
    LEFT = 3

    def to_counter_goal(self, parameters: HighBayParameters) -> int:
        if self == Column.CONVEYOR:
            return parameters.conveyor_column
        elif self == Column.RIGHT:
            return parameters.right_column
        elif self == Column.MIDDLE:
            return parameters.middle_column
        elif self == Column.LEFT:
            return parameters.left_column
        else:
            raise ValueError(f"no counter goal defined for {self}")



class Row(Enum):
    CONVEYOR = 0
    BOTTOM = 1
    MIDDLE = 2
    TOP = 3

    def to_counter_goal(self, parameters: HighBayParameters) -> int:
        if self == Row.CONVEYOR:
            return parameters.conveyor_row
        elif self == Row.BOTTOM:
            return parameters.bottom_row
        elif self == Row.MIDDLE:
            return parameters.middle_row
        elif self == Row.TOP:
            return parameters.top_row
        else:
            raise ValueError(f"no counter goal defined for {self}")


class HighBay(Machine, TransitioningMachine[HighBayConfig]):
    def __init__(self, id1, parameters: Optional[HighBayParameters] = None):
        if parameters is None:
            parameters = HighBayParameters()

        self.__parameters = parameters

        #  inputs
        self.__highbaySensHorizontal = False
        self.__highbaySensInside = True
        self.__highbaySensOutside = True
        self.__highbaySensVertical = False
        self.__highbaySensCantileverFront = False
        self.__highbaySensCantileverBack = False

        #  outputs
        self.__highbayActConveyorForward = False
        self.__highbayActConveyorBackward = False
        self.__highbayActHorizontalToRack = False
        self.__highbayActHorizontalToConveyor = False
        self.__highbayActDown = False
        self.__highbayActUp = False
        self.__highbayActCantileverForward = False
        self.__highbayActCantileverBackward = False
        self.__pwmVertical = 100
        self.__pwmHorizontal = 100

        #  encoder
        self.__highbaySensHorizontalEncoderCounter = 0
        self.__highbaySensVerticalEncoderCounter = 0
        self.__axisHorizontal = Axis(AxisType.Encoder, 5, parameters.max_horizontal_counter_value)
        self.__axisVertical = Axis(AxisType.Encoder, 10, parameters.max_vertical_counter_value)
        self.__horizontal_reset_helper = ResetHelper()
        self.__vertical_reset_helper = ResetHelper()

        dictMap = {
            RequestedParameter.REFERENCESWITCHHORIZONTALAXIS:
                self.__highbaySensHorizontal,
            RequestedParameter.LIGHTBARRIERINSIDE: self.__highbaySensInside,
            RequestedParameter.LIGHTBARRIEROUTSIDE: self.__highbaySensOutside,
            RequestedParameter.REFERENCESWITCHVERTICALAXIS:
                self.__highbaySensVertical,
            RequestedParameter.HORIZONTALAXISSTEP:
                self.__axisHorizontal.counterValueCurrent,
            RequestedParameter.VERTICALAXISSTEP:
                self.__axisVertical.counterValueCurrent,
            RequestedParameter.REFERENCESWITCHCANTILEVERFRONT:
                self.__highbaySensCantileverFront,
            RequestedParameter.REFERENCESWITCHCANTILEVERBACK:
                self.__highbaySensCantileverBack,
            RequestedParameter.MOTORCONVEYORBELTFORWARD:
                self.__highbayActConveyorForward,
            RequestedParameter.MOTORCONVEYORBELTBACKWARD:
                self.__highbayActConveyorBackward,
            RequestedParameter.MOTORHORIZONTALAXISFORWARD:
                self.__highbayActHorizontalToRack,
            RequestedParameter.MOTORHORIZONTALAXISBACKWARD:
                self.__highbayActHorizontalToConveyor,
            RequestedParameter.MOTORVERTICALAXISUPWARD: self.__highbayActUp,
            RequestedParameter.MOTORVERTICALAXISDOWNWARD:
                self.__highbayActDown,
            RequestedParameter.MOTORCANTILEVERFORWARD:
                self.__highbayActCantileverForward,
            RequestedParameter.MOTORCANTILEVERBACKWARD:
                self.__highbayActCantileverBackward,
            RequestedParameter.PWMVERTICAL: self.__pwmVertical,
            RequestedParameter.PWMHORIZONTAL: self.__pwmHorizontal
            }
        Machine.__init__(self, id1, dictMap)
        TransitioningMachine.__init__(self)

        # helper variables
        self.previous_isExecuting_log = None
        self.__is_initialized = False
        self.next_config = HighBayConfig()

    @property
    def __reset_helpers(self) -> List[ResetHelper]:
        return [self.vertical_reset_helper, self.horizontal_reset_helper]

    @property
    def must_reset(self) -> bool:
        return all((helper.is_marked_for_reset for helper in self.__reset_helpers))

    @must_reset.setter
    def must_reset(self, value: bool) -> None:
        if value:
            for helper in self.__reset_helpers:
                helper.mark_for_reset()
        else:
            for helper in self.__reset_helpers:
                helper.must_reset()

    @property
    def isInitialized(self) -> bool:
        return self.__is_initialized

    @isInitialized.setter
    def isInitialized(self, value):
        self.__is_initialized = value

    @Machine.isExecuting.getter
    def isExecuting(self) -> bool:
        res = (self.__highbayActUp or
               self.__highbayActDown or
               self.__highbayActConveyorForward or
               self.__highbayActConveyorBackward or
               self.__highbayActHorizontalToConveyor or
               self.__highbayActHorizontalToRack or
               self.__highbayActCantileverForward or
               self.__highbayActCantileverBackward or
               self.is_executing_runner)

        if self.executing_runner is None:
            routine = "None"
        else:
            routine = str(self.executing_runner)

        # log isexecuting and debug info only if message has changed
        isExecuting_log = f'\n\tisExecuting({self.id})={res}\n\tRoutine : {routine}\n\tSensors={self.sensorStatusString()}\n\tActuators= {self.actuatorStatusString()}'
        if isExecuting_log != self.previous_isExecuting_log :
            logging.debug(isExecuting_log)
            self.previous_isExecuting_log = isExecuting_log

        return res

    @property
    def parameters(self) -> HighBayParameters:
        return self.__parameters

    # ------------------ Input Properties ------------------

    @property
    def highbaySensHorizontal(self) -> bool:
        return self.__highbaySensHorizontal

    @highbaySensHorizontal.setter
    def highbaySensHorizontal(self, value: bool) -> None:
        self.__highbaySensHorizontal = value
        self.horizontal_reset_helper.mark_for_reset_if(value, self.highbaySensHorizontalEncoderCounter)

    @property
    def highbaySensInside(self) -> bool:
        return self.__highbaySensInside

    @highbaySensInside.setter
    def highbaySensInside(self, value: bool) -> None:
        self.__highbaySensInside = value

    @property
    def highbaySensOutside(self) -> bool:
        return self.__highbaySensOutside

    @highbaySensOutside.setter
    def highbaySensOutside(self, value: bool) -> None:
        self.__highbaySensOutside = value

    @property
    def highbaySensVertical(self) -> bool:
        return self.__highbaySensVertical

    @highbaySensVertical.setter
    def highbaySensVertical(self, value: bool) -> None:
        self.__highbaySensVertical = value
        self.vertical_reset_helper.mark_for_reset_if(value, self.highbaySensVerticalEncoderCounter)

    @property
    def highbaySensCantileverFront(self) -> bool:
        return self.__highbaySensCantileverFront

    @highbaySensCantileverFront.setter
    def highbaySensCantileverFront(self, value: bool) -> None:
        self.__highbaySensCantileverFront = value

    @property
    def highbaySensCantileverBack(self) -> bool:
        return self.__highbaySensCantileverBack

    @highbaySensCantileverBack.setter
    def highbaySensCantileverBack(self, value: bool) -> None:
        self.__highbaySensCantileverBack = value

    # ------------------ Output Properties ------------------

    @property
    def highbayActConveyorForward(self) -> bool:
        return self.__highbayActConveyorForward

    @highbayActConveyorForward.setter
    def highbayActConveyorForward(self, value: bool) -> None:
        self.__highbayActConveyorForward = value

    @property
    def highbayActConveyorBackward(self) -> bool:
        return self.__highbayActConveyorBackward

    @highbayActConveyorBackward.setter
    def highbayActConveyorBackward(self, value: bool) -> None:
        self.__highbayActConveyorBackward = value

    @property
    def highbayActHorizontalToRack(self) -> bool:
        return self.__highbayActHorizontalToRack

    @highbayActHorizontalToRack.setter
    def highbayActHorizontalToRack(self, value: bool) -> None:
        self.__highbayActHorizontalToRack = value

    @property
    def highbayActHorizontalToConveyor(self) -> bool:
        return self.__highbayActHorizontalToConveyor

    @highbayActHorizontalToConveyor.setter
    def highbayActHorizontalToConveyor(self, value: bool) -> None:
        self.__highbayActHorizontalToConveyor = value

    @property
    def highbayActDown(self) -> bool:
        return self.__highbayActDown

    @highbayActDown.setter
    def highbayActDown(self, value: bool) -> None:
        self.__highbayActDown = value

    @property
    def highbayActUp(self) -> bool:
        return self.__highbayActUp

    @highbayActUp.setter
    def highbayActUp(self, value: bool) -> None:
        self.__highbayActUp = value

    @property
    def highbayActCantileverForward(self) -> bool:
        return self.__highbayActCantileverForward

    @highbayActCantileverForward.setter
    def highbayActCantileverForward(self, value: bool) -> None:
        self.__highbayActCantileverForward = value

    @property
    def highbayActCantileverBackward(self) -> bool:
        return self.__highbayActCantileverBackward

    @highbayActCantileverBackward.setter
    def highbayActCantileverBackward(self, value: bool) -> None:
        self.__highbayActCantileverBackward = value

    @property
    def pwmHorizontal(self) -> int:
        return self.__pwmHorizontal

    @pwmHorizontal.setter
    def pwmHorizontal(self, value: int) -> None:
        self.__pwmHorizontal = value

    @property
    def pwmVertical(self) -> int:
        return self.__pwmVertical

    @pwmVertical.setter
    def pwmVertical(self, value: int) -> None:
        self.__pwmVertical = value

    # ------------------ Encoder Properties ------------------

    @property
    def highbaySensHorizontalEncoderCounter(self) -> int:
        return self.__highbaySensHorizontalEncoderCounter

    @highbaySensHorizontalEncoderCounter.setter
    def highbaySensHorizontalEncoderCounter(self, value: int) -> None:
        self.__highbaySensHorizontalEncoderCounter = value

    @property
    def highbaySensVerticalEncoderCounter(self) -> int:
        return self.__highbaySensVerticalEncoderCounter

    @highbaySensVerticalEncoderCounter.setter
    def highbaySensVerticalEncoderCounter(self, value: int) -> None:
        self.__highbaySensVerticalEncoderCounter = value

    @property
    def horizontal_reset_helper(self) -> ResetHelper:
        return self.__horizontal_reset_helper

    @property
    def vertical_reset_helper(self) -> ResetHelper:
        return self.__vertical_reset_helper

    def sensorStatusString(self) -> str:
        s = lambda b: "T" if b else "F"
        return (f"Counter: [{self.highbaySensHorizontalEncoderCounter}, {self.highbaySensVerticalEncoderCounter}], "
                f"Cantilev: [{s(self.highbaySensCantileverBack)}, {s(self.highbaySensCantileverFront)}], "
                f"SensorHV: [{s(self.highbaySensHorizontal)}, {s(self.highbaySensVertical)}], "
                f"SensConv: [{s(self.highbaySensInside)}, {s(self.highbaySensOutside)}]")

    def actuatorStatusString(self) -> str:
        s = lambda b: "T" if b else "F"
        return (f"Vertical: [{s(self.highbayActUp)}, {s(self.highbayActDown)}], "
                f"Horizont: [{s(self.highbayActHorizontalToRack)}, {s(self.highbayActHorizontalToConveyor)}], "
                f"Cantilev: [{s(self.highbayActCantileverBackward)}, {s(self.highbayActCantileverForward)}], "
                f"Conveyor: [{s(self.highbayActConveyorBackward)}, {s(self.highbayActConveyorForward)}], "
                f"PWM_VHR[{self.pwmVertical}, {self.pwmHorizontal}]")

    def inputStatus(self) -> Dict[str, Any]:
        return {  # TODO better adjust the names, I just made them up
            "highbaySensCantileverBack": self.__highbaySensCantileverBack,
            "highbaySensCantileverFront": self.__highbaySensCantileverFront,
            "highbaySensHorizontal": self.__highbaySensHorizontal,
            "highbaySensHorizontalEncoderCounter":
                self.__highbaySensHorizontalEncoderCounter,
            "highbaySensInside": self.__highbaySensInside,
            "highbaySensOutside": self.__highbaySensOutside,
            "highbaySensVertical": self.__highbaySensVertical,
            "highbaySensVerticalEncoderCounter":
                self.__highbaySensVerticalEncoderCounter,
            }

    def outputStatus(self) -> Dict[str, Any]:
        return {  # TODO better adjust the names, I just made them up
            "highbayActCantileverBackward":
                self.__highbayActCantileverBackward,
            "highbayActCantileverForward": self.__highbayActCantileverForward,
            "highbayActConveyorBackward": self.__highbayActConveyorBackward,
            "highbayActConveyorForward": self.__highbayActConveyorForward,
            "highbayActHorizontalToRack": self.__highbayActHorizontalToRack,
            "highbayActHorizontalToConveyor":
                self.__highbayActHorizontalToConveyor,
            "highbayActDown": self.__highbayActDown,
            "highbayActUp": self.__highbayActUp,
            "pwmVertical": self.__pwmVertical,
            "pwmHorizontal": self.__pwmHorizontal
        }

    def get_current_config(self) -> HighBayConfig:
        """
        Get the config describing the state in which the machine currently
        resides
        :return: The current config
        """
        return HighBayConfig(AxisConfig(self.highbaySensHorizontal,
                                        self.highbaySensHorizontalEncoderCounter),
                             AxisConfig(self.highbaySensVertical,
                                        self.highbaySensVerticalEncoderCounter),
                             not self.highbaySensCantileverBack,
                             ConveyorState.from_actuators(
                                 self.highbayActConveyorForward,
                                 self.highbayActConveyorBackward))

    @override
    @cycle_step_function()
    def goto_config_CycleStep(self,
                    config: HighBayConfig = HighBayConfig()) -> CycleStepResult:
        """
        Transfer the machine into another configuration
        :param config: The new configuration to transfer the machine to
        :return: True if the machine has reached the configuration,
        otherwise false
        """
        # logging.debug(f"going to {config}")

        # update pwm speed
        self.pwmVertical = config.verticalPWM

        # update conveyor belt state
        if config.conveyor_state == ConveyorState.IDLE:
            self.highbayActConveyorForward = False
            self.highbayActConveyorBackward = False
        elif config.conveyor_state == ConveyorState.FORWARD:
            self.highbayActConveyorForward = True
            self.highbayActConveyorBackward = False
        elif config.conveyor_state == ConveyorState.BACKWARD:
            self.highbayActConveyorForward = False
            self.highbayActConveyorBackward = True

        class ArmMovement(Enum):
            IDLE = 0
            """The arm doesn't need to move"""
            MINOR = 1
            """The arm needs to move a little bit, for picking up or
            dropping of an item"""
            MAYOR = 2
            """The arm needs to move alot and the cantilever should be
            retracted for that"""

        arm_movement = ArmMovement.IDLE

        # vertical axis
        self.__axisVertical.update(self.highbaySensVertical,
                                   self.highbaySensVerticalEncoderCounter)
        error = self.__axisVertical.config_would_exceed_max_counter_value(config.vertical_axis_config)
        if error:
            self.stop_CycleStep()
            return error.as_abort()
        if not self.__axisVertical.gotoAxisConfig(config.vertical_axis_config):
            # only allow small vertical movements for pickup
            distance_to_move = self.__axisVertical.counterValueCurrent - (
                config.vertical_axis_config.counter_goal or 0)
            if (
                abs(distance_to_move) > self.parameters.pickup_distance +
                    self.__axisVertical.tolerance):
                arm_movement = ArmMovement.MAYOR
            else:
                arm_movement = ArmMovement.MINOR

        # horizontal axis
        self.__axisHorizontal.update(self.highbaySensHorizontal,
                                     self.highbaySensHorizontalEncoderCounter)
        error = self.__axisHorizontal.config_would_exceed_max_counter_value(config.horizontal_axis_config)
        if error:
            self.stop_CycleStep()
            return error.as_abort()
        if not self.__axisHorizontal.gotoAxisConfig(config.horizontal_axis_config):
            arm_movement = ArmMovement.MAYOR
            if self.__axisHorizontal.isCloseFromEnd(config.horizontal_axis_config, self.parameters.pwm_approach_tolerance):
                self.pwmHorizontal = self.parameters.pwm_reduced_speed
            else :
                self.pwmHorizontal = self.parameters.pwm_standard_speed

        # logging.debug(f"arm_movement: {arm_movement}")
        if (
            arm_movement is ArmMovement.MAYOR and not
        self.highbaySensCantileverBack):
            self.highbayActCantileverForward = False
            self.highbayActCantileverBackward = True
            self.highbayActHorizontalToRack = False
            self.highbayActHorizontalToConveyor = False
            self.highbayActUp = False
            self.highbayActDown = False
            return CycleStepResult(CycleStepResultEnum.MUST_CONTINUE,
                                   "cantilever needs to be retracted for "
                                   "mayor arm movement")
        elif arm_movement is not ArmMovement.IDLE:
            self.highbayActCantileverForward = False
            self.highbayActCantileverBackward = False
            self.highbayActHorizontalToRack = self.__axisHorizontal.outputplus
            self.highbayActHorizontalToConveyor = (
                self.__axisHorizontal.outputminus)
            self.highbayActUp = self.__axisVertical.outputminus
            self.highbayActDown = self.__axisVertical.outputplus
            return CycleStepResult(CycleStepResultEnum.MUST_CONTINUE,
                                   "arm needs to be moved")
        elif (
            config.cantilever_extended and not
        self.highbaySensCantileverFront):
            self.highbayActCantileverForward = True
            self.highbayActCantileverBackward = False
            self.highbayActHorizontalToRack = False
            self.highbayActHorizontalToConveyor = False
            self.highbayActUp = False
            self.highbayActDown = False
            return CycleStepResult(CycleStepResultEnum.MUST_CONTINUE,
                                   "cantilever needs to be extended")
        elif (
            not config.cantilever_extended and not
        self.highbaySensCantileverBack):
            self.highbayActCantileverForward = False
            self.highbayActCantileverBackward = True
            self.highbayActHorizontalToRack = False
            self.highbayActHorizontalToConveyor = False
            self.highbayActUp = False
            self.highbayActDown = False
            return CycleStepResult(CycleStepResultEnum.MUST_CONTINUE,
                                   "cantilever needs to be retracted")
        else:
            self.highbayActCantileverForward = False
            self.highbayActCantileverBackward = False
            self.highbayActHorizontalToRack = False
            self.highbayActHorizontalToConveyor = False
            self.highbayActUp = False
            self.highbayActDown = False
            return CycleStepResult(CycleStepResultEnum.DONE)

    def internalStatus(self) -> Dict[str, Any]:
        return {"isExecuting": self.isExecuting}

    def create_next_config(self) -> HighBayConfig:
        """
        Sets the next_config to the current config and returns the
        config object for editing
        :return: The new next_config
        """
        self.next_config = self.get_current_config()
        return self.next_config

    def goto_next_config(self) -> Callable[[], CycleStepResult]:
        """
        Goes to the next_config and returns a lambda going to that config
        :return:
        """
        runnable = lambda: self.goto_config_CycleStep(self.next_config)
        runnable()
        return runnable

    @override
    @cycle_step_function()
    def stop_CycleStep(self) -> CycleStepResult:
        self.highbayActUp = False
        self.highbayActDown = False
        self.highbayActHorizontalToRack = False
        self.highbayActHorizontalToConveyor = False
        self.highbayActConveyorForward = False
        self.highbayActConveyorBackward = False
        self.highbayActCantileverForward = False
        self.highbayActCantileverBackward = False

        self.__axisVertical.resetDirection()
        self.__axisHorizontal.resetDirection()

        return CycleStepResult(CycleStepResultEnum.DONE)

    @runner_augment_function()
    def run_setup_unless_initialized(self, runner: Runner) -> None:
        """
        Appends a setup step to the provided runner, if this is not initialized

        :param runner: The Runner to append the setup step to
        :return: None
        """
        if not self.isInitialized:
            runner.then_run_runner_from(self.setup_Command, info="setup")

    # methods intended for orchestrator

    @protocol_command_function()
    def setup_Command(self) -> Runner:
        """
        Set up the HighBay and calibrate the counters.
        :return: A Runner performing the setup
        """

        self.pwmHorizontal = self.parameters.pwm_standard_speed
        self.pwmVertical = self.parameters.pwm_standard_speed

        def mark_setup_finished():
            self.__is_initialized = True
            return CycleStepResult.done()

        return self.create_runner().then_run(self.goto_config_CycleStep, info="goto_config_setup").then_run(
            mark_setup_finished, info="mark_setup_finished")

    @protocol_command_function()
    def conveyor_forward_Command(self) -> Callable[[], CycleStepResult]:
        """
        Starts the conveyor belt moving forward (ie. towards the exit)
        :return: A Runner performing the action
        """
        self.create_next_config().conveyor_state = ConveyorState.FORWARD
        return self.goto_next_config()

    @protocol_command_function()
    def conveyor_backward_Command(self) -> Callable[[], CycleStepResult]:
        """
        Starts the conveyor belt moving backward (ie. towards the telescopic fork of the stacker crane)
        :return: A Runner performing the action
        """
        self.create_next_config().conveyor_state = ConveyorState.BACKWARD
        return self.goto_next_config()

    @protocol_command_function()
    def conveyor_stop_Command(self) -> Callable[[], CycleStepResult]:
        """
        Stops the conveyor belt
        :return: A Runner performing the action
        """
        self.create_next_config().conveyor_state = ConveyorState.IDLE
        return self.goto_next_config()

    @protocol_command_function()
    def cantilever_forward_Command(self) -> Callable[[], CycleStepResult]:
        """
        Extends the Telescopic Fork (cantilever or Load Handling Device (LHD)) toward the storage racks or the conveyor
        :return: A Runner performing the action
        """
        self.create_next_config().cantilever_extended = True
        return self.goto_next_config()

    @protocol_command_function()
    def cantilever_backward_Command(self) -> Callable[[], CycleStepResult]:
        """
        Retracts the Telescopic Fork (cantilever or Load Handling Device (LHD))
        :return: A Runner performing the action
        """
        self.create_next_config().cantilever_extended = False
        return self.goto_next_config()

    @protocol_command_function()
    def horizontal_to_Command(self, counter_goal: int) -> Runner:
        """
        Moves the stacker crane horizontally to the specified counter goal

        approximative encoder values for key horizontal places are:
            - Conveyor column: 70 (Load/Unload position)
            - first rack column: 1550
            - second rack column: 2700
            - third rack column: 3900

        :param counter_goal: The target counter goal for the horizontal axis (traveling axis),
        :return: A Runner performing the action
        """
        runner = self.create_runner()
        self.run_setup_unless_initialized(runner)
        config = self.get_current_config()
        config.horizontal_axis_config = AxisConfig.to_counter_goal(counter_goal)
        return runner.then_goto(config)

    @protocol_command_function()
    def vertical_to_Command(self, counter_goal: int) -> Runner:
        """
        Moves the stacker crane vertically to the specified counter goal

        approximate encoder values for key vertical rows are:
            - TOP row: 200 (Highest position)
            - MIDDLE row: 900
            - BOTTOM row: 1700
            - CONVEYOR row: 1450 (Load/Unload position)

        :param counter_goal: The target counter goal for the vertical axis (lifting axis),
        :return: A Runner performing the action
        """
        runner = self.create_runner()
        self.run_setup_unless_initialized(runner)
        config = self.get_current_config()
        config.vertical_axis_config = AxisConfig.to_counter_goal(counter_goal)
        return runner.then_goto(config)

    @protocol_command_function()
    def goto_column_Command(self, column: Union[Column, int]) -> Callable[
        [], CycleStepResult]:
        """
        Moves the stacker crane horizontally to the specified column

        Possible values are:
            - Column.CONVEYOR or 0: Load/Unload position at the conveyor belt
            - Column.RIGHT or 1: First storage rack column
            - Column.MIDDLE or 2: Middle storage rack column
            - Column.LEFT or 3: Last storage rack column

        :param column: The target column to move to

        :return: A Callable performing the action
        """
        if isinstance(column, int):
            column = Column(column)

        return self.horizontal_to_Command(column.to_counter_goal(self.parameters))

    @protocol_command_function()
    def goto_row_Command(self, row: Union[Row, int]) -> Callable[
        [], CycleStepResult]:
        """
        Moves the stacker crane vertically to the specified row

        Possible values are:
            - Row.CONVEYOR or 0: Load/Unload position at the conveyor belt
            - Row.BOTTOM or 1: Bottom storage row
            - Row.MIDDLE or 2: Middle storage row
            - Row.TOP or 3: Top storage row

        :param row: The target row to move to
        :return: A Callable performing the action
        """
        if isinstance(row, int):
            row = Row(row)

        return self.vertical_to_Command(row.to_counter_goal(self.parameters))

    @protocol_command_function()
    def store_to_Command(self,
                         row: Union[Row, int],
                         column: Union[Column, int]) -> Runner:
        """
        Stores an item from the conveyor to the specified row and column in the rack
        then performs a setup to recalibrate the encoders.

        Row possible values are:
            - Row.CONVEYOR or 0: Load/Unload position at the conveyor belt
            - Row.BOTTOM or 1: Bottom storage row
            - Row.MIDDLE or 2: Middle storage row
            - Row.TOP or 3: Top storage row
        Column possible values are:
            - Column.CONVEYOR or 0: Load/Unload position at the conveyor belt
            - Column.RIGHT or 1: First storage rack column
            - Column.MIDDLE or 2: Middle storage rack column
            - Column.LEFT or 3: Last storage rack column

        :param row: The target row to store the item to
        :param column: The target column to store the item to
        :return: A Runner performing the command
        """
        if isinstance(row, int):
            row = Row(row)
        if isinstance(column, int):
            column = Column(column)

        runner = self.create_runner()
        self.run_setup_unless_initialized(runner)

        # goto conveyor
        horizontal_axis_config = AxisConfig.to_counter_goal(
            Column.CONVEYOR.to_counter_goal(self.parameters))
        vertical_axis_config = AxisConfig.to_counter_goal(
            Row.CONVEYOR.to_counter_goal(self.parameters))
        config = HighBayConfig(horizontal_axis_config,
                               vertical_axis_config,
                               True)
        runner.then_goto(config, info="goto conveyor")

        # move item on lever
        config.conveyor_state = ConveyorState.BACKWARD
        runner.then_goto(config,
                         until=lambda: not self.highbaySensInside,
                         info="move item on lever")

        # pickup item and stop conveyor
        config.conveyor_state = ConveyorState.IDLE
        config.vertical_axis_config.counter_goal -= self.parameters.pickup_distance
        config.verticalPWM = self.parameters.pwm_reduced_speed
        runner.then_goto(config, info="pickup item and stop conveyor")

        # move to rack
        horizontal_axis_config = AxisConfig.to_counter_goal(
            column.to_counter_goal(self.parameters))
        vertical_axis_config = AxisConfig.to_counter_goal(
            row.to_counter_goal(self.parameters) - self.parameters.pickup_distance)
        config = HighBayConfig(horizontal_axis_config,
                               vertical_axis_config,
                               True)
        runner.then_goto(config, info="move to rack")

        # drop off item
        config.vertical_axis_config.counter_goal += self.parameters.pickup_distance
        runner.then_goto(config, info="drop off item")

        # perform a setup to recalibrate the encoders
        runner.then_run_runner_from(self.setup_Command, info="setup")

        return runner

    @protocol_command_function()
    def pickup_from_Command(self,
                            row: Union[Row, int],
                            column: Union[Column, int]) -> Runner:
        """
        Picks up an item from the specified row and column in the rack and
        moves it to the conveyor belt, then performs a setup to recalibrate
        the encoders.

        Row possible values are:
            - Row.CONVEYOR or 0: Load/Unload position at the conveyor belt
            - Row.BOTTOM or 1: Bottom storage row
            - Row.MIDDLE or 2: Middle storage row
            - Row.TOP or 3: Top storage row
        Column possible values are:
            - Column.CONVEYOR or 0: Load/Unload position at the conveyor belt
            - Column.RIGHT or 1: First storage rack column
            - Column.MIDDLE or 2: Middle storage rack column
            - Column.LEFT or 3: Last storage rack column

        :param row: The source row to pick the item from
        :param column: The source column to pick the item from
        :return: A Runner performing the command
        """

        if isinstance(row, int):
            row = Row(row)
        if isinstance(column, int):
            column = Column(column)

        runner = self.create_runner()
        self.run_setup_unless_initialized(runner)

        # go to rack
        horizontal_axis_config = AxisConfig.to_counter_goal(
            column.to_counter_goal(self.parameters))
        vertical_axis_config = AxisConfig.to_counter_goal(
            row.to_counter_goal(self.parameters))
        config = HighBayConfig(horizontal_axis_config,
                               vertical_axis_config,
                               True)
        runner.then_goto(config, info="go to rack")

        # pickup item
        config.vertical_axis_config.counter_goal -= self.parameters.pickup_distance
        runner.then_goto(config, info="pickup item")

        # recalibrate horizontal-axis, since we need to be precise here
        config.horizontal_axis_config = AxisConfig.to_end_position()
        runner.then_goto(config, info="recalibrating horizontal-axis")

        # move to conveyor
        horizontal_axis_config = AxisConfig.to_counter_goal(
            Column.CONVEYOR.to_counter_goal(self.parameters))
        vertical_axis_config = AxisConfig.to_counter_goal(
            Row.CONVEYOR.to_counter_goal(self.parameters))
        config = HighBayConfig(horizontal_axis_config,
                               vertical_axis_config,
                               True,
                               ConveyorState.FORWARD)
        config.verticalPWM = self.parameters.pwm_reduced_speed
        runner.then_goto(config,
                         until=lambda: not self.highbaySensOutside,
                         info="move to conveyor")

        # perform another setup
        runner.then_run_runner_from(self.setup_Command, info="setup")

        return runner

    @protocol_command_function()
    def stop_Command(self) -> Callable[[], CycleStepResult]:
        """
        Stops all movements of the highbay
        :return: A callable performing the command
        """
        return lambda: self.stop_CycleStep()

    @protocol_command_function()
    def move_to_safe_position_Command(self) -> Runner:
        """
        Moves the Vacuum Gripper to the safe position if specified. Go to setup position else
        :return: A Runner performing the command
        """
        if self.parameters.vertical_safety_position is not None and self.parameters.horizontal_safety_position is not None:
            runner = self.create_runner()
            self.run_setup_unless_initialized(runner)
            config = self.get_current_config()
            config.horizontal_axis_config = AxisConfig.to_counter_goal(self.parameters.horizontal_safety_position)
            config.vertical_axis_config = AxisConfig.to_counter_goal(self.parameters.vertical_safety_position)
            return runner.then_goto(config)

            # runner = self.create_runner()
            # runner.then_run(self.vertical_to_Command(self.safeVertical), info="Moving vertically to safe pos")
            # runner.then_run(self.horizontal_to_Command(self.safeHorizontal), info="Moving horizontally to safe pos")
            # return runner
        else:
            return self.setup_Command()
