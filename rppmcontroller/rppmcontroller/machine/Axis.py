from __future__ import annotations

import logging
from enum import Enum
from typing import Tuple, Optional

from rppmcontroller.machine.AxisConfig import AxisConfig
from rppmcontroller.machine.MachineParameters import AxisParameters, \
    PIDParameters, PWMParameters
from rppmcontroller.machine.MaxValueExceededError import MaxValueExceededError
from rppmcontroller.machine.PIDController import DiscretePIDController
from rppmcontroller.utils.Counter import Counter
from rppmcontroller.utils.ImpulseCounter import ImpulseCounter
from rppmcontroller.utils.PlusMinusStop import PlusMinusStop

_DEFAULT_PWM_VALUE = 100


class AxisType(Enum):
    """used to differentiate between axis using impulse counters and encoders"""
    Counter = 1
    Encoder = 2


class Axis:

    @staticmethod
    def encoder(parameters: AxisParameters) -> Axis:
        """
        Create a new Encoder type axis based on the given parameters
        :param parameters: The parameters to configure the axis
        :return: The created axis
        """
        tolerance = parameters.tolerance if parameters.tolerance is not None else 0
        pid_controller = DiscretePIDController.from_opt_pid_parameters(
            parameters.control_parameters) if isinstance(parameters.control_parameters,
                                                         PIDParameters) else None
        pwm_control = parameters.control_parameters if isinstance(parameters.control_parameters,
                                                                  PWMParameters) else None
        return Axis(axis_type=AxisType.Encoder,
                    tolerance=tolerance,
                    max_counter_value=parameters.max_counter_value,
                    pid_controller=pid_controller,
                    pwm_control=pwm_control)

    def __init__(self, axis_type: AxisType,
                 tolerance: int,
                 max_counter_value: int | None = None,
                 *,
                 pwm_standard_speed: int = 100,
                 pid_controller: DiscretePIDController | None = None,
                 pwm_control: PWMParameters | None = None):
        """
        constructor creates ImpulseCounter object if necessary
        :param axis_type: What kind of axis this is
        :param tolerance: How much tolerance the axis may have
        :param max_counter_value: If the counter exceeds this value, the axis
        :param pwm_standard_speed: How fast the axis should move normally
        :param pid_controller: A PID controller to use for this axis
        :param pwm_control: How the pid output should behave - will be ignored when a pid controller is used
        will not move to higher counter-values
        """
        self.__type = axis_type
        self._counter = ImpulseCounter() if axis_type == AxisType.Counter else Counter()
        self.__tolerance = tolerance
        self.__first = True
        self.__standard_speed = pwm_standard_speed
        self.__pwm_control = pwm_control
        self.__max_counter_value = max_counter_value
        self.__pid_controller = pid_controller

        # current input state
        self.__at_ref_switch: bool = False
        """Whether the axis has reached and pressed the ref-switch"""
        self.__counter_input: int = 0
        """The current value of the counter"""

        # outputs
        self.__output_plus = False
        """Whether the axis should move to larger values, away from the ref-switch"""
        self.__output_minus = False
        """Whether the axis should move to smaller values, towards the ref-switch"""
        self.__pwm_output: int = pwm_standard_speed
        """How quickly the axis should move, 100 means 100%. Most motors need at least 15 or similar to move at all."""

        # play values; those and the tolerances are fully ignored, when a pid controller is used.
        if axis_type == AxisType.Encoder:
            self.play = 8
        else:
            self.play = 2

    @property
    def counterValueCurrent(self):
        return self._counter.counter

    @counterValueCurrent.setter
    def counterValueCurrent(self, value):
        self._counter.counter = value

    @property
    def endpos(self):
        return self.__at_ref_switch

    @property
    def counterinput(self):
        return self.__counter_input

    @property
    def outputplus(self):
        return self.__output_plus

    @property
    def outputminus(self):
        return self.__output_minus

    @property
    def pwm_output(self) -> int:
        return self.__pwm_output

    @property
    def tolerance(self) -> int:
        return self.__tolerance + self.play

    def update(self, at_end_pos: bool, current_counter_input: int) -> None:
        """
        Informs this Axis about the current values

        :param at_end_pos: Whether the Axis has reached its end-switch
        :param current_counter_input: The current counter value
        :return: None
        """
        self.__at_ref_switch = at_end_pos
        self.__counter_input = current_counter_input

    # we might want to make this method non-static in the future
    # noinspection PyMethodMayBeStatic
    def howtoCounterPos(self,
                        counterGoal: int,
                        counterCurrent: int,
                        tolerance: int) -> PlusMinusStop:
        """method to determine which way the axis needs to rotate"""

        # sanitize counterGoal input
        if counterGoal < 0:
            counterGoal = 0

        if counterGoal > counterCurrent + tolerance:
            return PlusMinusStop.PLUS

        # "handle" overflow
        # assume overflow if counter greater than 4 million
        if counterGoal < counterCurrent - tolerance and counterCurrent > 4E6:
            logging.debug('handled overflow')
            return PlusMinusStop.PLUS

        if counterGoal < counterCurrent - tolerance:
            return PlusMinusStop.MINUS

        return PlusMinusStop.STOP

    def config_would_exceed_max_counter_value(self, axis_config: AxisConfig) \
        -> Optional[MaxValueExceededError]:
        """
        Checks whether the counter-goal specified by the config would exceed
        the maximum counter-value

        Example:
        .. code-block:: python

            error = axis.config_would_exceed_max_counter_value(config)
            if error:  # `error` itself will always evaluate to `True`
                raise error

        :param axis_config: An AxisConfig specifying where to move to
        :return: A `MaxValueExceededError` if the goal exceeds the maximum
            counter-value, otherwise `None`
        """
        return self.goal_would_exceed_max_counter_value(
            axis_config.counter_goal)

    def goal_would_exceed_max_counter_value(self, counter_goal: int) \
        -> Optional[MaxValueExceededError]:
        """
        Checks whether the specified counter-goal would exceed the maximum
        counter-value

        Example:
        .. code-block:: python

            error = axis.goal_would_exceed_max_counter_value(4000)
            if error:  # `error` itself will always evaluate to `True`
                raise error

        :param counter_goal: A counter-goal to check
        :return: A `MaxValueExceededError` if the goal exceeds the maximum
            counter-value, otherwise `None`
        """
        return MaxValueExceededError(counter_goal, self.__max_counter_value) \
            if self.__max_counter_value is not None and counter_goal > self.__max_counter_value \
            else None

    def gotoAxisConfig(self, axis_config: AxisConfig) -> bool | Tuple[
        bool, PlusMinusStop | None]:
        """
        Set the outputs to move towards the specified axis_config

        :param axis_config: An AxisConfig specifying where to move to
        :return: True if the goal specified by the config has been reached. If the internal counter is a pulse counter, then the direction of that is also returned.
        """
        return self.gotoConfig(axis_config.end_position,
                               axis_config.counter_goal)

    def gotoConfig(self, go_to_ref_switch: bool, counter_goal: int) -> bool | \
                                                                       Tuple[
                                                                           bool, PlusMinusStop | None]:
        """
        Set outputs to reach the wanted counter-goal for that axis.

        Note: If you set `counter_goal` to `0,` this method will behave exactly
        as when `end_pos` is `True`.

        :param go_to_ref_switch: Whether to move to the end-position of the axis
        :param counter_goal: The counter-position to move to. Will be ignored
        if end_pos is `True`
        :return: `True` if the target has been reached. If the internal counter
        is an ImpulseCounter, the direction of the movement is returned as
         a second parameter
        """
        # To improve precision, consider a move to 0 as a move to
        # ref-switch
        # -> there are checks in place that prevent moving beyond a
        #   ref-switch
        if counter_goal <= 0:
            go_to_ref_switch = True
            # this value should not be used when the above value is True, but
            # let's set it to 0 anyway, just to be sure
            counter_goal = 0

        if go_to_ref_switch:
            target_reached = self.go_to_ref_switch()
        else:
            target_reached = self._go_to_positive_counter_value(counter_goal)

        if isinstance(self._counter, ImpulseCounter):
            return target_reached, self.calculate_direction()
        return target_reached

    def _go_to_positive_counter_value(self, counter_goal: int) -> bool:
        """
        Moves this axis to a given positive counter value.

        It is the callers responsibility to ensure that the counter goal is
        positive.
        :param counter_goal: A positive counter value to reach
        :return: Whether that counter value is reached
        """
        assert counter_goal > 0

        self._update_current_counter_value()

        target_reached = self.__set_output_values_for_target(counter_goal)
        if target_reached:
            # no further break checks needed
            return True

        # extra check to make sure we are not telling the hardware to
        # move beyond a ref-switch
        is_at_ref_switch = self.__at_ref_switch
        moving_towards_ref_switch = self.outputminus
        if is_at_ref_switch and moving_towards_ref_switch:
            self.reset_direction()
            logging.warning("Prevented moving beyond ref-switch")
            return True

        # extra check to make sure we are not moving further beyond the
        # max counter-value
        max_counter_value_exceeded = self.__max_counter_value is not None and self._counter.counter > self.__max_counter_value
        moving_towards_greater_counter_values = self.outputplus
        if max_counter_value_exceeded and moving_towards_greater_counter_values:
            self.reset_direction()
            logging.warning("Prevented moving beyond max counter value")
            return True

        return False

    def __set_output_values_for_target(self, counter_goal: int) -> bool:
        pid_controller = self.__pid_controller
        if pid_controller is not None:
            logging.debug("Using PID controller")
            if counter_goal != pid_controller.target.get():
                pid_controller.reset()
                pid_controller.target.set(counter_goal)

            logging.debug(f"PID processing: {self.counterValueCurrent} -> {counter_goal}")
            pwm = pid_controller.process(self.counterValueCurrent)
            logging.debug(f"PID output: {pwm}")
            self.__pwm_output = abs(pwm)
            self.__output_plus = pwm > 0
            self.__output_minus = pwm < 0
            # don't reset pid here since pid state might be useful for future iterations
            return pwm != 0
        logging.debug("Using counter pos logic")

        counterPos = self.howtoCounterPos(counter_goal,
                                          self.counterValueCurrent,
                                          self.tolerance)
        self.__output_plus = counterPos is PlusMinusStop.PLUS
        self.__output_minus = counterPos is PlusMinusStop.MINUS
        self.__pwm_output = self.get_pwm_speed_to_approach(AxisConfig.to_counter_goal(
            counter_goal))
        return counterPos is PlusMinusStop.STOP

    def go_to_ref_switch(self) -> bool:
        """
        Sets the output of this axis to move towards the reference switch
        :return: Whether the reference switch is reached
        """
        if self.__at_ref_switch:
            self.reset_direction()
            return True

        # update outputs
        # the ref-switch resets the counter value -> always move at full
        # speed so we actually hit it in the same spot for better accuracy
        self.__output_minus = True
        self.__output_plus = False
        self.__pwm_output = self.__standard_speed

        # update internal values
        self._update_current_counter_value()

        return False

    def _update_current_counter_value(self) -> None:
        """
        Overrides the counter value of the _counter based on the current
        __counter_input.

        For encoder axes, this will be a 1:1 mapping. For impulse counter axes,
        a direction based computation will be performed.
        The direction used in that case will be determined by the current outputs.
        :return: None
        """
        if not isinstance(self._counter, ImpulseCounter):
            self.counterValueCurrent = self.__counter_input
            return

        # special handling for counter type axis TODO this case could be solved via inheritance
        direction = self.calculate_direction()
        if direction == PlusMinusStop.STOP:
            return

        self.counterValueCurrent = self._counter.compute(self.__counter_input,
                                                         direction)
        logging.debug(self.counterValueCurrent)

        # impulse counters usually rely on the previous direction, which
        # might be nonsense in some edge cases
        self.__reset_counter_if_needed()

    def __reset_counter_if_needed(self) -> None:
        """
        Resets the current counter value to zero on the first call of this
        method or when the ref-switch is reached.
        :return: None
        """
        if not self.__first and not self.endpos:
            return

        self.__first = False
        logging.debug("Reset counter here here here here here here")
        self.counterValueCurrent = 0

    def calculate_direction(self) -> PlusMinusStop:
        """
        Calculates the direction the axis is currently moving in based on
        the current outputs
        :return: The direction the axis is currently moving in
        """
        if self.outputplus == self.outputminus:
            return PlusMinusStop.STOP

        return PlusMinusStop.PLUS if self.outputplus else PlusMinusStop.MINUS

    def isCloseFromEnd(self,
                       axis_config: AxisConfig,
                       approachTol: int) -> bool:
        targetPos = axis_config.counter_goal
        counterCurrent = self._counter.counter

        if targetPos - approachTol > counterCurrent:
            return False
        elif targetPos + approachTol < counterCurrent:
            return False
        else:
            return True

    def get_pwm_speed_to_approach(self, target_position: AxisConfig) -> int:
        """
        Calculates the PWM speed to approach a given target position from the
        current position of the axis
        :param target_position: A position or configuration to approach
        :return: The pwm speed to use in the approach
        """
        if self.__pwm_control is not None and not target_position.to_end_position() and self.isCloseFromEnd(
            target_position,
            self.__pwm_control.approach_tolerance):
            return self.__pwm_control.approach_speed

        return self.__standard_speed

    def reset_direction(self) -> None:
        self.__output_minus = False
        self.__output_plus = False
        self.__pwm_output = self.__standard_speed

        if self.__pid_controller is not None:
            self.__pid_controller.reset()
