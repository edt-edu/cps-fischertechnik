import logging
from rppmcontroller.machine.highbay.HighBay import HighBay
from rppmcontroller.machine.highbay.HighBaySimpleSimulator import HighBaySimpleSimulator
from rppmcontroller.RevPiPyMachineController import RevPiPyMachineController

class SimulatedHighBayController(RevPiPyMachineController):
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
        self.highBayMachine = HighBay("HighBay01")
        self.machines = [self.highBayMachine]
        self.currentlyExecuting = {
            self.highBayMachine: None
        }

        self.machineFeedback = {
            self.highBayMachine: None
        }

        self.commandFeedback = {
            self.highBayMachine: None
        }

        self.highBaySimulator = HighBaySimpleSimulator(self.highBayMachine)
        """Simulator for the HighBay"""

    def read(self) -> None:
        self.highBaySimulator.simulatedRead()

    def write(self) -> None:
        self.highBaySimulator.simulatedWrite()
   
    def reset(self) -> None:
        if self.highBayMachine.must_reset:
            self.highBaySimulator.simulatedReset()
            self.highBayMachine.must_reset = False

if __name__ == "__main__":
    logging.basicConfig(format='%(asctime)s %(levelname)-5s: %(module)-30s,%(lineno)-3s: %(message)s', 
                        level=logging.DEBUG,
                        datefmt='%Y-%m-%d %H:%M:%S')
    handler = logging.FileHandler("logfile.log")
    logFormatter = logging.Formatter("%(levelname)-5s: %(module)-30s,%(lineno)-3s: %(message)s")
    handler.setFormatter(logFormatter)
    logging.getLogger().addHandler(handler)
    # Start ConveyorBeltStreamer app
    root = SimulatedHighBayController("config.yml")
    
    # start communication threads and main control loop
    root.start()