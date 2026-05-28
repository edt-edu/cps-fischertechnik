import logging
from dataclasses import dataclass, field
from typing import Optional, Dict

from typing_extensions import Self

from rppmcontroller.machine.MachineParameters import (MachineParameters,
                                                      AxisMonitorParameters)
from rppmcontroller.machine.Position import Position


@dataclass
class VacuumGripperParameters(MachineParameters):
    """All configurable parameters of the VacuumGripper machine"""

    horizontal_safety_position: Optional[int] = None
    """
    The encoder value of the horizontal safety position.
    Must be set together with the `vertical_safety_position` and
    `rotational_safety_position` in order to have an effect.
    If either of those parameters is `None` a setup will be assumed to be
    "safe".
    """
    vertical_safety_position: Optional[int] = None
    """
    The encoder value of the vertical safety position.
    Must be set together with the `horizontal_safety_position` and
    `rotational_safety_position` in order to have an effect.
    If either of those parameters is `None` a setup will be assumed to be
    "safe".
    """
    rotational_safety_position: Optional[int] = None
    """
    The encoder value of the rotational safety position.
    Must be set together with the `vertical_safety_position` and
    `horizontal_safety_position` in order to have an effect.
    If either of those parameters is `None` a setup will be assumed to be
    "safe".
    """
    pwm_standard_speed: int = 100
    """The standard speed used in PWM"""
    pwm_vertical_approach_speed: int = 30
    """
    The reduced speed used in vertical PWM, when the axis is close to its'
    target
    """
    pwm_horizontal_approach_speed: int = 30
    """
    The reduced speed used in horizontal PWM, when the axis is close to its'
    target
    """
    pwm_rotational_approach_speed: int = 20
    """
    The reduced speed used in rotational PWM, when the axis is close to its'
    target
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
    max_horizontal_counter_value: int = 1970
    """
    The maximum encoder counter value for the horizontal axis.
    This value is limited by the physical setup.
    """
    max_rotational_counter_value: int = 3040
    """
    The maximum encoder counter value for the rotational axis.
    This value is limited by the physical setup.
    """
    named_positions: Dict[str, Position] = field(default_factory=dict)
    """
    All named positions
    """
    hover_offset: int = 350
    """
    Offset how high to hover over a position.
    Used in e.g. picking or placing.
    """
    pressure_offset: int = 250
    """
    Offset how much lower to go when pressuring a position.
    Used in e.g. picking or placing.
    """
    horizontal_axis_monitor_parameters: AxisMonitorParameters = (
        AxisMonitorParameters())
    """Configuration for the horizontal axis monitor"""
    vertical_axis_monitor_parameters: AxisMonitorParameters = (
        AxisMonitorParameters())
    """Configuration for the vertical axis monitor"""
    rotational_axis_monitor_parameters: AxisMonitorParameters = (
        AxisMonitorParameters())
    """Configuration for the rotational axis monitor"""

    def derive_over_positions(self) -> Self:
        """
        For all configured named_positions derive and add an "OVER_ position",
        which hovers over the configured position.

        Over-positions will not extend horizontally, since the arm
        is retracted anyway before performing a pickup or placement, and
        they can be used as safety positions.
        :return: self
        """
        for name, position in list(self.named_positions.items()):
            hover_positon_name = f"OVER_{name}"
            if hover_positon_name in self.named_positions:
                logging.warning(f"Cannot create and over-position for "
                                f"position {name},"
                                f" since position {hover_positon_name} "
                                f"already exists.")
                continue

            self.named_positions[
                hover_positon_name] = Position(rot=position.rot,
                                               vertical=position.vertical
                                                        - self.hover_offset,
                                               horizontal=0,
                                               meaning=position.meaning)
        return self
