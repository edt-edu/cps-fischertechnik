from dataclasses import dataclass

from rppmcontroller.machine.ConveyorState import ConveyorState
from rppmcontroller.machine.MachineConfiguration import MachineConfiguration


@dataclass
class ConveyorBeltConfig(MachineConfiguration):
    state: ConveyorState = ConveyorState.IDLE
