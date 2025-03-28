from enum import Enum

from typing_extensions import deprecated


@deprecated("in favor of ConveyorState and TurnTableDirection")
class Direction(Enum):
    NONE = 0
    FORWARD = 1
    BACKWARD = 2
    CLOCKWISE = 3
    COUNTERCLOKWISE = 4
