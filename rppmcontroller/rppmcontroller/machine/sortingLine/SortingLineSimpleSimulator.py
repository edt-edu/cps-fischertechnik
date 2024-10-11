import logging
from rppmcontroller.machine.sortingLine.SortingLine import SortingLine
from rppmcontroller.machine.RequestedParameter import RequestedParameter
from rppmcontroller.machine.MachineSimpleSimulator import MachineSimpleSimulator


class SortingLineSimpleSimulator(MachineSimpleSimulator):
    """A Simple simulator connected to a SortingLine Machine """

    def __init__(self, controlledSortingLine : SortingLine):
        """Initialize a simulator connected to a SortingLine Machine

        Parameters:
            controlledSortingLine (SortingLine): SortingLine that is connected to this simulator
        """
        self.__controlledSortingLine = controlledSortingLine

        #True is the value when there is no object in front of light sensors
        self.controlledSortingLine.sortingLineSensRedLightBarrier = True
        self.controlledSortingLine.sortingLineSensWhiteLightBarrier = True
        self.controlledSortingLine.sortingLineSensBlueLightBarrier = True
        self.controlledSortingLine.sortingLineSensInputLightBarrier = True
        self.controlledSortingLine.sortingLineSensMiddleLightBarrier = True

    @property
    def controlledSortingLine(self):
        return self.__controlledSortingLine
    
    def simulatedRead(self) -> None:    
        logging.debug(f"simulatedRead  {self.controlledSortingLine.sensorStatusString()} ")

    def simulatedWrite(self) -> None:
        # if moving increase counter
        if self.controlledSortingLine.sortingLineActMotorConveyor:
            self.controlledSortingLine.sortingLineSensImpulseCounterRaw += 1  

        logging.debug(f"simulatedWrite {self.controlledSortingLine.actuatorStatusString()} ")

    def simulatedReset(self) -> None:
        pass

    def fakeSensor(self, parameter : RequestedParameter, value: bool):
        if parameter == RequestedParameter.LIGHTBARRIERINLET:
            self.controlledSortingLine.sortingLineSensInputLightBarrier = value
        elif parameter == RequestedParameter.LIGHTBARRIERBEHINDCOLORSENSOR:
            self.controlledSortingLine.sortingLineSensMiddleLightBarrier = value
        elif parameter == RequestedParameter.LIGHTBARRIERWHITE:
            self.controlledSortingLine.sortingLineSensWhiteLightBarrier = value
        elif parameter == RequestedParameter.LIGHTBARRIERRED:
            self.controlledSortingLine.sortingLineSensRedLightBarrier = value
        elif parameter == RequestedParameter.LIGHTBARRIERBLUE:
            self.controlledSortingLine.sortingLineSensBlueLightBarrier = value
        elif parameter == RequestedParameter.PULSECOUNTER:
            self.controlledSortingLine.sortingLineSensImpulseCounterRaw = value
        else :
            logging.warning("Wrong parameter in fakeSensor function")

    def getCounter(self) -> int:
        return self.controlledSortingLine.sortingLineCounterValue