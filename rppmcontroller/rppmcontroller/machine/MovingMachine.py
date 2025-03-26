from abc import abstractmethod
from rppmcontroller.behavior.CycleStepResult import CycleStepResult
from rppmcontroller.machine.Machine import Machine
from rppmcontroller.machine.Position import Position

#from ThreeDRobotConfig import ThreeDRobotConfig

#from VacuumGripperConfig import VacuumGripperConfig
import logging


# this class should be used for all machines, that have no strict movement path, but can follow various paths
# examples: Warehouse, 3D Robot
class MovingMachine(Machine):

    def __init__(self, id1: str, dictMap: dict) -> None:
        """Init for a moving machine, additionally needs a list of places where pick/place operations could be performed

        :param int id1: the machine id
        :param list placeList: the list of places
        """
        super().__init__(id1, dictMap)
        self._configReached = False
        self._setupFinished = self.setupFinishedHelper = False
        self.start = Position("START", 0, 0, 0)
        self.fin = Position("END", 0, 0, 0)
        self.setupFirst = True
        self.isSetupRunning = False
        self.isSetupDone = False
        

    @property
    def setupFinished(self):
        """Record if a setup has been performed and finished on this machine"""
        return self._setupFinished

    @setupFinished.setter
    def setupFinished(self, value):
        self._setupFinished = value

    

    @abstractmethod
    def setup_CycleStep(self) -> CycleStepResult:
        """Performs the setup of the machine to ensure all counters are correctly set

        :return CycleStepResult: CycleStepResult that indicates if the setup is reached, must continue or terminated
        """
        pass

    @abstractmethod
    def gotoconfig(self, config) -> CycleStepResult:
        """Takes all necessary actions to ensure the machine reaches the specified configuration

        :param config: a configuration object fitting the machine type
        :returns CycleStepResult: CycleStepResult that indicates if the config is reached, must continue or terminated
        """
        pass

    
 