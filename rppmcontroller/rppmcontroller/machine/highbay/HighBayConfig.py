from dataclasses import dataclass

from rppmcontroller.machine.AxisConfig import AxisConfig
from rppmcontroller.machine.ConveyorState import ConveyorState
from rppmcontroller.machine.MachineConfiguration import MachineConfiguration


@dataclass
class HighBayConfig(MachineConfiguration):
    horizontal_axis_config: AxisConfig = AxisConfig.to_end_position()
    vertical_axis_config: AxisConfig = AxisConfig.to_end_position()
    cantilever_extended: bool = False
    conveyor_state: ConveyorState = ConveyorState.IDLE
