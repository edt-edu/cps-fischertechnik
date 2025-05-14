from typing import Dict, Any

from rppmcontroller.behavior.CycleStepResult import CycleStepResult
from rppmcontroller.machine.Machine import Machine
from rppmcontroller.machine.RequestedParameter import RequestedParameter
from rppmcontroller.machine.Runner import TransitioningMachine


class IndexedLine(Machine, TransitioningMachine):
    def __init__(self):
        # inputs
        self.__indexedLineSensSlider1Front = False
        self.__indexedLineSensSlider1Rear = False
        self.__indexedLineSensSlider2Front = False
        self.__indexedLineSensSlider2Rear = False
        self.__indexedLineSensSlider1 = True
        self.__indexedLineSensMilling = True
        self.__indexedLineSensLoading = True
        self.__indexedLineSensDrilling = True
        self.__indexedLineSensSwap = True

        # outputs
        self.__indexedLineActSlider1Forward = False
        self.__indexedLineActSlider1Backward = False
        self.__indexedLineActSlider2Forward = False
        self.__indexedLineActSlider2Backward = False
        self.__indexedLineActFeedConveyor = False
        self.__indexedLineActMillingConveyor = False
        self.__indexedLineActDrillingConveyor = False
        self.__indexedLineActSwapConveyor = False
        self.__indexedLineActMilling = False
        self.__indexedLineActDrilling = False

        dictMap = {
            RequestedParameter.PUSHBUTTONSLIDER1FRONT: self.__indexedLineSensSlider1Front,
            RequestedParameter.PUSHBUTTONSLIDER1REAR: self.__indexedLineSensSlider1Rear,
            RequestedParameter.PUSHBUTTONSLIDER2FRONT: self.__indexedLineSensSlider2Front,
            RequestedParameter.PUSHBUTTONSLIDER2REAR: self.__indexedLineSensSlider2Rear,
            RequestedParameter.LIGHTBARRIERSLIDER1: self.__indexedLineSensSlider1,
            RequestedParameter.LIGHTBARRIERMILLINGMACHINE: self.__indexedLineSensMilling,
            RequestedParameter.LIGHTBARRIERLOADINGSTATION: self.__indexedLineSensLoading,

            }


    @property
    def isExecuting(self) -> bool:
        pass

    @property
    def isInitialized(self) -> bool:
        pass

    def sensorStatusString(self) -> str:
        pass

    def actuatorStatusString(self) -> str:
        pass

    def inputStatus(self) -> Dict[str, Any]:
        pass

    def outputStatus(self) -> Dict[str, Any]:
        pass

    def internalStatus(self) -> Dict[str, Any]:
        pass

    def stop_CycleStep(self) -> CycleStepResult:
        pass

    def goto_config(self, config) -> CycleStepResult:
        pass
