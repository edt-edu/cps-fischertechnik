
import logging

class CyclicWaiter:
    def __init__(self, cycles):
        self.__count = 0
        self.__cycles = cycles

    def wait(self):
        """
        Waits the defined amount of time and returns True when the given number of cycles is elapsed

        :return: bool: value indicating whether required cycles has passed
        """
        if self.__count < self.__cycles:
            self.__count += 1
            logging.debug(f'cyclic waiter count={self.__count}')
            return False
        else:
            return True

    def reset(self):
        self.__count = 0

