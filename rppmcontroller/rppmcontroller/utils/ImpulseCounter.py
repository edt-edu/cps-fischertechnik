from rppmcontroller.utils.Counter import Counter
from rppmcontroller.utils.PlusMinusStop import PlusMinusStop
import logging


class ImpulseCounter(Counter):

    def __init__(self):
        super().__init__()
        self.counter = 0
        self.__dirForward = self.__dirBackward = False
        self.__numalt = 0

    @property
    def dirForward(self):
        return self.__dirForward

    @property
    def dirBackward(self):
        return self.__dirBackward

    def compute(self, num, direction):
        num = int(num) #in case of bad typing, the fucntion may receive a boolean
        if direction == PlusMinusStop.PLUS:
            self.counter += (num - self.__numalt)
        elif direction == PlusMinusStop.MINUS:
            self.counter -= (num - self.__numalt)
        self.__numalt = num
        logging.debug("counter " + str(self.counter))
        return self.counter