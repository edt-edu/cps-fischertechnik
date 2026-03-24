from abc import ABC
from dataclasses import dataclass, field
from typing import Any, Dict

@dataclass
class MachineParameters(ABC):
    """
    Base class for parameter classes
    """

    named_positions: Dict[str, Any] = field(default_factory=dict)
    """
    All named positions, as a mapping of the name to the position.
    What a position is in this context is machine-specific and therefore
    not further specifiable here
    """
