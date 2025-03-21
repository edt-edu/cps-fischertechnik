
from typing import Callable, List

from rppmcontroller.behavior.ProcessSequenceCommand import ProcessSequenceCommand


class ProcessSequenceContext:
    """class holding the execution context of a process_sequence
    Attributes:
        __subCycleStepList (List[ProcessSequenceCommand]) : Store the list of CycleStepCommand that are processed by a process_sequence
        __currentSubCycleStepIndex (int) : index in the __subCycleStepList of the ProcessSequenceCommand that is currently processed 

    """
    def __init__(self, subCycleStepList: List[ProcessSequenceCommand]):
        self.__subCycleStepList: List[ProcessSequenceCommand] = subCycleStepList
        self.__currentSubCycleStepIndex: int = 0

    @property
    def subCycleStepList(self) -> List[ProcessSequenceCommand]:
        return self.__subCycleStepList
    
    @subCycleStepList.setter
    def subCycleStepList(self, subCycleStepList : List[ProcessSequenceCommand]): 
        self.__subCycleStepList = subCycleStepList

    @property
    def currentSubCycleStepIndex(self) -> int:
        return self.__currentSubCycleStepIndex
    
    @currentSubCycleStepIndex.setter
    def currentSubCycleStepIndex(self, currentSubCycleStepIndex : int):
        self.__currentSubCycleStepIndex = currentSubCycleStepIndex