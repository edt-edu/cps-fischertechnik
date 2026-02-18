from dataclasses import dataclass
from typing import Optional


@dataclass
class VacuumGripperParameters:
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
