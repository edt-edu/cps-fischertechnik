import time
from typing import Optional


class Timer:
    """
    A tool to implement a realtime delay in a machine.
    The timer starts running after the `poll` method is called for the first
    time
    """

    custom_current_time: Optional[float] = None
    """
    Set a custom current time for testing purposes. It is usually safe to set this
    value to zero in order to initialize a test.

    Note that this is a STATIC VARIABLE!
    Settings this value sets it for every Timer instance in the runtime!

    DO NOT FORGET TO RESET THIS VALUE BACK TO `None` AFTER TESTING!
    Otherwise other unit tests might be impacted and fail.
    This is best done by overriding the method unittest.TestCase#tearDown:
    .. highlight:: python
    .. code-block:: python
        def tearDown(self):
            Timer.custom_current_time = None

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

    def __get_current_time(self) -> float:
        """
        Get the current time, usually in seconds since epoch.

        Note that this value can be overridden for testing purposes.

        :return: Current time
        """
        return self.custom_current_time if self.custom_current_time is not None else time.time()

    def elapsed(self) -> bool:
        """
        Check whether the time specified by the timer has elapsed.

        If the Timer has not started yet, an Exception is raised.
        Otherwise, it checks if the set time has elapsed since the call to `start`.

        If the timer is not `single_use` it'll reset itself after it returned
        `True`.

        If the time specified by the timer is zero or negative, this method
        will directly return `True`.
        :return: True if the specified time has elapsed, otherwise False
        """
        if self.__seconds <= 0.0:
            return True

        if self.__start_time is None:
            raise Exception("Timer#elapsed is called without being started!")

        elapsed_seconds = self.__get_current_time() - self.__start_time

        if elapsed_seconds >= self.__seconds:
            if not self.__single_use:
                self.__start_time = None
            return True

        return False

    def reset(self, start: bool = False) -> None:
        """
        Reset this timer. Optionally, start it again.
        :param start: Whether to start the timer again immediately
        :return: None
        """
        if start:
            self.__start_time = self.__get_current_time()
        else:
            self.__start_time = None

    def start(self):
        self.reset(True)

    def is_started(self):
        return self.__start_time is not None
