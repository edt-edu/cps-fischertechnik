from dataclasses import dataclass
from typing import Optional

from rppmcontroller.machine.MachineParameters import MachineParameters


@dataclass
class MultiProcessingParameters(MachineParameters):
    safety_at_oven: Optional[bool] = None
    """
    Whether the safety position for the arm is considered to be at the oven or
    not.

    When not set, a setup is considered to be "safe".
    """
