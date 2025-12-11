from typing import Union, Tuple, Optional

import logging
from enum import Enum

from rppmcontroller.machine.AxisConfig import AxisConfig
from rppmcontroller.utils.Counter import Counter
from rppmcontroller.utils.ImpulseCounter import ImpulseCounter
from rppmcontroller.utils.PlusMinusStop import PlusMinusStop


class AxisType(Enum):
    """used to differentiate between axis using impulse counters and encoders"""
    Counter = 1
    Encoder = 2

#TODO ensure no negative values are accepted for counter goal
class Axis:

    def __init__(self, axis_type: AxisType, tolerance: int):
        """constructor creates ImpulseCounter object if necessary"""
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
        self.__outputplus = False
        self.__outputminus = False
        if axis_type == AxisType.Encoder:
            self.play = 8
        else:
            self.play = 2

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
        return self.__outputplus

    @property
    def outputminus(self):
        return self.__outputminus

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
        #TODO probably set a different play for impulse counters(maybe 2) vs encoder counters (maybe 10)
        #"handle" overflow
        #assume overflow if counter greater 4 millions
        if counterGoal < 0:
            counterGoal = 0
        if counterGoal > counterCurrent + tolerance:
            return PlusMinusStop.PLUS
        elif counterGoal < counterCurrent - tolerance and counterCurrent > 4000000:
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

        Note: If you set counter goal to `0` this method will behave exactly
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
        if end_pos <= 0:
            end_pos = True

        target_reached = False
        direction = None
        #if you want to use the limit switch always set up counterGoal
        if end_pos:
            if not self.__end_pos:
                self.__outputminus = True
                self.__outputplus = False
                if isinstance(self.__counter, ImpulseCounter):
                    self.__counter.counter = self.__counter.compute(self.__counter_input, PlusMinusStop.MINUS)
                    logging.debug(self.__counter.counter)
                direction = PlusMinusStop.MINUS
            else:
                self.__outputminus = False
                self.__outputplus = False
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
                self.__outputminus = False
                self.__outputplus = True
                direction = PlusMinusStop.PLUS
            elif counterPos == PlusMinusStop.MINUS:
                self.__outputminus = True
                self.__outputplus = False
                direction = PlusMinusStop.MINUS
            elif counterPos == PlusMinusStop.STOP:
                self.__outputminus = False
                self.__outputplus = False
                target_reached = True
                direction = PlusMinusStop.STOP

            # extra check to make sure we are not telling the hardware to
            # move beyond a ref-switch
            ref_switch_reached = self.__end_pos
            movToRefSwitch = self.outputminus
            if ref_switch_reached and movToRefSwitch:
                self.__outputminus = False
                self.__outputplus = False
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
        self.__outputminus = False
        self.__outputplus = False
