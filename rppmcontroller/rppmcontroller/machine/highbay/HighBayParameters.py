from dataclasses import dataclass, field
from typing import Optional

from rppmcontroller.machine.MachineParameters import (MachineParameters,
                                                      AxisMonitorParameters)


@dataclass
class HighBayParameters(MachineParameters):
    """
    All configurable parameters of the HighBay machine.
    """

    pickup_distance: int = 150
    """How far up the arm will be moved, when picking up an item"""
    conveyor_column: int = 70
    """The encoder value of the horizontal position of the column"""
    right_column: int = 1550
    """The encoder value of the horizontal position of the column closest to
    the conveyor"""
    middle_column: int = 2700
    """The encoder value of the horizontal position of the middle column"""
    left_column: int = 3900
    """The encoder value of the horizontal position of the column furthest
    from the conveyor"""
    conveyor_row: int = 1450
    """The encoder value of the vertical conveyor position"""
    bottom_row: int = 1700
    """The encoder value of the vertical position of the bottom row"""
    middle_row: int = 900
    """The encoder value of the vertical position of the middle row"""
    top_row: int = 200
    """The encoder value of the vertical position of the top row"""
    horizontal_safety_position: Optional[int] = None
    """
    The encoder value of the horizontal safety position.
    Must be set together with the `vertical_safety_position` in order to have
    an effect.
    If either of those parameters is `None` a setup will be assumed to be
    "safe".
    """
    vertical_safety_position: Optional[int] = None
    """
    The encoder value of the vertical safety position.
    Must be set together with the `horizontal_safety_position` in order to have
    an effect.
    If either of those parameters is `None` a setup will be assumed to be
    "safe".
    """
    pwm_standard_speed: int = 100
    """The standard speed for PWM"""
    pwm_reduced_speed: int = 50
    """
    The reduced speed for PWM, when an axis is close to its' target position
    """
    pwm_approach_tolerance: int = 100
    """
    Tolerance for when an axis is considered to be close to its target position
    """
    max_vertical_counter_value: int = 1750
    """
    The maximum encoder counter value for the vertical axis.
    This value is limited by the physical setup.
    """
    max_horizontal_counter_value: int = 4000
    """
    The maximum encoder counter value for the horizontal axis.
    This value is limited by the physical setup.
    """
    vertical_axis_monitor_parameters: AxisMonitorParameters = field(
        default_factory=lambda: AxisMonitorParameters())
    """Configuration for the vertical axis monitor"""
    horizontal_axis_monitor_parameters: AxisMonitorParameters = field(
        default_factory=lambda: AxisMonitorParameters())
    """Configuration for the horizontal axis monitor"""

    def add_horizontal_offset(self, offset: int) -> None:
        """
        Add an offset to all columns.

        :param offset: A relative offset
        :return: self
        """
        self.conveyor_column += offset
        self.right_column += offset
        self.middle_column += offset
        self.left_column += offset

    def add_vertical_offset(self, offset: int) -> None:
        """
        Add an offset to all rows.

        :param offset: A relative offset
        :return: self
        """
        self.conveyor_row += offset
        self.bottom_row += offset
        self.middle_row += offset
        self.top_row += offset
