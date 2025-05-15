from dataclasses import dataclass

from rppmcontroller.machine.MachineConfiguration import MachineConfiguration


@dataclass
class IndexedLineConfig(MachineConfiguration):
    slider_1_extended: bool = False
    slider_2_extended: bool = False
    feed_conveyor: bool = False
    milling_conveyor: bool = False
    drilling_conveyor: bool = False
    swap_conveyor: bool = False
    milling: bool = False
    drilling: bool = False
