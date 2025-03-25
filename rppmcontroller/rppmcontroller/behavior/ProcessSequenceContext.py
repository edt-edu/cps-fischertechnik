
from typing import List

from rppmcontroller.behavior.CycleStepCommand import CycleStepCommand


class ProcessSequenceContext:
    """class holding the execution context of a process_sequence
    Attributes:
        __subCycleStepList (List[ProcessSequenceCommand]) : Store the list of CycleStepCommand that are processed by a process_sequence
        __currentSubCycleStepIndex (int) : index in the __subCycleStepList of the ProcessSequenceCommand that is currently processed 

    """
    def __init__(self, subCycleStepList: List[CycleStepCommand]):
        self.__subCycleStepList: List[CycleStepCommand] = subCycleStepList
        self.__currentSubCycleStepIndex: int = 0

    @property
    def subCycleStepList(self) -> List[CycleStepCommand]:
        return self.__subCycleStepList
    
    @subCycleStepList.setter
    def subCycleStepList(self, subCycleStepList : List[CycleStepCommand]): 
        self.__subCycleStepList = subCycleStepList

    @property
    def currentSubCycleStepIndex(self) -> int:
        return self.__currentSubCycleStepIndex
    
    @currentSubCycleStepIndex.setter
    def currentSubCycleStepIndex(self, currentSubCycleStepIndex : int):
        self.__currentSubCycleStepIndex = currentSubCycleStepIndex