from abc import ABC
from typing import Any, Dict


class MachineParameters(ABC):
    """
    Base class for parameter classes
    """

    def __init__(self):
        self.named_positions: Dict[str, Any] = {}
        """
        All named positions, as a mapping of the name to the position.
        What a position is in this context is machine-specific and therefore
        not further specifiable here
        """
