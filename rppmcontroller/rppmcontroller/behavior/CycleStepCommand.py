from __future__ import annotations  # place at the very top of the file. fixes pylance type hinting and forward references

from typing import Callable

from rppmcontroller.behavior.CycleStepResult import CycleStepResult


class CycleStepCommand:
    """class holding a command (i.e. a Callable) to be used by process_sequence
    it is associated to a displayName in order to simplify logging and feedback
    and the commandId that is the origin for the command
    Attributes:
        _cycleStep (Callable[[], CycleStepResult]) : Callable (i.e. lambda)
        to the CycleStep function implementing the command
        _displayName (str) : human-readable name
        to be displayed in log or in feedback, or a supplier for such a name
        _commandId (str) : protocol commandId that triggered this command
    """

    def __init__(self,
                 cycleStep: Callable[[], CycleStepResult],
                 displayName: str,
                 commandId: str = ""):
        self._cycleStep: Callable[[], CycleStepResult] = cycleStep
        self._displayName: str = displayName
        self._commandId: str = commandId

    @property
    def cycleStep(self) -> Callable[[], CycleStepResult]:
        return self._cycleStep

    @cycleStep.setter
    def cycleStep(self, cycleStep: Callable[[], CycleStepResult]):
        self._cycleStep = cycleStep

    @property
    def displayName(self) -> str:
        return self._displayName

    @displayName.setter
    def displayName(self, displayName: str):
        self._displayName = displayName

    @property
    def commandId(self) -> str:
        return self._commandId

    @commandId.setter
    def commandId(self, commandId: str):
        self._commandId = commandId

    def __repr__(self):
        return (f"CycleStepCommand(displayName={self._displayName}, "
                f"commandId={self._commandId})")
