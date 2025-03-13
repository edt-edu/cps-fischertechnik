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

def conveyor_state_from_movements(forward: bool, backwards: bool) -> ConveyorState:
    """
    Determines a conveyor state from the current
    :param forward:
    :param backwards:
    :return:
    """
    if forward == backwards:
        return ConveyorState.IDLE
    if forward:
        return ConveyorState.FORWARD
    return ConveyorState.BACKWARD



