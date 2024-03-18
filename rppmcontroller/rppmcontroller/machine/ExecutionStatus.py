from enum import Enum


class ExecutionStatus(Enum):
    INACTION = 1
    FINISHED = 2
    ERROR = 3

    MESSAGERECEIVED = 4
    INACTIONWITHOUTPACKAGE = 5
