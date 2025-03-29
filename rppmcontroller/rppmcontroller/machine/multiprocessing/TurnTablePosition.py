from enum import Enum


class TurnTablePosition(Enum):
    """Where the turn table should currently be"""
    VACUUM = 0
    """At the vacuum arm drop off point"""
    SAW = 1
    """At the saw with an active saw"""
    CONVEYOR = 2
    """At the conveyor belt with an active feeder"""
