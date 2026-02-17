import logging
import rppmcontroller.machine
import rppmcontroller.machine.multiprocessing
from rppmcontroller.machine.multiprocessing.MultiProcessing import MultiProcessing
from rppmcontroller.RevPiPyMachineController import RevPiPyMachineController
from rppmcontroller.machine.multiprocessing.MultiProcessingParameters import \
    MultiProcessingParameters
from rppmcontroller.machine.multiprocessing.MultiProcessingSimpleSimulator import MultiProcessingSimpleSimulator

class SimulatedMultiProcessingController(RevPiPyMachineController):
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
        self.multiProcessingMachine = MultiProcessing("MultiProcessing01")

        self.machines = [self.multiProcessingMachine]
        self.currentlyExecuting = {
            self.multiProcessingMachine: None
        }

        self.machineFeedback = {
            self.multiProcessingMachine: None
        }

        self.commandFeedback = {
            self.multiProcessingMachine: None
        }

        self.multiProcessingSimulator = MultiProcessingSimpleSimulator(self.multiProcessingMachine)
        """Simulator for the Sorting Line"""

    def read(self) -> None:
        self.multiProcessingSimulator.simulatedRead()

    def write(self) -> None:
        self.multiProcessingSimulator.simulatedWrite()

    def reset(self) -> None:
        pass

if __name__ == "__main__":
    logging.basicConfig(format='%(asctime)s %(levelname)-5s: %(module)-30s,%(lineno)-3s: %(message)s',
                        level=logging.DEBUG,
                        datefmt='%Y-%m-%d %H:%M:%S')
    handler = logging.FileHandler("logfile.log")
    logFormatter = logging.Formatter("%(levelname)-5s: %(module)-30s,%(lineno)-3s: %(message)s")
    handler.setFormatter(logFormatter)
    logging.getLogger().addHandler(handler)
    # Start ConveyorBeltStreamer app
    root = SimulatedMultiProcessingController("config.yml")

    # start communication threads and main control loop
    root.start()
