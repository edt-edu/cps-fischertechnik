import math
from abc import abstractmethod
from time import time
from rppmcontroller.machine.RequestedParameter import RequestedParameter
from rppmcontroller.machine.ParameterRequestAnswer import ParameterRequestAnswer
from rppmcontroller.machine.ExecutionStatus import ExecutionStatus
import logging


class Machine:
    """
    Superclass for all machines in the factory.

    Specifies the machine id (should be an individual string for all machines)
    and the dictMap, which holds the connection between the machine-parameters
    and the names their status can be Requested with
    """
    def __init__(self, id1: str, dictMap: dict):
        self.__id = id1
        self.__dictMap = dictMap
        self.__isExecuting = False
        self.__fakeIsExecuting = False
        self.__lastExecutionTime = -math.inf
        self.__isExecutingCount = 0
        self.setFakeExecuting = False

    @property
    def id(self) -> str:
        return self.__id

    #use for feedbackOnChange
    @property
    @abstractmethod
    def isExecuting(self) -> bool:
        """Returns whether the machine is currently performing actions

        :return bool: the executing status
        """
        return self.__isExecuting
        #pass

    #use only in exLoop
    @property
    @abstractmethod
    def fakeIsExecuting(self) -> bool:
        """Returns whether the machine should currently be performing actions

        :return bool: the fake executing status
        """
        if self.isExecuting or self.setFakeExecuting:
            logging.debug("is executing true")
            self.setFakeExecuting = False
            self.__isExecutingCount = 0
            return True
        else:
            logging.debug('isexecutingCount ' + str(self.__isExecutingCount))
            if self.__isExecutingCount < 15:
                self.__isExecutingCount += 1
                logging.debug('fake is executing true')
                return True
            else:
                logging.debug('is executing false')
                return False

    @isExecuting.setter
    def isExecuting(self, value: bool):
        self.__isExecuting = value
        if value:
            self.__lastExecutionTime = time()

    def timeSinceExecution(self):
        if self.__isExecuting:
            return 0
        else:
            return time() - self.__lastExecutionTime

    def execute(self, *args):
        pass

    @abstractmethod
    def stop(self):
        """
        Used to stop the execution immediately by setting all outputs to false
        """
        pass

    def request(self, params: list) -> list:
        """
        Used to get the values of requested machine parameters

        :param params: A list with the requested parameters
        :return: A list of tuples of the parameter and its corresponding value
        """
        logging.debug('called request with: %s', params)
        result = []
        for p in params:
            if p == RequestedParameter.ALL:
                for key in self.__dictMap.keys():
                    result.append(ParameterRequestAnswer(key, self.__dictMap[key], self.__dictMap[key].__class__))
            else:
                try:
                    result.append(ParameterRequestAnswer(p, self.__dictMap[p], self.__dictMap[p].__class__))
                except KeyError:
                    print("unknown attribute")
        return result

    #TODO implement me
    def feedback(self):
        if self.isExecuting:
            return ExecutionStatus.INACTION
        else:
            return ExecutionStatus.FINISHED

    def fakeFeedback(self):
        if self.fakeIsExecuting:
            return ExecutionStatus.INACTION
        else:
            return ExecutionStatus.FINISHED
