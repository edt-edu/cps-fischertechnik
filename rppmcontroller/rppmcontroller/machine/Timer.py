import time
from typing import Optional


class Timer:
    """
    A tool to implement a realtime delay in a machine.
    The timer starts running after the `poll` method is called for the first
    time
    """

    def __init__(self, seconds: float, single_use: bool = False):
        """
        Create a new timer with a delay in seconds
        :param seconds: The delay in seconds
        :param single_use: Whether the timer can only be used once
        """
        self.__seconds: float = seconds
        self.__single_use: bool = single_use
        self.__start_time: Optional[float] = None

    def elapsed(self) -> bool:
        """
        Check whether the time specified by the timer has elapsed.

        Calling this method either starts the timer, if it has not started yet
        or checks if it has elapsed.

        If the timer is not `single_use` it'll reset itself after it returned
        `True`.

        If the time specified by the timer is zero or negative, this method
        will directly return `True`.
        :return: True if the specified time has elapsed, otherwise False
        """
        if self.__seconds <= 0.0:
            return True

        if self.__start_time is None:
            self.__start_time = time.time()
            return False

        elapsed_seconds = time.time() - self.__start_time

        if elapsed_seconds >= self.__seconds:
            if not self.__single_use:
                self.__start_time = None
            return True

        return False

    def reset(self, start: bool = False) -> None:
        """
        Reset this timer. Optionally start it again.
        :param start: Whether to start the timer again immediately
        :return: None
        """
        if start:
            self.__start_time = time.time()
        else:
            self.__start_time = None
