import logging
import rppmcontroller
import rppmcontroller.machine
import rppmcontroller.machine.sortingLine
from rppmcontroller.machine.sortingLine.SortingLine import SortingLine
from rppmcontroller.RevPiPyMachineController import RevPiPyMachineController
from rppmcontroller.machine.sortingLine.SortingLineSimpleSimulator import SortingLineSimpleSimulator

class SimulatedSortingLineController(RevPiPyMachineController):
    """
    Class allowing to stream commands to and from a simulated sorting line
    """

    def __init__(self, configurationFile : str = ""):
        """
        Init method of this class, starts all threads and everything is ready for receiving commands via Sockets and executing them
        """

        super().__init__(configurationFile=configurationFile)
        
        #the list of all machines that are connected to this core
        self.machines = []
        #dict, which keys are the machines, than there is a tuple holding the function currently executed ([0]) and the id it was sent with ([1])
        self.currentlyExecuting = {}
        self.sortingLineMachine = SortingLine("SortingLine01")
        self.machines = [self.sortingLineMachine]
        self.currentlyExecuting = {
            self.sortingLineMachine: [None, None]
        }

        self.feedback = {
            self.sortingLineMachine: None
        }

        self.sortingLineSimulator = SortingLineSimpleSimulator(self.sortingLineMachine)
        """Simulator for the Sorting Line"""

    def read(self) -> None:
        self.sortingLineSimulator.simulatedRead()

    def write(self) -> None:
        self.sortingLineSimulator.simulatedWrite()

if __name__ == "__main__":
    logging.basicConfig(format='%(levelname)-5s: %(module)-20s,%(lineno)-3s: %(message)s', level=logging.DEBUG)
    logging.debug('main')
    # Start ConveyorBeltStreamer app
    root = SimulatedSortingLineController("config.yml")
    
    # start communication threads and main control loop
    root.start()