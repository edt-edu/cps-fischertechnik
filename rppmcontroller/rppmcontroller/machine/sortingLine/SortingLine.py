#from Layout.Machine import Layout.Machine #why is this Layout.Machine???
import logging

from rppmcontroller.machine.Machine import Machine
from rppmcontroller.utils.ImpulseCounter import ImpulseCounter
from rppmcontroller.utils.PlusMinusStop import PlusMinusStop
from rppmcontroller.machine.Color import Color
from rppmcontroller.machine.RequestedParameter import RequestedParameter
from rppmcontroller.utils.CyclicWaiter import CyclicWaiter
from typing import Any, Callable, Dict, Optional
from typing_extensions import override


class SortingLine(Machine):

    #TODO self.once: implement reset possibility from execute


    def __isExecuting(self) -> bool:
        return self.__packageOnLine

    @Machine.isExecuting.getter
    def isExecuting(self) -> bool:
        res = self.__isExecuting()

        # log isexecuting and debug info only if message has changed
        isExecuting_log = f'isExecuting({self.id})={res} | Sensors={self.sensorStatusString()} | Actuators= {self.actuatorStatusString()}'
        if isExecuting_log != self.previous_isExecuting_log :
            logging.debug(isExecuting_log)
            self.previous_isExecuting_log = isExecuting_log

        return res
    
    @Machine.isCommandSuccessed.getter
    def isCommandSuccessed(self) -> bool:
        return self.__isCommandSuccessed

    @Machine.isCommandRunning.getter
    def isCommandRunning(self) -> bool:
        return self.__isCommandRunning
    
    @Machine.isCommandTimedOut.getter
    def isCommandTimedOut(self) -> bool:
        return self.__isCommandTimedOut

    def __init__(self, id1: str):
        self.__sortingLineSensImpulseCounterRaw = 0
        self.__sortingLineSensInputLightBarrier = self.__sortingLineSensMiddleLightBarrier = self.__sortingLineSensWhiteLightBarrier = self.__sortingLineSensBlueLightBarrier = self.__sortingLineSensRedLightBarrier = True
        self.__sortingLineActMotorConveyor = self.__sortingLineActCompressorOn = self.__sortingLineActWhiteEjector = self.__sortingLineActRedEjector = self.__sortingLineActBlueEjector = False
        self.__counter = ImpulseCounter()
        self.__isCommandSuccessed = False
        self.__isCommandRunning = False
        self.__isCommandTimedOut = False
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
        self.once = True

        self.previous_isExecuting_log = None # 

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

    def sensorStatusString(self) -> str:
        return f"[{self.sortingLineSensInputLightBarrier}, {self.sortingLineSensMiddleLightBarrier}], [{self.sortingLineSensWhiteLightBarrier}, {self.sortingLineSensBlueLightBarrier}, {self.sortingLineSensRedLightBarrier}], {self.sortingLineSensImpulseCounterRaw}"

    def actuatorStatusString(self) -> str:
        return f"{self.sortingLineActMotorConveyor}, {self.sortingLineActCompressorOn}, [{self.sortingLineActWhiteEjector}, {self.sortingLineActRedEjector}, {self.sortingLineActBlueEjector}]"
 

    def inputStatus(self) -> Dict[str, Any]:
        status = {
            "sortingLineSensInputLightBarrier": self.sortingLineSensInputLightBarrier,
            "sortingLineSensMiddleLightBarrier": self.sortingLineSensMiddleLightBarrier,
            "sortingLineSensWhiteLightBarrier": self.sortingLineSensWhiteLightBarrier,
            "sortingLineSensBlueLightBarrier": self.sortingLineSensBlueLightBarrier,
            "sortingLineSensRedLightBarrier": self.sortingLineSensRedLightBarrier,
            "sortingLineSensImpulseCounterRaw": self.sortingLineSensImpulseCounterRaw,
        }
        return status

    def outputStatus(self) -> Dict[str, Any]:
        
        status = {
            "sortingLineActMotorConveyor": self.sortingLineActMotorConveyor,
            "sortingLineActCompressorOn": self.sortingLineActCompressorOn,
            "sortingLineActWhiteEjector": self.sortingLineActWhiteEjector,
            "sortingLineActRedEjector": self.sortingLineActRedEjector,
            "sortingLineActBlueEjector": self.sortingLineActBlueEjector,
        }
        return status
    
    def internalStatus(self) -> Dict[str, Any]:
        status = {
            "isExecuting": self.__isExecuting(),
        }
        return status


    def startOfProcess(self, packageIncoming):
        self.__isCommandSuccessed = False
        self.__isCommandRunning = True
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

    @override
    def stop_CycleStep(self) -> bool:
        self.__sortingLineActMotorConveyor = False
        self.__sortingLineActCompressorOn = False
        self.__sortingLineActRedEjector = self.__sortingLineActBlueEjector = self.__sortingLineActWhiteEjector = False
        self.__packageOnLine = False
        self.__packageCountSteps = False
        # self.once = True
        self.__counter.counter = 0
        self.__isCommandSuccessed = True
        return True
    
    def eject_CycleStep(self, color: Color) -> bool:
        """
        Used to eject a token, 
        it first detects the presence of the token on the conveyor, then eject the token to the appropriate colored line, it ends with a token detected in the color line.

        This function is a cycleStep, it is call on each controller cycle, until its goal is reached

        :return: as a CycleStep, this function must return True when it is finished so it can be removed from the currentlyExecuting map
        """ 
        ret = False
        whiteCounter = 2
        redCounter = 11
        blueCounter = 20
        current = self.__counter.compute(self.__sortingLineSensImpulseCounterRaw, PlusMinusStop.PLUS)
        logging.debug(f'eject color={str(color)}, current={current}, counter={self.__counter.counter}, __packageCountSteps={self.__packageCountSteps}, __packageOnLine={self.__packageOnLine}, once={self.once}')
        if not self.__packageCountSteps : # and self.once:
            self.startOfProcess(True)
            if not self.__packageCountSteps:
                self.__counter.counter = 0
        else:
            if current > blueCounter and color == color.BLUE:
                self.__sortingLineActMotorConveyor = False
                self.__sortingLineActCompressorOn = True
                self.__sortingLineActBlueEjector = True
                if not self.__sortingLineSensBlueLightBarrier:
                    self.__packageOnLine = self.__packageCountSteps = False
                    print("packageOnLine False")
                    self.__sortingLineActCompressorOn = False
                    self.__sortingLineActBlueEjector = False
                    self.__isCommandRunning = False
                    self.__isCommandSuccessed = True
                    ret = True # command final goal reached, no need to call this cycleStep again
            if current > redCounter and color == Color.RED:
                self.__sortingLineActMotorConveyor = False
                self.__sortingLineActCompressorOn = True
                self.__sortingLineActRedEjector = True
                if not self.__sortingLineSensRedLightBarrier:
                    self.__packageOnLine = False
                    self.__packageCountSteps = False
                    print("packageOnLine False")
                    self.__sortingLineActCompressorOn = False
                    self.__sortingLineActRedEjector = False
                    self.__isCommandRunning = False
                    self.__isCommandSuccessed = True
                    ret = True # command final goal reached, no need to call this cycleStep again
            if current > whiteCounter and color == Color.WHITE:
                self.__sortingLineActMotorConveyor = False
                self.__sortingLineActCompressorOn = True
                self.__sortingLineActWhiteEjector = True
                if not self.__sortingLineSensWhiteLightBarrier:
                    self.__packageOnLine = self.__packageCountSteps = False
                    print("packageOnLine False")
                    self.__sortingLineActCompressorOn = False
                    self.__sortingLineActWhiteEjector = False
                    self.__isCommandRunning = False
                    self.__isCommandSuccessed = True
                    ret = True # command final goal reached, no need to call this cycleStep again
        return ret
    
    ### ____________ Functions callable from orchestrator ________________
    #   function name must be lowercase and finish with '_Command' postfix (cf. RevPiPyMachineController)

    def eject_Command(self, color: Color) -> Optional[Callable[[], bool]]:
        return lambda: self.eject_CycleStep(color)

    def stop_Command(self) -> Optional[Callable[[], bool]]:
        return lambda: self.stop_CycleStep()
        
