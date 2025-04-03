from __future__ import annotations

from enum import Enum
from typing import Optional


class TurnTablePosition(Enum):
    """Where the turn table should currently be"""
    VACUUM = 0
    """At the vacuum arm drop off point"""
    SAW = 1
    """At the saw with an active saw"""
    CONVEYOR = 2
    """At the conveyor belt with an active feeder"""

    @staticmethod
    def from_actuators(at_vacuum: bool, at_saw: bool, at_belt: bool) -> \
    Optional[TurnTablePosition]:
        if at_vacuum:
            return TurnTablePosition.VACUUM
        if at_saw:
            return TurnTablePosition.SAW
        if at_belt:
            return TurnTablePosition.CONVEYOR
        return None
