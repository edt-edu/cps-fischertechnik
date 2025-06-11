from rppmcontroller.machine.AxisConfig import AxisConfig
from dataclasses import dataclass
from rppmcontroller.machine.MachineConfiguration import MachineConfiguration


@dataclass
class VacuumGripperConfig(MachineConfiguration):
    vertical_axis_config: AxisConfig = AxisConfig.to_end_position()
    """The configuration for the height of the arm"""
    rotation_axis_config: AxisConfig = AxisConfig.to_end_position()
    """The configuration for the rotation of the arm"""
    horizontal_axis_config: AxisConfig = AxisConfig.to_end_position()
    """The configuration for the horizontal extension of the arm"""
    gripper_active: bool = False
    """Whether the vacuum valve on the arm is active"""
