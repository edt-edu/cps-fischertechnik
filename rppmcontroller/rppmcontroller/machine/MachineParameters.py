from abc import ABC
from dataclasses import dataclass, field
from typing import Any, Dict


@dataclass
class MachineParameters(ABC):
    """
    Base class for parameter classes
    """

    named_positions: Dict[str, Any] = field(default_factory=dict)
    """
    All named positions, as a mapping of the name to the position.
    What a position is in this context is machine-specific and therefore
    not further specifiable here
    """


@dataclass
class AxisMonitorParameters:
    """
    Generic parameters for an AxisMonitor
    """

    cycles_to_monitor: int = 10
    """Number of cycles for that will be stored, whether axis movement was
    successful"""
    required_cycles_to_average: int = 7
    """Number of cycles of the recorded cycles which must contain movement
    data for the current pwm value that are required to create an average
    movement distance. If not enough cycles provide data, then no small
    deviations can be detected, only high ones. This value must not exceed
    the number of cycles to monitor."""
    minor_deviation_penalty: int = 5
    """The amount of penalty points for a movement that is smaller than the
    expected one"""
    major_deviation_penalty: int = 6
    """The amount of penalty points for when the axis has not moved at all"""
    penalty_threshold: int = 30
    """The amount of penalty points that needs to be reached for the controller
    to take action and abort the current command or consider it completed.
    The exact kind of action taken may be machine-specific."""

