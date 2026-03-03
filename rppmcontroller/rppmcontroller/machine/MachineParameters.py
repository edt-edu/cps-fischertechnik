from abc import ABC
from typing import Any


class MachineParameters(ABC):
    """
    Base class for parameter classes
    """

    @property
    def named_positions(self) -> dict[str, Any]:
        """
        All named positions, as a mapping of the name to the position.
        What a position is in this context is machine-specific and therefore
        not further specifiable here
        :return: The named positions
        """
        return {}
