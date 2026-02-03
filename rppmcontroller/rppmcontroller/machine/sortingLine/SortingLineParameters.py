from dataclasses import dataclass

from typing_extensions import deprecated


@dataclass
class SortingLineParameters:
    """All configurable parameters of the SortingLine machine."""

    white_ejector_delay: float = 0.5
    """
    The delay in seconds after which the white ejector gets triggered after a
    token passed the light barrier behind the color sensor.
    """
    red_ejector_delay: float = 1.55
    """
    The delay in seconds after which the red ejector gets triggered after a
    token passed the light barrier behind the color sensor.
    """
    blue_ejector_delay: float = 2.6
    """
    The delay in seconds after which the blue ejector gets triggered after a
    token passed the light barrier behind the color sensor.
    """
    pass_through_delay: float = 3.0
    """
    The delay in seconds after which a payload should have been transported
    to the end of the conveyor belt, after it passed the light barrier behind
    the color sensor.
    """
    ejector_activation_time: float = 0.5
    """The time in seconds for which an ejector stays activated"""
    white_ejector_steps: int = 5
    """
    The number of steps after which the white ejector gets triggered after a
    token passed the light barrier behind the color sensor.
    """
    red_ejector_steps: int = 15
    """
    The number of steps after which the red ejector gets triggered after a
    token passed the light barrier behind the color sensor.
    """
    blue_ejector_steps: int = 25
    """
    The number of steps after which the blue ejector gets triggered after a
    token passed the light barrier behind the color sensor.
    """
    pass_through_steps: int = 30
    """
    The number of steps after which a payload should have been transported
    to the end of the conveyor belt, after it passed the light barrier behind
    the color sensor.
    """
    ejector_activation_steps: int = 5
    """The number of steps for which an ejector stays activated"""
    mock_analog_sensor: bool = False
    """
    Whether to mock the analog sensor instead of using the hardware one.

    Not every setup requires the analog sensor.
    Setting this to true will randomly pick a color.
    """

    def add_ejector_time_offset(self, seconds: float) -> None:
        """
        Adds an offset to all ejector delays and the pass-through delay

        :param seconds: The offset to add, in seconds
        :return: None
        """
        self.white_ejector_delay += seconds
        self.red_ejector_delay += seconds
        self.blue_ejector_delay += seconds
        self.pass_through_delay += seconds

    @deprecated("In favor of add_ejector_time_offset")
    def add_ejector_offset(self, offset: float) -> None:
        """
        Adds an offset to all ejector delays
        :param offset: The offset to add, in seconds
        :return: None
        """
        self.add_ejector_time_offset(offset)

    def add_ejector_steps_offset(self, steps: int) -> None:
        """
        Adds an offset to all ejector steps
        :param steps: The offset number of steps to add
        :return: None
        """
        self.white_ejector_steps += steps
        self.red_ejector_steps += steps
        self.blue_ejector_steps += steps
        self.pass_through_steps += steps
