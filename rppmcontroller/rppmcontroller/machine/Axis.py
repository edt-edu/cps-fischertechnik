import logging
from enum import Enum
from typing import Union, Tuple, Optional

from rppmcontroller.machine.AxisConfig import AxisConfig
from rppmcontroller.utils.Counter import Counter
from rppmcontroller.utils.ImpulseCounter import ImpulseCounter
from rppmcontroller.utils.PlusMinusStop import PlusMinusStop


class AxisType(Enum):
    """used to differentiate between axis using impulse counters and encoders"""
    Counter = 1
    Encoder = 2

class Axis:

    def __init__(self, axis_type: AxisType,
                 tolerance: int,
                 max_counter_value: Optional[int] = None):
        """
        constructor creates ImpulseCounter object if necessary
        :param axis_type: What kind of axis this is
        :param tolerance: How much tolerance the axis may have
        :param max_counter_value: If the counter exceeds this value, the axis
        will not move to higher counter-values
        """
        self.__type = axis_type
        if axis_type == AxisType.Counter:
            self.__counter = ImpulseCounter()
        else:
            self.__counter = Counter()
        self.__tolerance = tolerance
        self.__first = True
        #variables need to be manually updated/written
        self.__end_pos: bool = False
        self.__counter_input: int = 0
        self.__output_plus = False
        self.__output_minus = False
        if axis_type == AxisType.Encoder:
            self.play = 8
        else:
            self.play = 2
        self.__max_counter_value = max_counter_value

    @property
    def counterValueCurrent(self):
        return self.__counter.counter

    @counterValueCurrent.setter
    def counterValueCurrent(self, value):
        self.__counter.counter = value

    @property
    def endpos(self):
        return self.__end_pos

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
    def tolerance(self) -> int:
        return self.__tolerance + self.play

    def update(self, end_pos: bool, counter_input: int) -> None:
        """
        Informs this Axis about the current values

        :param end_pos: Whether the Axis has reached its end-switch
        :param counter_input: The current counter value
        :return: None
        """
        self.__end_pos = end_pos
        self.__counter_input = counter_input

    # we might want to make this method non-static in the future
    # noinspection PyMethodMayBeStatic
    def howtoCounterPos(self, counterGoal, counterCurrent, tolerance):
        """method to determine which way the axis needs to rotate"""
        #"handle" overflow
        #assume overflow if counter greater than 4 million
        if counterGoal < 0:
            counterGoal = 0
        if counterGoal > counterCurrent + tolerance:
            return PlusMinusStop.PLUS
        elif counterGoal < counterCurrent - tolerance and counterCurrent > 4E6:
            logging.debug('handled overflow')
            return PlusMinusStop.PLUS
        elif counterGoal < counterCurrent - tolerance:
            return PlusMinusStop.MINUS
        else:
            return PlusMinusStop.STOP


    def gotoAxisConfig(self, axis_config: AxisConfig) -> bool:
        """
        Set the outputs to move towards the specified axis_config

        :param axis_config: An AxisConfig specifying where to move to
        :return: True if the goal specified by the config has been reached. If the internal counter is a pulse counter, then the direction of that is also returned.
        """
        return self.gotoConfig(axis_config.end_position, axis_config.counter_goal)

    def gotoConfig(self, end_pos: bool, counter_goal: int) -> Union[bool, Tuple[bool, Optional[PlusMinusStop]]]:
        """
        Set outputs to reach the wanted counter goal for that axis.

        Note: If you set `counter_goal` to `0` this method will behave exactly
        as when `end_pos` is `True`.

        :param end_pos: Whether to move to the end-position of the axis
        :param counter_goal: The counter position to move to. Will be ignored
        if end_pos is `True`
        :return: `True` if the target has been reached. If the internal counter
        is an ImpulseCounter, the direction of the movement is returned as
        second parameter
        """
        # In order to improve precision, consider a move to 0 as a move to
        # ref-switch
        # -> there are checks in place which prevent moving beyond a
        #   ref-switch
        if counter_goal <= 0:
            end_pos = True

        target_reached = False
        direction = None
        #if you want to use the limit switch always set up counterGoal
        if end_pos:
            if not self.__end_pos:
                self.__output_minus = True
                self.__output_plus = False
                if isinstance(self.__counter, ImpulseCounter):
                    self.__counter.counter = self.__counter.compute(self.__counter_input, PlusMinusStop.MINUS)
                    logging.debug(self.__counter.counter)
                direction = PlusMinusStop.MINUS
            else:
                self.__output_minus = False
                self.__output_plus = False
                target_reached = True
        else:
            #calls compute methods for axis with impulse counters based on (previous) motor direction, not necessary for encoder
            if isinstance(self.__counter, ImpulseCounter):
                if self.outputplus:
                    self.__counter.counter = self.__counter.compute(self.__counter_input, PlusMinusStop.PLUS)
                    logging.debug(self.__counter.counter)
                    #print("compute counter")
                    if self.__first or self.endpos:
                        self.__first = False
                        logging.debug("Reset arm counter here here here here here here")
                        self.__counter.counter = 0
                elif self.outputminus:
                    self.__counter.counter = self.__counter.compute(self.__counter_input, PlusMinusStop.MINUS)
                    print(self.__counter.counter)
                    if self.__first or self.endpos:
                        self.__first = False
                        logging.debug("Reset arm counter here here here here here here")
                        self.__counter.counter = 0

            else:
                self.__counter.counter = self.__counter_input

            counterPos = self.howtoCounterPos(counter_goal, self.__counter.counter, self.tolerance)
            # logging.debug(f'howtoCounterPos({counterGoal}, {self.__counter.counter}, {self.tolerance})={counterPos}')
            if counterPos == PlusMinusStop.PLUS:
                self.__output_minus = False
                self.__output_plus = True
                direction = PlusMinusStop.PLUS
            elif counterPos == PlusMinusStop.MINUS:
                self.__output_minus = True
                self.__output_plus = False
                direction = PlusMinusStop.MINUS
            elif counterPos == PlusMinusStop.STOP:
                self.__output_minus = False
                self.__output_plus = False
                target_reached = True
                direction = PlusMinusStop.STOP

            # extra check to make sure we are not telling the hardware to
            # move beyond a ref-switch
            ref_switch_reached = self.__end_pos
            mov_to_ref_switch = self.outputminus
            if ref_switch_reached and mov_to_ref_switch:
                self.__output_minus = False
                self.__output_plus = False
                direction = PlusMinusStop.STOP
                target_reached = True

            # extra check to make sure we are not moving further beyond the
            # max counter-value
            max_counter_value_exceeded = self.__max_counter_value is not None and self.__counter.counter > self.__max_counter_value
            mov_to_greater_counter_values = self.outputplus
            if max_counter_value_exceeded and mov_to_greater_counter_values:
                self.__output_minus = False
                self.__output_plus = False
                direction = PlusMinusStop.STOP
                target_reached = True

        if isinstance(self.__counter, ImpulseCounter):
            return target_reached, direction
        return target_reached

    def isCloseFromEnd(self, axis_config: AxisConfig, approachTol: int) -> bool:
        targetPos = axis_config.counter_goal
        counterCurrent = self.__counter.counter

        if targetPos - approachTol > counterCurrent:
            return False
        elif targetPos + approachTol < counterCurrent:
            return False
        else:
            return True

    def resetDirection(self) -> None:
        self.__output_minus = False
        self.__output_plus = False


class MaxValueExceededError(Exception):
    """
    Indicates that a move to a specific configuration would exceed the
    maximum counter-value
    """

    def __init__(self, requested_counter_value: int, max_counter_value: int):
        self.__requested_counter_value = requested_counter_value
        self.__max_counter_value = max_counter_value

    @property
    def message(self) -> str:
        return (f"Movement to counter-value {self.__requested_counter_value} "
                f"would exceed maximum counter-value of {self.__max_counter_value}")

    def __bool__(self):
        """
        Evaluates to `True` so that this can be used in an if-statement
        :return: `True`
        """
        return True

    def __str__(self):
        return self.message
