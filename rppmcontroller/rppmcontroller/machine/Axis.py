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

    def __init__(self, typ: AxisType, tolerance, endpos_is_at_low_counter_values = True):
        """constructor creates ImpulseCounter object if necessary"""
        self.__type = typ
        if typ == AxisType.Counter:
            self.__counter = ImpulseCounter()
        else:
            self.__counter = Counter()
        self.__tolerance = tolerance
        self.__first = True
        #variables need to be manually updated/written
        self.__endpos: bool = False
        self.__counterinput: int = 0
        self.__outputplus = False
        self.__outputminus = False
        if typ == AxisType.Encoder:
            self.play = 6
        else:
            self.play = 2
        self.__endpos_is_at_low_counter_values = endpos_is_at_low_counter_values

    @property
    def counterValueCurrent(self):
        return self.__counter.counter

    @counterValueCurrent.setter
    def counterValueCurrent(self, value):
        self.__counter.counter = value

    @property
    def endpos(self):
        return self.__endpos

    @property
    def counterinput(self):
        return self.__counterinput

    @property
    def outputplus(self):
        return self.__outputplus

    @property
    def outputminus(self):
        return self.__outputminus

    @property
    def tolerance(self) -> int:
        return self.__tolerance + self.play

    def update(self, endpos: bool, counterinput: int) -> None:
        """
        Informs this Axis about the current values
        :param endpos: Whether the Axis has reached its end-switch
        :param counterinput: The current counter value
        :return: None
        """
        self.__endpos = endpos
        self.__counterinput = counterinput

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
            logging.debug('handeled overflow')
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

    def gotoConfig(self, endpos: bool, counterGoal: int) -> Union[bool, Tuple[bool, Optional[PlusMinusStop]]]:
        """
        method to set outputs to reach the wanted config goal for that axis
        :param endpos: Whether to move to the end-position of the axis
        :param counterGoal: The counter position to move to. Will be ignored
        if endpos is True
        :return: True if the target has been reached. If the internal counter
        is an ImpulseCounter, the direction of the movement is returned as
        second parameter
        """
        target_reached = False
        direction = None
        #if you want to use the limit switch always set up counterGoal
        if endpos:
            if not self.__endpos:
                if self.__endpos_is_at_low_counter_values:
                    self.__outputminus = True
                else:
                    self.__outputplus = True
                if isinstance(self.__counter, ImpulseCounter):
                    self.__counter.counter = self.__counter.compute(self.__counterinput, PlusMinusStop.MINUS)
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
                    self.__counter.counter = self.__counter.compute(self.__counterinput, PlusMinusStop.PLUS)
                    logging.debug(self.__counter.counter)
                    #print("compute counter")
                    if self.__first or self.endpos:
                        self.__first = False
                        logging.debug("Reset arm counter here here here here here here")
                        self.__counter.counter = 0
                elif self.outputminus:
                    self.__counter.counter = self.__counter.compute(self.__counterinput, PlusMinusStop.MINUS)
                    print(self.__counter.counter)
                    if self.__first or self.endpos:
                        self.__first = False
                        logging.debug("Reset arm counter here here here here here here")
                        self.__counter.counter = 0

            else:
                self.__counter.counter = self.__counterinput
            counterPos = self.howtoCounterPos(counterGoal, self.__counter.counter, self.tolerance)
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
