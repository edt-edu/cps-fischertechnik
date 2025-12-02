from enum import Enum

from typing_extensions import deprecated

@deprecated("in favor of TurnTablePosition")
class MPSTurntablePosition(Enum):
    ARM = 0
    SAW = 1
    CONVEYOR = 2
