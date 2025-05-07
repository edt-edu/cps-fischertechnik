from dataclasses import dataclass

from rppmcontroller.machine.ConveyorState import ConveyorState
from rppmcontroller.machine.MachineConfiguration import MachineConfiguration


@dataclass
class PunchingMachineConfig(MachineConfiguration):
    conveyor_state: ConveyorState = ConveyorState.IDLE
    punching: bool = False
    """Whether the machine should be punching an object"""
