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
    ejector_activation_time: float = 0.5
    """The time in seconds for which an ejector stays activated"""
    mock_analog_sensor: bool = False
    """
    Whether to mock the analog sensor instead of using the hardware one.

    Not every setup requires the analog sensor.
    Setting this to true will randomly pick a color.
    """

    def add_ejector_time_offset(self, seconds: float) -> None:
        """
        Adds an offset to all ejector delays

        :param seconds: The offset to add, in seconds
        :return: None
        """
        self.white_ejector_delay += seconds
        self.red_ejector_delay += seconds
        self.blue_ejector_delay += seconds

    @deprecated("In favor of add_ejector_time_offset")
    def add_ejector_offset(self, offset: float) -> None:
        """
        Adds an offset to all ejector delays
        :param offset: The offset to add, in seconds
        :return: None
        """
        self.add_ejector_time_offset(offset)
