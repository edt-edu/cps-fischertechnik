from rppmcontroller.machine.MachineConfiguration import MachineConfiguration
from rppmcontroller.machine.multiprocessing.TurnTablePosition import \
    TurnTablePosition
from rppmcontroller.machine.multiprocessing.VacuumArmState import \
    VacuumArmState


class MultiProcessingConfig(MachineConfiguration):
    def __init__(self,
                 turn_table_position: TurnTablePosition =
                 TurnTablePosition.VACUUM,
                 conveyor_active: bool = False,
                 oven_active: bool = False,
                 vacuum_arm_state: VacuumArmState =
                 VacuumArmState.AT_TURN_TABLE):
        self.turn_table_position = turn_table_position
        self.conveyor_active = conveyor_active
        self.oven_active = oven_active
        self.vacuum_arm_state = vacuum_arm_state

    def __eq__(self, other):
        return (isinstance(other,
                           MultiProcessingConfig) and
                self.turn_table_position == other.turn_table_position and
                self.conveyor_active == other.conveyor_active and
                self.oven_active == other.oven_active and
                self.vacuum_arm_state == other.vacuum_arm_state)
