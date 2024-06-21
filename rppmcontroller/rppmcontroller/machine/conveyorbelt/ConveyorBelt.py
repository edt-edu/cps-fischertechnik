from rppmcontroller.machine.MovingMachine import MovingMachine
from rppmcontroller.machine.conveyorbelt.ConveyorBeltConfig import ConveyorBeltConfig
from rppmcontroller.machine.RequestedParameter import RequestedParameter
from math import isclose



class ConveyorBelt(MovingMachine):

    
    def __init__(self, id1):
        self.__conveyorSensFeed = self.__conveyorSensSwap = False
        dictMap = {RequestedParameter.LIGHTBARRIERFEEDSTATION: self.__conveyorSensFeed,
                   RequestedParameter.LIGHTBARRIERSWAPSTATION: self.__conveyorSensSwap}
        super().__init__(id1, dictMap)


    @property
    def conveyorSensFeed(self):
        return self.__conveyorSensFeed

    @conveyorSensFeed.setter
    def conveyorSensFeed(self, value):
        self.__conveyorSensFeed = value

    @property
    def conveyorSensSwap(self):
        return self.__conveyorSensSwap

    @conveyorSensSwap.setter
    def conveyorSensSwap(self, value):
        self.__conveyorSensSwap = value

    def sensorStatusString(self) -> str:
        return f"[{self.conveyorSensFeed}, {self.conveyorSensSwap}]"