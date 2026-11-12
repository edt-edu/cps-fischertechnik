from __future__ import annotations

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

    log_to_csv: bool = False
    """Whether to log the monitored data to a CSV file"""
    cycles_to_monitor: int = 30
    """Number of cycles for that will be stored, whether axis movement was
    successful. Those will be used to calculate the average moved distance."""
    required_cycles_to_average: int = 10
    """Number of cycles of the recorded cycles which must contain movement
    data for the current pwm value that are required to create an average
    movement distance. If not enough cycles provide data, then no small
    deviations can be detected, only high ones. This value must not exceed
    the number of cycles to monitor."""
    cycles_to_penalize: int = 10
    """Number of cycles for which the penalty points will be accumulated.
    This value must not exceed the number of cycles to monitor."""
    minor_deviation_penalty: int = 1
    """The amount of penalty points for a movement that is smaller than the
    expected one"""
    major_deviation_penalty: int = 2
    """The amount of penalty points for when the axis has not moved at all"""
    penalty_threshold: int = 10
    """The amount of penalty points that needs to be reached for the controller
    to take action and abort the current command or consider it completed.
    The exact kind of action taken may be machine-specific.
    Should not exceed
    `penalty_threshold * max(minor_deviation_penalty, major_deviation_penalty)`
    """
    movement_tolerance: int = 3
    """Any moved distance lower than this value will be considered to be a
    major deviation"""
    measurement_interval: float = 0.1
    """Delay in seconds before another measurement is taken.
    Increase this value if you get too many false-positive high deviations."""


@dataclass
class AxisParameters:
    """Parameters for a generic axis"""
    control_parameters: ControlParameters = field(default_factory=lambda: PWMParameters(
        approach_speed=30,
        approach_tolerance=100))
    """Parameters that determine how the axis will be controlled"""
    tolerance: int | None = None
    """Tolerance of whether the axis target has been reached. When set in combination with pid_parameters then this value will be used as error_deadband"""
    max_counter_value: int | None = None
    """The maximum encoder counter value for this axis.
    This limit is induced by the physical setup."""
    pwm_standard_speed: int = 100
    """How fast the axis should move normally"""


class ControlParameters(ABC):
    """Parameters that determine how the machine should be controlled"""


@dataclass
class PIDParameters(ControlParameters):
    """Parameters for a PID controller of an axis"""
    main_loop_delay: float
    """The delay of the main control loop in seconds"""
    decimation_factor: int = 10
    """The factor by how much the pid controller is slowed down compared to the
    MachineController"""
    smoothing_delay_seconds: float = 0.05
    """How many seconds reaction delay will be introduced by smoothing out the
    position signal"""


@dataclass
class PWMParameters(ControlParameters):
    """Parameters for an approached based PWM control"""
    approach_speed: int
    """How fast the axis should approach the target value"""
    approach_tolerance: int
    """How close the axis must be to the target value before switching to approach mode"""
