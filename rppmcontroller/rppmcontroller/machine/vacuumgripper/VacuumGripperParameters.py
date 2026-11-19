from __future__ import annotations

import logging
from dataclasses import dataclass, field
from typing import Optional, Dict
from typing_extensions import Self, deprecated

from rppmcontroller.machine.MachineParameters import (MachineParameters,
                                                      AxisMonitorParameters,
                                                      AxisParameters,
                                                      ApproachParameters)
from rppmcontroller.machine.Position import Position


# redeclaration can be suppressed since we overload attributes with properties
# to maintain the init signature
# deprecation can be suppressed since we need to deprecate both getter and setter of deprecated attributes
# attribute-outside-init can be suppressed since we overload attributes with properties
# noinspection redeclaration,deprecation,attribute-outside-init
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
    pwm_standard_speed: int | None = field(default=None)
    """The standard speed used in PWM"""
    pwm_vertical_approach_speed: int | None = field(default=None)
    """
    The reduced speed used in vertical PWM, when the axis is close to its'
    target
    """
    pwm_horizontal_approach_speed: int | None = field(default=None)
    """
    The reduced speed used in horizontal PWM, when the axis is close to its'
    target
    """
    pwm_rotational_approach_speed: int | None = field(default=None)
    """
    The reduced speed used in rotational PWM, when the axis is close to its'
    target
    """
    pwm_approach_tolerance: int | None = field(default=None)
    """
    Tolerance for when an axis is considered to be close to its target position
    """
    max_vertical_counter_value: int | None = field(default=None)
    """
    The maximum encoder counter value for the vertical axis.
    This value is limited by the physical setup.

    This option is deprecated in favor of vertical_axis_parmeters.max_counter_value
    """
    max_horizontal_counter_value: int | None = field(default=None)
    """
    The maximum encoder counter value for the horizontal axis.
    This value is limited by the physical setup.

    This option is deprecated in favor of horizonal_axis_parmeters.max_counter_value
    """
    max_rotational_counter_value: int | None = field(default=None)
    """
    The maximum encoder counter value for the rotational axis.
    This value is limited by the physical setup.

    This option is deprecated in favor of rotational_axis_parmeters.max_counter_value
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
    horizontal_axis_monitor_parameters: AxisMonitorParameters = field(
        default_factory=lambda: AxisMonitorParameters())
    """Configuration for the horizontal axis monitor"""
    vertical_axis_monitor_parameters: AxisMonitorParameters = field(
        default_factory=lambda: AxisMonitorParameters())
    """Configuration for the vertical axis monitor"""
    rotational_axis_monitor_parameters: AxisMonitorParameters = field(
        default_factory=lambda: AxisMonitorParameters())
    """Configuration for the rotational axis monitor"""
    horizontal_axis_parameters: AxisParameters = field(
        default_factory=lambda: AxisParameters(max_counter_value=1970,
                                               tolerance=5))
    """Configuration of the horizontal axis"""
    vertical_axis_parameters: AxisParameters = field(
        default_factory=lambda: AxisParameters(max_counter_value=1750,
                                               tolerance=7))
    """Configuration of the vertical axis"""
    rotational_axis_parameters: AxisParameters = field(
        default_factory=lambda: AxisParameters(control_parameters=ApproachParameters(
            approach_speed=20), max_counter_value=3040, tolerance=5))
    """Configuration of the rotational axis"""

    # Deprecated properties that shadow the dataclass fields above.
    # The dataclass decorator still includes the field names in the auto-
    # generated __init__, but at runtime the property descriptors take
    # precedence, so PyCharm shows deprecation warnings on access.

    @property
    @deprecated("Use horizontal_axis_parameters.pwm_standard_speed, "
                "vertical_axis_parameters.pwm_standard_speed, or "
                "rotational_axis_parameters.pwm_standard_speed instead.")
    def pwm_standard_speed(self) -> int | None:
        """The standard speed used in PWM"""
        return self._pwm_standard_speed

    @pwm_standard_speed.setter
    @deprecated("Use horizontal_axis_parameters.pwm_standard_speed, "
                "vertical_axis_parameters.pwm_standard_speed, or "
                "rotational_axis_parameters.pwm_standard_speed instead.")
    def pwm_standard_speed(self, value: int | None) -> None:
        self._pwm_standard_speed = value

    @property
    @deprecated(
        "Use vertical_axis_parameters.control_parameters.approach_speed instead.")
    def pwm_vertical_approach_speed(self) -> int | None:
        """
        The reduced speed used in vertical PWM, when the axis is close to its'
        target
        """
        return self._pwm_vertical_approach_speed

    @pwm_vertical_approach_speed.setter
    @deprecated(
        "Use vertical_axis_parameters.control_parameters.approach_speed instead.")
    def pwm_vertical_approach_speed(self, value: int | None) -> None:
        self._pwm_vertical_approach_speed = value

    @property
    @deprecated(
        "Use horizontal_axis_parameters.control_parameters.approach_speed instead.")
    def pwm_horizontal_approach_speed(self) -> int | None:
        """
        The reduced speed used in horizontal PWM, when the axis is close to its'
        target
        """
        return self._pwm_horizontal_approach_speed

    @pwm_horizontal_approach_speed.setter
    @deprecated(
        "Use horizontal_axis_parameters.control_parameters.approach_speed instead.")
    def pwm_horizontal_approach_speed(self, value: int | None) -> None:
        self._pwm_horizontal_approach_speed = value

    @property
    @deprecated(
        "Use rotational_axis_parameters.control_parameters.approach_speed instead.")
    def pwm_rotational_approach_speed(self) -> int | None:
        """
        The reduced speed used in rotational PWM, when the axis is close to its'
        target
        """
        return self._pwm_rotational_approach_speed

    @pwm_rotational_approach_speed.setter
    @deprecated(
        "Use rotational_axis_parameters.control_parameters.approach_speed instead.")
    def pwm_rotational_approach_speed(self, value: int | None) -> None:
        self._pwm_rotational_approach_speed = value

    @property
    @deprecated(
        "Use axis_parameters.control_parameters.approach_tolerance instead.")
    def pwm_approach_tolerance(self) -> int | None:
        """
        Tolerance for when an axis is considered to be close to its target position
        """
        return self._pwm_approach_tolerance

    @pwm_approach_tolerance.setter
    @deprecated(
        "Use axis_parameters.control_parameters.approach_tolerance instead.")
    def pwm_approach_tolerance(self, value: int | None) -> None:
        self._pwm_approach_tolerance = value

    @property
    @deprecated("Use vertical_axis_parameters.max_counter_value instead.")
    def max_vertical_counter_value(self) -> int | None:
        """
        The maximum encoder counter value for the vertical axis.
        This value is limited by the physical setup.
        """
        return self._max_vertical_counter_value

    @max_vertical_counter_value.setter
    @deprecated("Use vertical_axis_parameters.max_counter_value instead.")
    def max_vertical_counter_value(self, value: int | None) -> None:
        self._max_vertical_counter_value = value

    @property
    @deprecated("Use horizontal_axis_parameters.max_counter_value instead.")
    def max_horizontal_counter_value(self) -> int | None:
        """
        The maximum encoder counter value for the horizontal axis.
        This value is limited by the physical setup.
        """
        return self._max_horizontal_counter_value

    @max_horizontal_counter_value.setter
    @deprecated("Use horizontal_axis_parameters.max_counter_value instead.")
    def max_horizontal_counter_value(self, value: int | None) -> None:
        self._max_horizontal_counter_value = value

    @property
    @deprecated("Use rotational_axis_parameters.max_counter_value instead.")
    def max_rotational_counter_value(self) -> int | None:
        """
        The maximum encoder counter value for the rotational axis.
        This value is limited by the physical setup.
        """
        return self._max_rotational_counter_value

    @max_rotational_counter_value.setter
    @deprecated("Use rotational_axis_parameters.max_counter_value instead.")
    def max_rotational_counter_value(self, value: int | None) -> None:
        self._max_rotational_counter_value = value

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


def _fix_deprecated_field_defaults(cls):
    """
    Patch a dataclass __init__ defaults tuple so that property-shadowed
    deprecated fields default to None instead of the property descriptor.

    The dataclass decorator reads defaults from class attributes, but when a
    property shadows a field the class attribute is the property object, not
    the intended default.  This helper replaces those property objects with
    None after the class has been created.
    """
    defaults = cls.__init__.__defaults__
    if defaults is None:
        return
    new_defaults = tuple(
        None if isinstance(d, property) else d
        for d in defaults
    )
    cls.__init__.__defaults__ = new_defaults


# Patch the dataclass __init__ defaults so property-shadowed deprecated fields
# default to None instead of the property descriptor objects.
_fix_deprecated_field_defaults(VacuumGripperParameters)
