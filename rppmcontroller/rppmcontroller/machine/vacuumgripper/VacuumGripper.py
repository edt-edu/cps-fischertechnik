import logging
from typing import Any, Callable, Dict, Optional, List

from typing_extensions import override, deprecated

from rppmcontroller.behavior.CycleStepResult import CycleStepResult
from rppmcontroller.behavior.CycleStepResultEnum import CycleStepResultEnum
from rppmcontroller.behavior.decoratorFunctions import cycle_step_function
from rppmcontroller.machine.Axis import AxisType, Axis
from rppmcontroller.machine.AxisBoolThreeD import AxisBoolThreeD
from rppmcontroller.machine.AxisConfig import AxisConfig
from rppmcontroller.machine.Machine import Machine
from rppmcontroller.machine.Position import Position
from rppmcontroller.machine.RequestedParameter import RequestedParameter
from rppmcontroller.machine.ResetHelper import ResetHelper
from rppmcontroller.machine.Runner import TransitioningMachine, Runner
from rppmcontroller.machine.vacuumgripper.VacuumGripperConfig import \
    VacuumGripperConfig
from rppmcontroller.machine.vacuumgripper.VacuumGripperParameters import \
    VacuumGripperParameters
from rppmcontroller.protocol.decoratorFunctions import \
    protocol_command_function
from rppmcontroller.utils.CyclicWaiter import CyclicWaiter


class VacuumGripper(Machine, TransitioningMachine[VacuumGripperConfig]):

    def __init__(self, id1, parameters: Optional[VacuumGripperParameters] = None):
        if parameters is None:
            parameters = VacuumGripperParameters()

        self.__parameters = parameters

        # inputs
        self.__vacuumSensArmEndIn = False
        self.__vacuumSensVerticalEndUp = False
        self.__vacuumSensRotEnd = False

        # outputs
        self.__vacuumActArmOut = False
        self.__vacuumActArmIn = False
        self.__vacuumActVerticalDown = False
        self.__vacuumActVerticalUp = False
        self.__vacuumActRotRight = False
        self.__vacuumActRotLeft = False
        self.__vacuumActCompressorOn = False
        self.__vacuumActValve = False
        self.__pwmVertical = 100
        self.__pwmHorizontal = 100
        self.__pwmRotational = 100

        # encoders
        self.__vacuumSensRotEncoderCounter = 0
        self.__vacuumSensVerticalEncoderCounter = 0
        self.__vacuumSensArmEncoderCounter = 0
        self.__axisArm = Axis(AxisType.Encoder, 5, parameters.max_horizontal_counter_value)
        self.__axisVertical = Axis(AxisType.Encoder, 10, parameters.max_vertical_counter_value)
        self.__axisRot = Axis(AxisType.Encoder, 5, parameters.max_rotational_counter_value)
        self.__rot_reset_helper = ResetHelper()
        self.__vertical_reset_helper = ResetHelper()
        self.__arm_reset_helper = ResetHelper()


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
                   RequestedParameter.VALVEVACUUM: self.__vacuumActValve,
                   RequestedParameter.PWMVERTICAL: self.__pwmVertical,
                   RequestedParameter.PWMHORIZONTAL: self.__pwmHorizontal,
                   RequestedParameter.PWMROTATION: self.__pwmRotational}
        super().__init__(id1, dictMap)
        TransitioningMachine.__init__(self)

        # helper variables
        self.configReached = False
        self.__is_initialized = False
        self.configGoal = None
        self.previous_isExecuting_log = None
        self.__gripperWaiter = CyclicWaiter(10)

    @property
    def parameters(self) -> VacuumGripperParameters:
        return self.__parameters

    @property
    def __reset_helpers(self) -> List[ResetHelper]:
        return [self.vertical_reset_helper, self.rot_reset_helper, self.arm_reset_helper]

    @property
    def mustReset(self) -> bool:
        return all(
            (helper.is_marked_for_reset for helper in self.__reset_helpers))

    @mustReset.setter
    def mustReset(self, value: bool) -> None:
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
        # noinspection PyDeprecation
        res = (self.isProcessingSequence() or
               self.__vacuumActVerticalUp or
               self.__vacuumActVerticalDown or
               self.__vacuumActRotRight or
               self.__vacuumActRotLeft or
               self.__vacuumActArmOut or
               self.__vacuumActArmIn or
               self.__vacuumActCompressorOn or
               self.__vacuumActValve or
               self.nbMinimumRequiredExecutionCycles != 0 or
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
    def pwmVertical(self):
        return self.__pwmVertical

    @pwmVertical.setter
    def pwmVertical(self, value):
        self.__pwmVertical = value

    @property
    def pwmHorizontal(self):
        return self.__pwmHorizontal

    @pwmHorizontal.setter
    def pwmHorizontal(self, value):
        self.__pwmHorizontal = value

    @property
    def pwmRotational(self):
        return self.__pwmRotational

    @pwmRotational.setter
    def pwmRotational(self, value):
        self.__pwmRotational = value

    @property
    def vacuumSensVerticalEndUp(self) -> bool:
        """The Vacuum Gripper sensor for vertical axis

        True if arm is up at maximum position  so that it touches the sensor"""
        return self.__vacuumSensVerticalEndUp

    @vacuumSensVerticalEndUp.setter
    def vacuumSensVerticalEndUp(self, value):
        self.__vacuumSensVerticalEndUp = value
        self.vertical_reset_helper.mark_for_reset_if(value and self.vacuumSensVerticalEncoderCounter != 0, self.vacuumSensVerticalEncoderCounter)

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
        self.arm_reset_helper.mark_for_reset_if(value and self.vacuumSensArmEncoderCounter != 0, self.vacuumSensArmEncoderCounter)

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
        self.rot_reset_helper.mark_for_reset_if(value and self.vacuumSensRotEncoderCounter != 0, self.vacuumSensRotEncoderCounter)

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

    @property
    def vertical_reset_helper(self) -> ResetHelper:
        return self.__vertical_reset_helper

    @property
    def arm_reset_helper(self) -> ResetHelper:
        return self.__arm_reset_helper

    @property
    def rot_reset_helper(self) -> ResetHelper:
        return self.__rot_reset_helper

    def sensorStatusString(self) -> str:
        s = lambda b: "T" if b else "F"

        return f"CountVRH[{self.vacuumSensVerticalEncoderCounter}, {self.vacuumSensRotEncoderCounter}, {self.vacuumSensArmEncoderCounter}], " + \
               f"SensVRH[{s(self.vacuumSensVerticalEndUp)}, {s(self.vacuumSensRotEnd)}, {s(self.vacuumSensArmEndIn)}]"

    def actuatorStatusString(self) -> str:
        s = lambda b: "T" if b else "F"

        return f"MoveVert[{s(self.vacuumActVerticalUp)}, {s(self.vacuumActVerticalDown)}], " + \
               f"MoveRot[{s(self.vacuumActRotRight)}, {s(self.vacuumActRotLeft)}], " + \
               f"MoveHor[{s(self.vacuumActArmOut)}, {s(self.vacuumActArmIn)}], " + \
               f"CompValv[{s(self.vacuumActCompressorOn)}, {s(self.vacuumActValve)}], " + \
               f"PWM_VHR[{self.pwmVertical}, {self.pwmHorizontal}, {self.pwmRotational}]"


    def inputStatus(self) -> Dict[str, Any]:
        return {
            "vacuumSensVerticalEncoderCounter": self.vacuumSensVerticalEncoderCounter,
            "vacuumSensRotEncoderCounter": self.vacuumSensRotEncoderCounter,
            "vacuumSensArmEncoderCounter": self.vacuumSensArmEncoderCounter,
            "vacuumSensVerticalEndUp": self.vacuumSensVerticalEndUp,
            "vacuumSensRotEnd": self.vacuumSensRotEnd,
            "vacuumSensArmEndIn": self.vacuumSensArmEndIn,
        }

    def outputStatus(self) -> Dict[str, Any]:
        return {
            "vacuumActVerticalUp": self.__vacuumActVerticalUp,
            "vacuumActVerticalDown": self.__vacuumActVerticalDown,
            "vacuumActRotRight": self.__vacuumActRotRight,
            "vacuumActRotLeft": self.__vacuumActRotLeft,
            "vacuumActArmOut": self.__vacuumActArmOut,
            "vacuumActArmIn": self.__vacuumActArmIn,
            "vacuumActCompressorOn": self.__vacuumActCompressorOn,
            "vacuumActValve": self.__vacuumActValve,
            "pwmVertical": self.__pwmVertical,
            "pwmHorizontal": self.__pwmHorizontal,
            "pwmRotational": self.__pwmRotational,
        }

    def internalStatus(self) -> Dict[str, Any]:
        return {
            "isExecuting": self.isExecuting,
        }

    @override
    @cycle_step_function()
    def goto_config_CycleStep(self, config: VacuumGripperConfig = VacuumGripperConfig()) -> CycleStepResult:
        res = CycleStepResult.done()

        # horizontal axis
        self.vacuumActArmIn = False
        self.vacuumActArmOut = False
        self.__axisArm.update(self.vacuumSensArmEndIn, self.vacuumSensArmEncoderCounter)
        error = self.__axisArm.config_would_exceed_max_counter_value(config.horizontal_axis_config)
        if error:
            self.stop_CycleStep()
            return error.as_abort()
        if not self.__axisArm.gotoAxisConfig(config.horizontal_axis_config):
            self.vacuumActArmIn = self.__axisArm.outputminus
            self.vacuumActArmOut = self.__axisArm.outputplus
            if self.__axisArm.isCloseFromEnd(config.horizontal_axis_config, self.parameters.pwm_approach_tolerance):
                self.pwmHorizontal = self.parameters.pwm_horizontal_approach_speed
            else :
                self.pwmHorizontal = self.parameters.pwm_standard_speed
            res = CycleStepResult(CycleStepResultEnum.MUST_CONTINUE, "extending or retracting arm")
        if self.vacuumActArmIn and self.vacuumSensArmEndIn:
            self.stop_CycleStep()
            return CycleStepResult(CycleStepResultEnum.ABORTED_ERROR, "can't move beyond ref switch")

        # vertical axis
        self.vacuumActVerticalUp = False
        self.vacuumActVerticalDown = False
        self.__axisVertical.update(self.vacuumSensVerticalEndUp, self.vacuumSensVerticalEncoderCounter)
        error = self.__axisVertical.config_would_exceed_max_counter_value(config.vertical_axis_config)
        if error:
            self.stop_CycleStep()
            return error.as_abort()
        if not self.__axisVertical.gotoAxisConfig(config.vertical_axis_config):
            self.vacuumActVerticalUp = self.__axisVertical.outputminus
            self.vacuumActVerticalDown = self.__axisVertical.outputplus
            if self.__axisVertical.isCloseFromEnd(config.vertical_axis_config, self.parameters.pwm_approach_tolerance):
                self.pwmVertical= self.parameters.pwm_vertical_approach_speed
            else :
                self.pwmVertical = self.parameters.pwm_standard_speed
            res = CycleStepResult(CycleStepResultEnum.MUST_CONTINUE, "moving down or up")
        if self.vacuumActVerticalUp and self.vacuumSensVerticalEndUp:
            self.stop_CycleStep()
            return CycleStepResult(CycleStepResultEnum.ABORTED_ERROR, "can't move beyond ref switch")

        # rotation
        self.vacuumActRotRight = False
        self.vacuumActRotLeft = False
        self.__axisRot.update(self.vacuumSensRotEnd, self.vacuumSensRotEncoderCounter)
        error = self.__axisRot.config_would_exceed_max_counter_value(config.rotation_axis_config)
        if error:
            self.stop_CycleStep()
            return error.as_abort()
        if not self.__axisRot.gotoAxisConfig(config.rotation_axis_config):
            self.vacuumActRotRight = self.__axisRot.outputminus
            self.vacuumActRotLeft = self.__axisRot.outputplus
            if self.__axisRot.isCloseFromEnd(config.rotation_axis_config, self.parameters.pwm_approach_tolerance):
                self.pwmRotational = self.parameters.pwm_rotational_approach_speed
            else :
                self.pwmRotational = self.parameters.pwm_standard_speed
            res = CycleStepResult(CycleStepResultEnum.MUST_CONTINUE, "rotating arm")
        if self.vacuumActRotRight and self.vacuumSensRotEnd:
            self.stop_CycleStep()
            return CycleStepResult(CycleStepResultEnum.ABORTED_ERROR, "can't move beyond ref switch")

        # vacuum valve
        self.vacuumActValve = config.gripper_active

        # activate compressor only if the valve is active
        self.vacuumActCompressorOn = self.vacuumActValve

        return res

    def resetHelper(self) -> bool:
        """ Returns whether the counters must be reset
        will return true only one time per mustReset request
        :return bool: True if self.mustReset
        """
        if self.mustReset:
            self.mustReset = False
            return True
        else:
            return False


    ### ____________ Functions intended to be called in the exLoop function of the RevPiPyMachineController ________________

    @override
    @cycle_step_function()
    def stop_CycleStep(self) -> CycleStepResult:
        self.vacuumActArmOut = False
        self.vacuumActArmIn = False
        self.vacuumActVerticalDown = False
        self.vacuumActVerticalUp = False
        self.vacuumActRotRight = False
        self.vacuumActRotLeft = False
        self.vacuumActCompressorOn = False
        self.vacuumActValve = False

        self.__axisVertical.resetDirection()
        self.__axisArm.resetDirection()
        self.__axisRot.resetDirection()

        self.pwmHorizontal = self.parameters.pwm_standard_speed
        self.pwmRotational = self.parameters.pwm_standard_speed
        self.pwmVertical = self.parameters.pwm_standard_speed

        self.stop_runners()

        return CycleStepResult.done()

    def get_current_config(self) -> VacuumGripperConfig:
        """
        Get the config in which the gripper currently resides in
        :return: A VacuumGripperConfig describing the current state of the
         gripper
        """
        return VacuumGripperConfig(AxisConfig.to_counter_goal(
            self.vacuumSensVerticalEncoderCounter),
            AxisConfig.to_counter_goal(self.vacuumSensRotEncoderCounter),
            AxisConfig.to_counter_goal(self.vacuumSensArmEncoderCounter),
            self.vacuumActValve)

    @staticmethod
    def config_from_target_pos(target_position: Position) -> (
        VacuumGripperConfig):
        """
        Create a VacuumGripperConfig which transfers the gripper into the
        target position
        :param target_position: The desired position
        :return: A VacuumGripperConfig pointing to that target position
        """
        return VacuumGripperConfig(AxisConfig.to_counter_goal(
            target_position.vertical),
            AxisConfig.to_counter_goal(target_position.rot),
            AxisConfig.to_counter_goal(target_position.horizontal))

    ### ____________ Functions callable from orchestrator ________________
    #   function name must be lowercase and finish with '_Command' postfix (cf. RevPiPyMachineController)
    # they must return a lambda to a CycleStep function

    @protocol_command_function(description="Used to move the engines to a reference point.")
    def setup_Command(self) -> Runner:
        """
        Command to triggering a setup. Used to move the engines to a reference point (i.e. a point with a reference switch) so we can reset the counters or encoders

        :return: A Runner performing the setup
        """
        # when performing a setup we want to horizontally retract the arm first, in order to avoid collision with other machines
        runner = self.create_runner()
        config = self.get_current_config()
        config.horizontal_axis_config = AxisConfig.to_end_position()
        runner.then_goto(config, info="retracting arm")

        # now we want to move everything else into setup position
        config = VacuumGripperConfig()
        runner.then_goto(config, info="setup")

        # finally we mark the setup as done
        def on_setup_finish():
            self.__is_initialized = True
            return CycleStepResult.done()

        runner.then_run(on_setup_finish, info="finishing")

        return runner

    @protocol_command_function(description="Moves the gripper to the position without changing the valve or compressor status.")
    def go_to_position_Command(self, targetPos: Position) -> Runner:
        """
        Command triggering a go_to_position action. I.e. it moves the gripper to the position without changing the valve or compressor status.
        It may trigger a setup first if the machine is not initialized

        :return: A Runner performing the command
        """
        runner = self.create_runner()
        if not self.isInitialized:
            runner.then_run_runner_from(self.setup_Command, info="setup")

        config = self.config_from_target_pos(targetPos)
        config.gripper_active = self.vacuumActValve
        runner.then_goto(config, info="moving to target position")

        return runner

    @protocol_command_function(description="Moves the arm to the specified position, but retracts the arm before")
    def retracted_go_to_position_Command(self, target_position: Position) -> Runner:
        """
        Moves the arm to the specified position, but retracts the arm before
        moving and extends it afterward if needed

        :param target_position: Where to go
        :return: A Runner performing the move
        """
        runner = self.create_runner()
        runner.then_run_runner_from(lambda: self.retract_arm_Command(), info="Retracting arm")
        runner.then_run_runner_from(lambda: self.ordered_go_to_Command(target_position, AxisBoolThreeD(vertical=True, horizontal=False, rot=True)), info="Move to position")
        return runner


    @protocol_command_function(description="Command triggering a move token action. I.e. it picks a token on the startPos and drop it on the endPos")
    def move_Command(self, startPos: Position, endPos: Position) -> Runner:
        """
        Command triggering a move token action. I.e. it picks a token on the startPos and drop it on the endPos

        :return: A Runner performing the command
        """
        runner = self.create_runner()

        logging.debug(f"moving from {startPos} to {endPos}")

        # pickup token
        runner.then_run_runner_from(lambda: self.pick_Command(startPos), info="pickup")

        # place token
        runner.then_run_runner_from(lambda: self.place_Command(endPos), info="place")

        return runner


    @protocol_command_function(description="Command triggering a pick token action. I.e. it moves the arm to the startPos and grips a token on that position")
    def pick_Command(self, startPos: Position) -> Runner:
        """
        Command triggering a pick token action. I.e. it moves the arm to the startPos and grips a token on that position

        :return: A Runner performing the action
        """
        runner = self.create_runner()
        hover_pos = Position(startPos.meaning, startPos.vertical - self.parameters.hover_offset, startPos.rot, startPos.horizontal)
        pressure_pos = Position(startPos.meaning, startPos.vertical + self.parameters.pressure_offset, startPos.rot, startPos.horizontal)

        # hover over payload
        runner.then_run_runner_from(lambda: self.retracted_go_to_position_Command(hover_pos), info="hover over payload")

        # touch payload
        runner.then_run_runner_from(lambda: self.go_to_position_Command(pressure_pos), info="preparing pickup")

        # pickup
        runner.then_run_runner_from(lambda: self.grip_Command(), info="grab payload")

        # move back to hover pos
        runner.then_run_runner_from(lambda: self.go_to_position_Command(hover_pos), info="lift payload")

        return runner


    @protocol_command_function(description="Command triggering a place token action. I.e. it moves the arm to the endPos and release the token on that position")
    def place_Command(self, endPos: Position) -> Runner:
        """
        Command triggering a place token action. I.e. it moves the arm to the endPos and release the token on that position

        :return: A Runner performing the placement
        """
        runner = self.create_runner()
        hover_pos = Position(endPos.meaning, endPos.vertical - self.parameters.hover_offset, endPos.rot, endPos.horizontal)
        pressure_pos = Position(endPos.meaning, endPos.vertical + self.parameters.pressure_offset, endPos.rot, endPos.horizontal)

        # hover over end pos
        runner.then_run_runner_from(lambda: self.retracted_go_to_position_Command(hover_pos), info="hover over drop-off position")

        # move down
        runner.then_run_runner_from(lambda: self.go_to_position_Command(pressure_pos), info="preparing drop-off")

        # release token
        runner.then_run_runner_from(lambda: self.release_Command(), info="releasing")

        # move up again
        runner.then_run_runner_from(lambda: self.go_to_position_Command(hover_pos), info="retreat from drop-off position")

        return runner


    @protocol_command_function(description="Command activating the gripper without moving the arm.")
    def grip_Command(self) -> Runner:
        """
        Command activating the gripper without moving the arm.

        :return: A Runner performing the grab
        """
        config = self.get_current_config()
        config.gripper_active = True
        return self.create_runner().then_goto(config, and_stay_for=0.5, info="gripping")

    #@protocol_command_function(description="Command deactivating the gripper without moving the arm.")
    # def release_Command(self) -> Runner:
    def release_Command(self) -> Runner:
        """
        Command deactivating the gripper without moving the arm.

        :return: A Runner performing the release
        """
        config = self.get_current_config()
        config.gripper_active = False
        return self.create_runner().then_goto(config, info="releasing")

    @protocol_command_function(description="Command stopping all engines (incl. compressor).")
    def stop_Command(self) -> Callable[[], CycleStepResult]:
        return self.stop_CycleStep

    @protocol_command_function(description="Moves the Vacuum Gripper to the safe position if specified. Go to setup position otherwise.")
    def move_to_safe_position_Command(self) -> Runner:
        """
        Moves the Vacuum Gripper to the safe position if specified. Go to setup position otherwise

        :return: A Runner performing the command
        """
        if self.parameters.vertical_safety_position is not None and self.parameters.rotational_safety_position is not None and self.parameters.horizontal_safety_position is not None:
            position = Position("END", self.parameters.vertical_safety_position, self.parameters.rotational_safety_position, self.parameters.horizontal_safety_position)
            return self.go_to_position_Command(position)
        else:
            return self.setup_Command()

    @protocol_command_function(description="Retract the arm of the vacuum gripper.")
    def retract_arm_Command(self) -> Runner:
        """
        Retract the arm of the vacuum gripper.

        :return: A Runner performing the command
        """
        config = self.get_current_config()
        config.horizontal_axis_config = AxisConfig.to_end_position()
        return self.create_runner().then_goto(config, info="retracting arm")

    @deprecated("Irritating name, use ordered_go_to_Command instead.")
    @protocol_command_function(description="Deprecated equivalent to the "
                                           "ordered_go_to_Command")
    def ordered_move_to_Command(self,
                                dest_pos: Position,
                                prioritized_dir: AxisBoolThreeD) -> Runner:
        """
        Deprecated equivalent to the ordered_go_to_Command.

        This function is deprecated and will be removed in the future. Use the
        ordered_go_to_Command instead.

        :param dest_pos: The destination position
        :param prioritized_dir: Which directions to prioritize
        :return: A Runner performing the command
        """
        logging.warning("You are using a deprecated version of the "
                        "ordered_go_to_Command. Please update your usages "
                        "since this function might be removed in the future.")
        return self.ordered_go_to_Command(dest_pos, prioritized_dir)

    @protocol_command_function(description="Go to the specified position, moving the prioritized axes first and then the others.")
    def ordered_go_to_Command(self, dest_pos: Position, prioritized_dir: AxisBoolThreeD) -> Runner:
        """
        Go to the specified position, moving the prioritized axes first and
        then the others.

        :return: A Runner performing the command
        """
        config = self.get_current_config()
        runner = self.create_runner()

        # Initialized if needed
        if not self.isInitialized:
            runner.then_run_runner_from(self.setup_Command, info="setup")
            # assume default config since that is where the gripper will be
            # after setup
            config = VacuumGripperConfig()

        # Move to the destination along the prioritized axis
        if prioritized_dir.horizontal:
            config.horizontal_axis_config = AxisConfig.to_counter_goal(
                dest_pos.horizontal)
        if prioritized_dir.vertical:
            config.vertical_axis_config = AxisConfig.to_counter_goal(
                dest_pos.vertical)
        if prioritized_dir.rot:
            config.rotation_axis_config = AxisConfig.to_counter_goal(
                dest_pos.rot)
        logging.debug(f"prioritizing according to {prioritized_dir}: {config}")
        runner.then_goto(config, info="Moving according to priority")

        # Move to destination along the remaining axis
        config = self.config_from_target_pos(dest_pos)
        config.gripper_active = self.vacuumActValve # copy other properties
        runner.then_goto(config, info="Moving to dest position")

        return runner
