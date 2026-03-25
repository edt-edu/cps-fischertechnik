from dataclasses import dataclass


@dataclass(frozen=True)
class AxisBoolThreeD:
    vertical: bool
    rot: bool
    horizontal: bool
