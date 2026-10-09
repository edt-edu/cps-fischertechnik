from __future__ import annotations

import logging
import math
from abc import abstractmethod, ABC
from time import time
from typing import Any, Dict, List, Optional

from typing_extensions import deprecated

from rppmcontroller.behavior.CycleStepCommand import CycleStepCommand
from rppmcontroller.behavior.CycleStepResult import CycleStepResult
from rppmcontroller.behavior.CycleStepResultEnum import CycleStepResultEnum
from rppmcontroller.behavior.ProcessSequenceContext import \
    ProcessSequenceContext
from rppmcontroller.machine.MachineParameters import MachineParameters
from rppmcontroller.machine.MachineStatus import MachineStatus
from rppmcontroller.machine.ParameterRequestAnswer import \
    ParameterRequestAnswer
from rppmcontroller.machine.RequestedParameter import RequestedParameter


class Machine(ABC):
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
        self.__isInitialized = False
        self.__lastExecutionTime = -math.inf
        self.__nbMinimumRequiredExecutionCycles = 0 # number of cycles (ie. IO read/write, before considering the execution done)
        self.__processSequenceContext : Optional[ProcessSequenceContext] =  None

    @property
    def id(self) -> str:
        return self.__id

    @property
    def parameters(self) -> MachineParameters | None:
        return None

    #use for feedbackOnChange
    @property
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
    def isInitialized(self) -> bool:
        """Returns whether the machine is initialized . ie if the setup is Done

        :return bool: the setup status
        """
        return self.__isInitialized

    @isInitialized.setter
    def isInitialized(self, value: bool):
        self.__isInitialized = value

    def on_command_started(self) -> None:
        """Called by the controller when a new command starts executing on this machine, before its first cycle.

        Machines keeping per-command state (e.g. axis-movement monitoring) reset it here.
        """


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

    @property
    def processSequenceContext(self) -> Optional[ProcessSequenceContext]:
        """Returns whether the machine context of the currently performing actions

        :return Optional[ProcessSequenceContext]: the processSequenceContext

        """
        return self.__processSequenceContext

    @processSequenceContext.setter
    def processSequenceContext(self, value: Optional[ProcessSequenceContext] ):
        self.__processSequenceContext = value

    def timeSinceExecution(self):
        if self.__isExecuting:
            return 0
        else:
            return time() - self.__lastExecutionTime

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
    def stop_CycleStep(self) -> CycleStepResult:
        """
        Used to stop the execution immediately by setting all outputs to false

        :return: as a CycleStep, this function must return True when it is finished so it can be removed from the currentlyExecuting map
        """
        pass



    @deprecated("in favor of Runner class")
    def process_sequence_CycleStep(self, subCycleStepList: List[CycleStepCommand]) -> CycleStepResult:
        """
        perform each subCycleStep one after another as soon as the previous one ha indicated it had finished.
        Note: Only one subCycleStep can be performe in a cycle.
        Note: despite compatible signature, nested process_sequence_CycleStep are NOT allowed (it would require stack management)


        :param List[Callable[[], bool] subCycleStepList: list of Callable that should be processed
        :return: as a CycleStep, this function must return True when all subCycleStep have finished so this 'process_sequnece' can be removed from the currentlyExecuting map
        """


        if self.__processSequenceContext is not None:
            # we are currently processing a sequence

            # check if this is the same
            if self.__processSequenceContext.subCycleStepList != subCycleStepList:
                logging.error(f"calling process_sequence_CycleStep on a machine with 2 different subCycleStepList, \n{self.__processSequenceContext.subCycleStepList} != {subCycleStepList}")
                raise Exception(f"Invalid subCycleList when calling process_sequence_CycleStep() on machine {self.id}")
        else:
            self.__processSequenceContext =  ProcessSequenceContext(subCycleStepList)
            psContext = self.__processSequenceContext
            logging.info(f"starting subCycleStep {psContext.currentSubCycleStepIndex+1}/{len(psContext.subCycleStepList)} : {self.id}.{psContext.subCycleStepList[psContext.currentSubCycleStepIndex].displayName}")


        # call current subCycleStep
        psContext = self.__processSequenceContext
        subCommand = psContext.subCycleStepList[psContext.currentSubCycleStepIndex]
        res = subCommand.cycleStep()
        # analyse result
        if not res.must_continue():
            if not res.is_terminated():

                logging.debug(f"normal end of subCycleStepCommand reached {psContext.currentSubCycleStepIndex}/{len(psContext.subCycleStepList)}")
                if psContext.currentSubCycleStepIndex+1 >= len(psContext.subCycleStepList):
                    # finished processing this sequence
                    self.__processSequenceContext = None
                    logging.debug(f"process_sequence_CycleStep {self.id} last sub command done ")
                    return CycleStepResult(CycleStepResultEnum.DONE,
                                            f"process_sequence_CycleStep {psContext.currentSubCycleStepIndex+1}/{len(psContext.subCycleStepList)}"
                                            f" : {psContext.subCycleStepList[psContext.currentSubCycleStepIndex].displayName}",
                                            (subCommand.displayName, res))
                else:
                    # proceed to next subCycleStep
                    psContext.currentSubCycleStepIndex = psContext.currentSubCycleStepIndex+1
                    logging.info(f"starting subCycleStep {psContext.currentSubCycleStepIndex+1}/{len(psContext.subCycleStepList)} : {self.id}.{psContext.subCycleStepList[psContext.currentSubCycleStepIndex].displayName}")
                    return CycleStepResult(CycleStepResultEnum.MUST_CONTINUE,
                                            f"process_sequence_CycleStep {psContext.currentSubCycleStepIndex+1}/{len(psContext.subCycleStepList)}"
                                            f" : {self.id}.{psContext.subCycleStepList[psContext.currentSubCycleStepIndex].displayName}",
                                            (subCommand.displayName, res))
            else:
                # transfert termination result to upper CycleStep
                return CycleStepResult(res.result,
                                        f"process_sequence_CycleStep terminated due to result of {psContext.currentSubCycleStepIndex+1}/{len(psContext.subCycleStepList)}"
                                        f" : {self.id}.{psContext.subCycleStepList[psContext.currentSubCycleStepIndex].displayName}",
                                        (subCommand.displayName, res))
        else:
            # use same text etc as in "proceed to next subCycleStep" in order to avoid multiple notifications due to comparison mismatch
            return CycleStepResult(CycleStepResultEnum.MUST_CONTINUE,
                                    f"process_sequence_CycleStep {psContext.currentSubCycleStepIndex+1}/{len(psContext.subCycleStepList)}"
                                    f" : {self.id}.{psContext.subCycleStepList[psContext.currentSubCycleStepIndex].displayName}",
                                    (subCommand.displayName, res))

    @deprecated("in favor of Runner class")
    def isProcessingSequence(self) -> bool:
        """
        Indicates if process_sequence_CycleStep is currently processing a sequence
        """
        return self.__processSequenceContext is not None

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

    def machineFeedback(self):
        if not self.isInitialized:
            if self.isExecuting:
                return MachineStatus.UNINITIALIZED_ACTIVE
            else:
                return MachineStatus.UNINITIALIZED_IDLE
        else:
            if self.isExecuting:
                return MachineStatus.INITIALIZED_ACTIVE
            else:
                return MachineStatus.INITIALIZED_IDLE

    def __str__(self):
        return f"Machine({self.id})"



