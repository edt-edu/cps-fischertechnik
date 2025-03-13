from rppmcontroller.machine.AxisConfig import AxisConfig
from rppmcontroller.machine.ConveyorState import ConveyorState
from rppmcontroller.machine.MachineConfiguration import MachineConfiguration


class HighBayConfig(MachineConfiguration):
    def __init__(self,
                 horizontal_axis_config: AxisConfig = AxisConfig.to_end_position(),
                 vertical_axis_config: AxisConfig = AxisConfig.to_end_position(),
                 cantilever_extended: bool = False,
                 conveyor_state: ConveyorState = ConveyorState.IDLE):
        self.horizontal_axis_config = horizontal_axis_config
        self.vertical_axis_config = vertical_axis_config
        self.cantilever_extended = cantilever_extended
        self.conveyor_state = conveyor_state

    def __eq__(self, other):
        return (isinstance(other, HighBayConfig) and
                self.horizontal_axis_config == other.horizontal_axis_config and
                self.vertical_axis_config == other.vertical_axis_config and
                self.cantilever_extended == other.cantilever_extended and
                self.conveyor_state == other.conveyor_state)
