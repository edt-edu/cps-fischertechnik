from enum import Enum


class ConveyorState(Enum):
    """
    Describes the different directions a conveyor may move.

    Which direction is 'forward' and which is 'backward' is machine- and
    conveyor specific and is to be taken from the application context
    """
    IDLE = 0
    FORWARD = 1
    BACKWARD = 2


