from enum import Enum


class VacuumArmState(Enum):
    """Where the vacuum gripper arm should be"""
    AT_TURN_TABLE = 0
    """Idle at the turn table"""
    AT_OVEN = 1
    """Idle at the oven"""
    PICKUP = 2
    """Lowered with active vacuum at the oven"""
