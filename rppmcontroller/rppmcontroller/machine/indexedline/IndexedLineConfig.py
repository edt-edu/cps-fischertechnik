from dataclasses import dataclass

from rppmcontroller.machine.ConveyorState import ConveyorState
from rppmcontroller.machine.MachineConfiguration import MachineConfiguration


@dataclass
class IndexedLineConfig(MachineConfiguration):
    slider_1_extended: bool = False
    slider_2_extended: bool = False
    feed_conveyor_state: ConveyorState = ConveyorState.IDLE
    milling_conveyor_state: ConveyorState = ConveyorState.IDLE
    drilling_conveyor_state: ConveyorState = ConveyorState.IDLE
    swap_conveyor_state: ConveyorState = ConveyorState.IDLE
    milling: bool = False
    drilling: bool = False
