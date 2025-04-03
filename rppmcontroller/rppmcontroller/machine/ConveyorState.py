from __future__ import annotations

from enum import Enum
from warnings import deprecated

from rppmcontroller.machine.Direction import Direction


class ConveyorState(Enum):
    """
    Describes the different directions a conveyor may move.

    Which direction is 'forward' and which is 'backward' is machine- and
    conveyor specific and is to be taken from the application context
    """
    IDLE = 0
    FORWARD = 1
    BACKWARD = 2


    @staticmethod
    def from_direction(direction: Direction) -> ConveyorState:
        """
        Convert a Direction into a ConveyorState
        :param direction: The Direction to convert
        :return: The corresponding conveyor state
        """
        if direction is Direction.NONE:
            return ConveyorState.IDLE
        if direction is Direction.FORWARD:
            return ConveyorState.FORWARD
        if direction is Direction.BACKWARD:
            return ConveyorState.BACKWARD
        raise ValueError(f"direction doesn't represent a valid conveyor state: {direction}")

    @staticmethod
    def from_actuators(forward: bool, backward: bool) -> ConveyorState:
        """
        Determines a conveyor state from the current actuators
        :param forward: the forward actuator
        :param backward: the backward actuator
        :return: The corresponding conveyor state
        """
        if forward == backward:
            return ConveyorState.IDLE
        if forward:
            return ConveyorState.FORWARD
        return ConveyorState.BACKWARD


@deprecated("in favor of ConveyorState#from_actuators")
def conveyor_state_from_movements(forward: bool, backwards: bool) -> ConveyorState:
    """
    Determines a conveyor state from the current actuators
    :param forward: the forward actuator
    :param backwards: the backward actuator
    :return: The corresponding conveyor state
    """
    return ConveyorState.from_actuators(forward, backwards)



