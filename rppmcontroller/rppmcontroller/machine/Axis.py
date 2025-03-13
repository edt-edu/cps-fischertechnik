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

    def __init__(self, typ: AxisType, tolerance):
        """constructor creates ImpulseCounter object if necessary"""
        self.__type = typ
        if typ == AxisType.Counter:
            self.__counter = ImpulseCounter()
        else:
            self.__counter = Counter()
        self.__tolerance = tolerance
        self.__first = True
        #variables need to be manually updated/written
        self.__endpos = 0
        self.__counterinput = 0
        self.__outputplus = 0
        self.__outputminus = 0
        if typ == AxisType.Encoder:
            self.play = 10
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

    def update(self, endpos, counterinput):
        self.__endpos = endpos
        self.__counterinput = counterinput

    def howtoCounterPos(self, counterGoal, counterCurrent, tolerance):
        """method to determine which way the axis needs to rotate"""
        #TODO probably set a different play for impulse counters(vllt 2) vs encoder counters (vllt 10)
        #"handle" overflow
        #assume overflow if counter greater 4 millions
        if counterGoal < 0:
            counterGoal = 0
        if counterGoal > counterCurrent + tolerance:
            return PlusMinusStop.PLUS
        elif counterGoal < counterCurrent - tolerance - self.play and counterCurrent > 4000000:
            logging.debug('handeled overflow')
            return PlusMinusStop.PLUS
        elif counterGoal < counterCurrent - tolerance - self.play:
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

    def gotoConfig(self, endpos: bool, counterGoal: int) -> bool | (bool, PlusMinusStop):
        """method to set outputs to reach the wanted config goal for that axis"""
        t = False
        d = None
        #if you want to use the limit switch always set up counterGoal
        if endpos:
            if not self.__endpos:
                self.__outputminus = True
                if isinstance(self.__counter, ImpulseCounter):
                    self.__counter.counter = self.__counter.compute(self.__counterinput, PlusMinusStop.MINUS)
                    logging.debug(self.__counter.counter)
                d = PlusMinusStop.MINUS
            else:
                self.__outputminus = False
                t = True
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
            counterPos = self.howtoCounterPos(counterGoal, self.__counter.counter, self.__tolerance)
            # logging.debug(f'howtoCounterPos({counterGoal}, {self.__counter.counter}, {self.__tolerance})={counterPos}')
            if counterPos == PlusMinusStop.PLUS:
                self.__outputminus = False
                self.__outputplus = True
                d = PlusMinusStop.PLUS
            elif counterPos == PlusMinusStop.MINUS:
                self.__outputminus = True
                self.__outputplus = False
                d = PlusMinusStop.MINUS
            elif counterPos == PlusMinusStop.STOP:
                self.__outputminus = False
                self.__outputplus = False
                t = True
        if isinstance(self.__counter, ImpulseCounter):
            return t, d
        return t
