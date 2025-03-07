import math
from abc import abstractmethod
from time import time
from typing import Any, Dict
from rppmcontroller.machine.RequestedParameter import RequestedParameter
from rppmcontroller.machine.ParameterRequestAnswer import ParameterRequestAnswer
from rppmcontroller.machine.MachineStatus import MachineStatus
from rppmcontroller.machine.CommandExecutionStatus import CommandExecutionStatus
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
        self.__isCommandSuccessed = False
        self.__isCommandRunning = False
        self.__isCommandTimedOut = False
        self.__isSetupRunning = False
        self.__isSetupDone = False
        self.__lastExecutionTime = -math.inf
        self.__nbMinimumRequiredExecutionCycles = 0 # number of cycles (ie. IO read/write, before considering the execution done)

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

    @isExecuting.setter
    def isExecuting(self, value: bool):
        self.__isExecuting = value
        if value:
            self.__lastExecutionTime = time()

    def machineTypeName(self) -> str:
        return self.__class__.__name__
    
    @property
    @abstractmethod
    def isCommandSuccessed(self) -> bool:
        """Returns whether the command successed

        :return bool: the command status
        """
        return self.__isCommandSuccessed

    @isCommandSuccessed.setter
    def isCommandSuccessed(self, value: bool):
        self.__isCommandSuccessed = value

    @property
    @abstractmethod
    def isCommandRunning(self) -> bool:
        """Returns whether the command Running

        :return bool: the command status
        """
        return self.__isCommandRunning

    @isCommandRunning.setter
    def isCommandRunning(self, value: bool):
        self.__isCommandRunning = value

    @property
    @abstractmethod
    def isCommandTimedOut(self) -> bool:
        """Returns whether the command TimedOut

        :return bool: the command status
        """
        return self.__isCommandTimedOut

    @isCommandTimedOut.setter
    def isCommandTimedOut(self, value: bool):
        self.__isCommandTimedOut = value

    @property
    @abstractmethod
    def isSetupRunning(self) -> bool:
        """Returns whether the setup is running

        :return bool: the setup status
        """
        return self.__isSetupRunning

    @isSetupRunning.setter
    def isSetupRunning(self, value: bool):
        self.__isSetupRunning = value

    @property
    @abstractmethod
    def isSetupDone(self) -> bool:
        """Returns whether the setup is Done

        :return bool: the setup status
        """
        return self.__isSetupDone

    @isSetupDone.setter
    def isSetupDone(self, value: bool):
        self.__isSetupDone = value
    
    
    @property
    def nbMinimumRequiredExecutionCycles(self) -> int:
        """Returns the number of cycles still required before considerring the current execution being done
        when > 0 this condition can be used to help ensuring that at least this number of IO read, execute,  IO write is performed before 
        setting the isExecuting back to FINISHED
        """
        return self.__nbMinimumRequiredExecutionCycles
    
    @nbMinimumRequiredExecutionCycles.setter
    def nbMinimumRequiredExecutionCycles(self,value: int) -> None:
        self.__nbMinimumRequiredExecutionCycles = value

    def timeSinceExecution(self):
        if self.__isExecuting:
            return 0
        else:
            return time() - self.__lastExecutionTime

    def execute(self, *args):
        pass

    @abstractmethod
    def sensorStatusString(self) -> str:
        """return a human readable version of the sensors values

        most used for logging and testing purposes
        """
        return "(not implemented)"

    @abstractmethod
    def actuatorStatusString(self) -> str:
        """return a human readable version of the actuator values

        most used for logging and testing purposes
        """
        return "(not implemented)"

    @abstractmethod
    def inputStatus(self) -> Dict[str, Any]:
        """return a dict of input of the machine
        Can be used to build MQTT messages
        """
        pass

    @abstractmethod
    def outputStatus(self) -> Dict[str, Any]:
        """return a dict of output of the machine
        Can be used to build MQTT messages
        """
        pass
    
    @abstractmethod
    def internalStatus(self) -> Dict[str, Any]:
        """return a dict of internal values of the machine
        Note: it may contain nested dictionnaries 
        Can be used to build MQTT messages
        """
        pass


    def incrementNbMinimumRequiredExecutionCycles(self) -> None:
        """increment nbMinimumRequiredExecutionCycles. """
        self.__nbMinimumRequiredExecutionCycles += 1

    def decrementNbMinimumRequiredExecutionCycles(self) -> None:
        """decrement nbMinimumRequiredExecutionCycles. """
        if self.__nbMinimumRequiredExecutionCycles > 0:
            self.__nbMinimumRequiredExecutionCycles -= 1

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
            p = RequestedParameter[p]
            if p == RequestedParameter.ALL:
                for key in self.__dictMap.keys():
                    result.append(ParameterRequestAnswer(key, self.__dictMap[key], self.__dictMap[key].__class__))
            else:
                try:
                    result.append(ParameterRequestAnswer(p, self.__dictMap[p], self.__dictMap[p].__class__))
                except KeyError:
                    print("unknown attribute")
        return result

    def feedback(self):
        if self.isExecuting:
            return MachineStatus.INACTION
        else:
            return MachineStatus.FINISHED
        
    def commandFeedback(self):
        if self.isCommandSuccessed:
            self.isCommandRunning = False #reset variable
            return CommandExecutionStatus.SUCCESS
        elif self.isCommandTimedOut:
            self.isCommandRunning = False #reset variable
            return CommandExecutionStatus.ABORTED_TIMEOUT
        elif self.isSetupDone:
            self.isSetupRunning = False 
            self.isSetupDone = False #ensure only one message is sent
            return CommandExecutionStatus.SETUP_DONE
        elif self.isSetupRunning:
            return CommandExecutionStatus.SETUP_RUNNING
        elif self.isCommandRunning:
            return CommandExecutionStatus.RUNNING
        else:
            return CommandExecutionStatus.FEEDBACK_ERROR



