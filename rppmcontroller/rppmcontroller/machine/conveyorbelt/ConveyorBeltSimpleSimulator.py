import logging
from rppmcontroller.machine.conveyorbelt.ConveyorBelt import ConveyorBelt
from rppmcontroller.machine.RequestedParameter import RequestedParameter
from rppmcontroller.machine.MachineSimpleSimulator import MachineSimpleSimulator


class ConveyorBeltSimpleSimulator(MachineSimpleSimulator):
    """A Simple simulator connected to a ConveyorBelt Machine """

    def __init__(self, controlledConveyorBelt : ConveyorBelt):
        """Initialize a simulator connected to a ConveyorBelt Machine

        Parameters:
            controlledConveyorBelt (ConveyorBelt): ConveyorBelt that is connected to this simulator
        """
        self.__controlledConveyorBelt = controlledConveyorBelt

    @property
    def controlledConveyorBelt(self):
        return self.__controlledConveyorBelt
    
    def simulatedRead(self) -> None:        
        logging.debug(f"simulatedRead {self.controlledConveyorBelt.sensorStatusString()} ")

    def simulatedWrite(self) -> None:
        logging.debug(f"simulatedWrite {self.controlledConveyorBelt.sensorStatusString()} ")

    def simulatedReset(self) -> None:
        pass

    def fakeSensor(self, parameter : RequestedParameter, value: bool):
        if parameter == RequestedParameter.LIGHTBARRIERFEEDSTATION:
            self.controlledConveyorBelt.conveyorSensFeed = value
        if parameter == RequestedParameter.LIGHTBARRIERSWAPSTATION:
            self.controlledConveyorBelt.conveyorSensSwap = value
        if parameter == RequestedParameter.PULSECOUNTER:
            self.controlledConveyorBelt.conveyorSensImpulse = value
        else :
            logging.warning("Wrong parameter in fakeSensor function")