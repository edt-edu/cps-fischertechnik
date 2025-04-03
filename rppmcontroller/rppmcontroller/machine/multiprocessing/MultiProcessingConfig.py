from dataclasses import dataclass

from rppmcontroller.machine.MachineConfiguration import MachineConfiguration
from rppmcontroller.machine.multiprocessing.TurnTablePosition import \
    TurnTablePosition


@dataclass
class MultiProcessingConfig(MachineConfiguration):
    turn_table_position: TurnTablePosition = TurnTablePosition.VACUUM
    saw_active: bool = False
    conveyor_feeder_active: bool = False
    conveyor_active: bool = False
    oven_feeder_expanded: bool = True
    oven_lamp_on: bool = False
    oven_door_open: bool = False
    vacuum_arm_at_oven: bool = False
    vacuum_arm_lowered: bool = False
    vacuum_valve_active: bool = False
