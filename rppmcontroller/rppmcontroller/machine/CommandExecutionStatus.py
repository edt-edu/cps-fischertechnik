from enum import Enum


class CommandExecutionStatus(Enum):
    FEEDBACK_ERROR = 0
    SUCCESS = 1
    RUNNING = 2
    ABORTED = 3
    ABORTED_TIMEOUT = 4
    IGNORED = 5