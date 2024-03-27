import logging
from typing import Tuple
import traceback
from rppmcontroller.machine.vacuumgripper.VacuumGripper import VacuumGripper


class MachineSimpleSimulator():
    """Base class for machine simulator
    
    The intent is to provide support for the controller main loop functions : read, write and reset

    Engines are simulated by supposing that their associated counter are incremented/decreamented  on each iteration (ie. call to simulatedRead)
    """
    
    def simulatedRead(self) -> None:
        """Simulation of effects of the read() on the machine"""
        pass

    def simulatedWrite(self) -> None:
        """Simulation of effects of the write() on the machine"""
        pass

    def simulatedReset(self) -> None:
        """Simulation of effects of the reset() on the machine
        
        Note: only when the reset is really done, the check if it need to be performed or not should be done by the caller"""
        pass