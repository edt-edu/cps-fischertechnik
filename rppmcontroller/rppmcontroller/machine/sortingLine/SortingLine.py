import logging

from rppmcontroller.machine.Machine import Machine
from rppmcontroller.utils.ImpulseCounter import ImpulseCounter
from rppmcontroller.utils.PlusMinusStop import PlusMinusStop
from rppmcontroller.machine.Colour import Colour
from rppmcontroller.machine.RequestedParameter import RequestedParameter
from rppmcontroller.utils.CyclicWaiter import CyclicWaiter


class SortingLine(Machine):

    @property
    def isExecuting(self) -> bool:
        logging.debug('is executing ' + str(self.__packageOnLine))
        return self.__packageOnLine

    def __init__(self, id1: str):
        self.current = 0
        self.__sortingLineSensImpulseCounterRaw = 0
        self.__sortingLineSensInputLightBarrier = self.__sortingLineSensMiddleLightBarrier = self.__sortingLineSensWhiteLightBarrier = self.__sortingLineSensBlueLightBarrier = self.__sortingLineSensRedLightBarrier = True
        self.__sortingLineActMotorConveyor = self.__sortingLineActCompressorOn = self.__sortingLineActWhiteEjector = self.__sortingLineActRedEjector = self.__sortingLineActBlueEjector = False
        self.__counter = ImpulseCounter()
        dictMap = {RequestedParameter.PULSECOUNTER: self.__counter.counter,
                   RequestedParameter.LIGHTBARRIERINLET: self.__sortingLineSensInputLightBarrier,
                   RequestedParameter.LIGHTBARRIERBEHINDCOLORSENSOR: self.__sortingLineSensMiddleLightBarrier,
                   RequestedParameter.LIGHTBARRIERWHITE: self.__sortingLineSensWhiteLightBarrier,
                   RequestedParameter.LIGHTBARRIERRED: self.__sortingLineSensRedLightBarrier,
                   RequestedParameter.LIGHTBARRIERBLUE: self.__sortingLineSensBlueLightBarrier,
                   RequestedParameter.MOTORCONVEYORBELT: self.__sortingLineActMotorConveyor,
                   RequestedParameter.COMPRESSOR: self.__sortingLineActCompressorOn,
                   RequestedParameter.VALVEFIRSTEJECTORWHITE: self.__sortingLineActWhiteEjector,
                   RequestedParameter.VALVESECONDEJECTORRED: self.__sortingLineActRedEjector,
                   RequestedParameter.VALVETHIRDEJECTORBLUE: self.__sortingLineActBlueEjector}
        super().__init__(id1, dictMap)

        self.__packageOnLine = self.__packageCountSteps = False

    @property
    def sortingLineSensImpulseCounterRaw(self):
        return self.__sortingLineSensImpulseCounterRaw

    @sortingLineSensImpulseCounterRaw.setter
    def sortingLineSensImpulseCounterRaw(self, value):
        self.__sortingLineSensImpulseCounterRaw = value

    @property
    def sortingLineSensInputLightBarrier(self):
        return self.__sortingLineSensInputLightBarrier

    @sortingLineSensInputLightBarrier.setter
    def sortingLineSensInputLightBarrier(self, value):
        self.__sortingLineSensInputLightBarrier = value

    @property
    def sortingLineSensMiddleLightBarrier(self):
        return self.__sortingLineSensMiddleLightBarrier

    @sortingLineSensMiddleLightBarrier.setter
    def sortingLineSensMiddleLightBarrier(self, value):
        self.__sortingLineSensMiddleLightBarrier = value

    @property
    def sortingLineSensWhiteLightBarrier(self):
        return self.__sortingLineSensWhiteLightBarrier

    @sortingLineSensWhiteLightBarrier.setter
    def sortingLineSensWhiteLightBarrier(self, value):
        self.__sortingLineSensWhiteLightBarrier = value

    @property
    def sortingLineSensRedLightBarrier(self):
        return self.__sortingLineSensRedLightBarrier

    @sortingLineSensRedLightBarrier.setter
    def sortingLineSensRedLightBarrier(self, value):
        self.__sortingLineSensRedLightBarrier = value

    @property
    def sortingLineSensBlueLightBarrier(self):
        return self.__sortingLineSensBlueLightBarrier

    @sortingLineSensBlueLightBarrier.setter
    def sortingLineSensBlueLightBarrier(self, value):
        self.__sortingLineSensBlueLightBarrier = value

    @property
    def sortingLineActMotorConveyor(self):
        return self.__sortingLineActMotorConveyor

    @sortingLineActMotorConveyor.setter
    def sortingLineActMotorConveyor(self, value):
        self.__sortingLineActMotorConveyor = value

    @property
    def sortingLineActCompressorOn(self):
        return self.__sortingLineActCompressorOn

    @sortingLineActCompressorOn.setter
    def sortingLineActCompressorOn(self, value):
        self.__sortingLineActCompressorOn = value

    @property
    def sortingLineActWhiteEjector(self):
        return self.__sortingLineActWhiteEjector

    @sortingLineActWhiteEjector.setter
    def sortingLineActWhiteEjector(self, value):
        self.__sortingLineActWhiteEjector = value

    @property
    def sortingLineActRedEjector(self):
        return self.__sortingLineActRedEjector

    @sortingLineActRedEjector.setter
    def sortingLineActRedEjector(self, value):
        self.__sortingLineActRedEjector = value

    @property
    def sortingLineActBlueEjector(self):
        return self.__sortingLineActBlueEjector

    @sortingLineActBlueEjector.setter
    def sortingLineActBlueEjector(self, value):
        self.__sortingLineActBlueEjector = value

    @property
    def sortingLineCounterValue(self):
        return self.__counter.counter

    def startOfProcess(self, packageIncoming):
        if not self.__sortingLineSensInputLightBarrier and not self.__packageOnLine:
            self.__packageOnLine = True
            print("packageOnLine True")
        if self.__packageOnLine or packageIncoming:
            self.__packageOnLine = True
            print("packageOnLine True")
            self.__sortingLineActMotorConveyor = True
            if not self.__sortingLineSensMiddleLightBarrier:
                logging.debug('set count steps true')
                self.__packageCountSteps = True

    def eject(self, colour: Colour):
        logging.debug('eject loop ' + str(colour))
        whiteCounter = 1
        redCounter = 3
        blueCounter = 5
        self.current = self.current + self.__counter.compute(self.__sortingLineSensImpulseCounterRaw, PlusMinusStop.PLUS)
        logging.debug(f"NOMBRE PAS : {self.current}")
        if not self.__packageCountSteps :
            self.startOfProcess(True)
            if not self.__packageCountSteps:
                self.current = 0
        else:
            if self.current > blueCounter and colour == Colour.BLUE:
                self.__sortingLineActMotorConveyor = False
                self.__sortingLineActCompressorOn = True
                self.__sortingLineActBlueEjector = True
                if not self.__sortingLineSensBlueLightBarrier:
                    self.__packageOnLine = self.__packageCountSteps = False
                    print("packageOnLine False")
                    self.__sortingLineActCompressorOn = False
                    self.__sortingLineActBlueEjector = False
                    #return
            if self.current > redCounter and colour == Colour.RED:
                self.__sortingLineActMotorConveyor = False
                self.__sortingLineActCompressorOn = True
                self.__sortingLineActRedEjector = True
                if not self.__sortingLineSensRedLightBarrier:
                    self.__packageOnLine = self.__packageCountSteps= False
                    print("packageOnLine False")
                    self.__sortingLineActCompressorOn = False
                    self.__sortingLineActRedEjector = False
                    #return
            if self.current > whiteCounter and colour == Colour.WHITE:
                self.__sortingLineActMotorConveyor = False
                self.__sortingLineActCompressorOn = True
                self.__sortingLineActWhiteEjector = True
                if not self.__sortingLineSensWhiteLightBarrier:
                    self.__packageOnLine = self.__packageCountSteps = False
                    print("packageOnLine False")
                    self.__sortingLineActCompressorOn = False
                    self.__sortingLineActWhiteEjector = False
                    #return
        return lambda: self.eject(colour)

    #TODO
    def stop(self):
        pass
