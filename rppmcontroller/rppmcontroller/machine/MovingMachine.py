from abc import abstractmethod
from rppmcontroller.machine.Machine import Machine
from rppmcontroller.machine.Position import Position

#from ThreeDRobotConfig import ThreeDRobotConfig

#from VacuumGripperConfig import VacuumGripperConfig
import logging


# this class should be used for all machines, that have no strict movement path, but can follow various paths
# examples: Warehouse, 3D Robot
class MovingMachine(Machine):

    @property
    @abstractmethod
    def isExecuting(self) -> bool:
        pass

    def __init__(self, id1: str, dictMap: dict) -> None:
        """Init for a moving machine, additionally needs a list of places where pick/place operations could be performed

        :param int id1: the machine id
        :param list placeList: the list of places
        """
        super().__init__(id1, dictMap)
        self.__configGoal = None
        self.__configReached = False
        self.__setupFinished = self.setupFinishedHelper = False
        self.__pc = 0
        self.__moveList = []
        self.start = Position("START", 0, 0, 0)
        self.fin = Position("END", 0, 0, 0)
        self.setupFirst = True

    @property
    def setupFinished(self):
        """Record if a setup has been performed and finished on this machine"""
        return self.__setupFinished

    @setupFinished.setter
    def setupFinished(self, value):
        self.__setupFinished = value

    @abstractmethod
    def generateTransferMoveList(self, numPickup: Position, numPlace: Position) -> list:
        """Uses the specified Postions as the Pickup location or Place location to create a move list

        :param Position numPickup: place object is picked up at
        :param Position numPlace: place object is dropped up at

        :returns: configs specific to the machine to travel between the places
        :rtype: list
        """
        pass

    @abstractmethod
    def setup(self) -> bool:
        """Performs the setup of the machine to ensure all counters are correctly set

        :return bool: True if the setup is finished
        """
        pass

    @abstractmethod
    def gotoconfig(self, config) -> bool:
        """Takes all necessary actions to ensure the machine reaches the specified config

        :param config: a config object fitting the machine type
        :returns bool: True if the config is reached
        """
        pass

    
    def execute(self, start: Position, fin: Position, moveList: list) -> None:
        """execute performs the action indicated by the input numbers to move the product between the two specified places

        :param Position start: see method generate transferMoveList
        :param Position fin: see method generate transferMoveList
        :param list moveList:
        """
        logging.debug("execute")
        super().execute(start, fin, moveList) # ensure that the nbMinimumRequiredExecutionCycles is updated

        #Reset pc for new move list if input from start or target position changes
        if self.start != start or self.fin != fin:
            self.start = start
            self.fin = fin
            self.__pc = 0
        #TODO also reset pc when re-executing
        if not self.__setupFinished:
            logging.debug('setup from execute')
            #self.isExecuting = True
            #self.__setupFinished = self.setup()
            self.setup()
            self.setupFinishedHelper = self.__setupFinished
            self.__configReached = True
        else:
            logging.debug('populating move list')
            self.__moveList = []
            ########################################
            # TODO check this change and think about the above reset functionalities
            #self.__moveList.extend(self.generateTransferMoveList(start,fin))
            self.__moveList.extend(moveList)
            ########################################
            #print(self.__moveList)
            #fahre zur position
            if self.__configReached:
                logging.debug("config reached")
                logging.info(f'moveList size {len(self.__moveList)}')
                self.__configReached = False
                #weitere moves vorhanden
                if self.__pc < len(self.__moveList):
                    logging.debug(f"next move {self.__moveList[self.__pc]}")
                    self.__configGoal = self.__moveList[self.__pc]
                    self.__pc += 1
                    logging.debug('new pc is ' + str(self.__pc))
                else:
                    #TODO reactivate if necessary self.__pc = 0
                    pass
            self.__configReached = self.gotoconfig(self.__configGoal)
            logging.debug(f'nbRemainingMove {self.nbRemainingMove()}/{len(self.__moveList)}')

    @property
    def pc(self) -> int:
        """Returns the pc indicating the current executing position in the move list

        :returns: the pc value
        :rtype: int
        """
        return self.__pc


    @pc.setter
    def pc(self, value):
        self.__pc = value

    def nbMove(self) -> int:
        """Total number of moves that the Machine has to perform"""
        return len(self.__moveList)
    
    def nbRemainingMove(self) -> int:
        """Number of remaining moves that the Machine has to perform"""
        if self.__moveList is not None:
            return len(self.__moveList) - self.pc 
        else:
            return 0
    
    def hasRemainingMove(self) -> bool:
        """The Machine has some remaining move to perform"""
        return self.nbRemainingMove() != 0
