import logging
import rppmcontroller
import rppmcontroller.machine
import rppmcontroller.machine.conveyorbelt
from rppmcontroller.machine.conveyorbelt.ConveyorBelt import ConveyorBelt
from rppmcontroller.example.ConveyorBeltController import ConveyorBeltController
from rppmcontroller.machine.conveyorbelt.ConveyorBeltSimpleSimulator import ConveyorBeltSimpleSimulator

class SimulatedConveyorBeltController(ConveyorBeltController):
    """
    Class allowing to stream commands to and from a simulated conveyor belt
    """

    def __init__(self, configurationFile : str = ""):
        """
        Init method of this class, starts all threads and everything is ready for receiving commands via Sockets and executing them
        """

        super().__init__(simulatedRevPiModIO=True, configurationFile=configurationFile)
        
        #the list of all machines that are connected to this core
        self.machines = []
        #dict, which keys are the machines, than there is a tuple holding the function currently executed ([0]) and the id it was sent with ([1])
        self.currentlyExecuting = {}
        self.conveyorBeltMachine = ConveyorBelt("ConveyorBelt01")
        self.machines = [self.conveyorBeltMachine]
        self.currentlyExecuting = {
            self.conveyorBeltMachine: [None, None]
        }

        self.feedback = {
            self.conveyorBeltMachine: None
        }

        self.conveyorBeltSimulator = ConveyorBeltSimpleSimulator(self.conveyorBeltMachine)
        """Simulator for the Conveyor Belt"""

    def read(self) -> None:
        self.conveyorBeltSimulator.simulatedRead()

    def write(self) -> None:
        self.conveyorBeltSimulator.simulatedWrite()

if __name__ == "__main__":
    logging.basicConfig(format='%(asctime)s %(levelname)-5s: %(module)-30s,%(lineno)-3s: %(message)s', 
                        level=logging.DEBUG,
                        datefmt='%Y-%m-%d %H:%M:%S')
    handler = logging.FileHandler("logfile.log")
    logFormatter = logging.Formatter("%(levelname)-5s: %(module)-30s,%(lineno)-3s: %(message)s")
    handler.setFormatter(logFormatter)
    logging.getLogger().addHandler(handler)
    # Start ConveyorBeltStreamer app
    root = SimulatedConveyorBeltController("config.yml")
    
    # start communication threads and main control loop
    root.start()