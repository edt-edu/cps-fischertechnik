from enum import Enum

class TurnTableDirection(Enum):
    NONE = 0
    CLOCKWISE = 1
    COUNTER_CLOCKWISE = 2

    @staticmethod
    def from_actuators(clockwise: bool, anti_clockwise: bool):
        if clockwise == anti_clockwise:
            return TurnTableDirection.NONE
        if clockwise:
            return TurnTableDirection.CLOCKWISE
        return TurnTableDirection.COUNTER_CLOCKWISE
