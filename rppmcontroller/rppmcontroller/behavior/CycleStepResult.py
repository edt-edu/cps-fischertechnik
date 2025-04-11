from __future__ import annotations # place at the very top of the file. fixes pylance type hinting and forward references

from enum import Enum
from typing import Any, Optional, Tuple

from rppmcontroller.behavior.CycleStepResultEnum import CycleStepResultEnum

class CycleStepResult:
    
    """class holding the result of a '_CycleStep' function.
    It indicates if the goal of the CycleStep has been reached or not an if we need to continue to call this CycleStep 
    Attributes:
        _result (CycleStepResultEnum) : enum with the possible status of the function
        _info (str) : human readable information and details about the result
        _subCycleStepResult (Tuple[ProcessSequenceCommand, CycleStepResult]) : the result of the current CycleStep may come form subCycleSteps due to Machine.process_sequence_CycleStep() 
            The tuple stores the last ProcessSequenceCommand and its corresponding CycleStepResult that is used to create self.
    """

    def __init__(self, __result: CycleStepResultEnum, info : str = "", subCycleStepResult : Optional[Tuple[str, CycleStepResult ]] = None):
        self._result: CycleStepResultEnum = __result
        self._info: str = info
        self._subCycleStepResult : Optional[Tuple[str, CycleStepResult]] = subCycleStepResult
    
    @property
    def result(self) -> CycleStepResultEnum:
        return self._result
    
    @result.setter
    def result(self, result : CycleStepResultEnum): 
        self._result = result

    @property
    def info(self) -> str:
        return self._info
    
    @info.setter
    def info(self, info: str):
        self._info = info

    @property
    def subCycleStepResult(self) -> Optional[Tuple[str, CycleStepResult]]:
        return self._subCycleStepResult
    
    @subCycleStepResult.setter
    def subCycleStepResult(self, subCycleStepResult : Optional[Tuple[str, CycleStepResult]]): 
        self._subCycleStepResult = subCycleStepResult

    def must_continue(self) -> bool:
        """Convenience method in order to know if the _CycleStep function must be called again"""
        return self._result == CycleStepResultEnum.MUST_CONTINUE
    

    def is_terminated(self) -> bool:
        """Convenience method in order to know if the _CycleStep function must not continue due to interruption (timeout, error, aborted)"""
        return self._result in (CycleStepResultEnum.INTERRUPTED,
                               CycleStepResultEnum.ABORTED_TIMEOUT,
                               CycleStepResultEnum.ABORTED_ERROR)
    
    def __eq__(self, other: Any) -> bool:
        """
        Compares two CycleStepResult objects for equivalence.

        Args:
            other (Any): The object to compare with.

        Returns:
            bool: True if the objects are equivalent, False otherwise.
        """
        if not isinstance(other, CycleStepResult):
            return False

        if self._subCycleStepResult is None and other._subCycleStepResult is None:
            sub_cycle_step_result_equal = True
        elif self._subCycleStepResult is not None and other._subCycleStepResult is not None:
            sub_cycle_step_result_equal = (
                self._subCycleStepResult[0] is other._subCycleStepResult[0] and
                self._subCycleStepResult[1] == other._subCycleStepResult[1]
            )
        else:
            sub_cycle_step_result_equal = False

        return (
            self._result == other._result and
            self._info == other._info and
            sub_cycle_step_result_equal
        )
    