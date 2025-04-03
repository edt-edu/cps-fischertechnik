from dataclasses import dataclass

from rppmcontroller.machine.MachineConfiguration import MachineConfiguration


@dataclass
class SortingLineConfig(MachineConfiguration):
    conveyor_active: bool = False
    white_ejector_active: bool = False
    red_ejector_active: bool = False
    blue_ejector_active: bool = False
