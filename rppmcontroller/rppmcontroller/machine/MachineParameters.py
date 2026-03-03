from abc import ABC
from typing import Any, Dict


class MachineParameters(ABC):
    """
    Base class for parameter classes
    """

    @property
    def named_positions(self) -> Dict[str, Any]:
        """
        All named positions, as a mapping of the name to the position.
        What a position is in this context is machine-specific and therefore
        not further specifiable here
        :return: The named positions
        """
        return {}
