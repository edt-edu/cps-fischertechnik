from enum import Enum


class CycleStepResultEnum(Enum):
    MUST_CONTINUE = 0   # the command is running and the end condition has not been reached
    DONE = 1            # the end condition has been reached successfully
    INTERRUPTED = 2     # another command has replaced this command
    ABORTED_TIMEOUT = 3 # end condition was not reached but a should not continue due to timeout
    ABORTED_ERROR = 4   # should not continue due to an error (parameter, incorrect value)
