import logging
from typing import Tuple
import traceback
from rppmcontroller.machine.conveyorbelt.ConveyorBelt import ConveyorBelt
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