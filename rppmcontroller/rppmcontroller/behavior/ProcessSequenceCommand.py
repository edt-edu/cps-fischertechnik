from typing import Callable, List


class ProcessSequenceCommand:
    """class holding a command (ie. a Callable) to be used by process_sequence 
    it is associated to a displayName in order to simplify logging and feedback
    Attributes:
        __cycleStep (Callable[[], bool]) : Callable (ie. lambda) to the CycleStep function implementing the command
        __displayName (str) : human readable name to be displayed in log or in feedback
    """
    def __init__(self, cycleStep: Callable[[], bool], displayName : str):
        self.__cycleStep: Callable[[], bool] = cycleStep
        self.__displayName: str = displayName


    @property
    def cycleStep(self) -> Callable[[], bool]:
        return self.__cycleStep
    
    @cycleStep.setter
    def cycleStep(self, cycleStep : Callable[[], bool]): 
        self.__cycleStep = cycleStep

    @property
    def displayName(self) -> str:
        return self.__displayName
    
    @displayName.setter
    def displayName(self, displayName: str):
        self.__displayName = displayName