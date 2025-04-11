import logging
import rppmcontroller
import rppmcontroller.machine
import rppmcontroller.machine.conveyorbelt
from rppmcontroller.machine.conveyorbelt.ConveyorBelt import ConveyorBelt
from rppmcontroller.machine.conveyorbelt.ConveyorBeltSimpleSimulator import ConveyorBeltSimpleSimulator
from rppmcontroller.machine.vacuumgripper.VacuumGripper import VacuumGripper
from rppmcontroller.machine.vacuumgripper.VacuumGripperSimpleSimulator import VacuumGripperSimpleSimulator
from rppmcontroller.machine.multiprocessing.MultiProcessing import MultiProcessing
from rppmcontroller.RevPiPyMachineController import RevPiPyMachineController
from rppmcontroller.machine.multiprocessing.MultiProcessingSimpleSimulator import MultiProcessingSimpleSimulator
from rppmcontroller.machine.sortingLine.SortingLine import SortingLine
from rppmcontroller.machine.sortingLine.SortingLineSimpleSimulator import SortingLineSimpleSimulator

class SimulatedMiniFactoryController(RevPiPyMachineController):
    """
    Class allowing to stream commands to and from a simulated conveyor belt
    """

    def __init__(self, configurationFile : str = ""):
        """
        Init method of this class, starts all threads and everything is ready for receiving commands via Sockets and executing them
        """

        super().__init__( configurationFile=configurationFile)
        
        
        self.conveyorBeltMachine = ConveyorBelt("ConveyorBelt01")
        self.vacuumGripperMachine = VacuumGripper("VacuumGripper01")
        self.multiProcessingMachine = MultiProcessing("MultiProcessing01")
        self.sortingLineMachine = SortingLine("SortingLine01")
        
        #the list of all machines that are connected to this core
        self.machines = [self.conveyorBeltMachine,
                         self.vacuumGripperMachine,
                         self.multiProcessingMachine,
                         self.sortingLineMachine
        ]
        
        #dict, which keys are the machines, then there is a tuple holding the function currently executed ([0]) and the id it was sent with ([1])        
        self.currentlyExecuting = {
            self.conveyorBeltMachine: None,
            self.vacuumGripperMachine: None,
            self.multiProcessingMachine: None,
            self.sortingLineMachine: None
        }

        self.machineFeedback = {
            self.conveyorBeltMachine: None,
            self.vacuumGripperMachine: None,
            self.multiProcessingMachine: None,
            self.sortingLineMachine: None
        }
        self.commandFeedback = {
            self.conveyorBeltMachine: None,
            self.vacuumGripperMachine: None,
            self.multiProcessingMachine: None,
            self.sortingLineMachine: None
        }

        self.conveyorBeltSimulator = ConveyorBeltSimpleSimulator(self.conveyorBeltMachine)
        """Simulator for the Conveyor Belt"""

        self.vaccumGripperSimulator = VacuumGripperSimpleSimulator(self.vacuumGripperMachine)
        """Simulator for the Vacuum Gripper"""

        self.multiProcessingSimulator = MultiProcessingSimpleSimulator(self.multiProcessingMachine)
        """Simulator for the MultiProcessingStation"""

        self.sortingLineSimulator = SortingLineSimpleSimulator(self.sortingLineMachine)
        """Simulator for the Sorting Line"""

    def read(self) -> None:
        self.conveyorBeltSimulator.simulatedRead()
        self.vaccumGripperSimulator.simulatedRead()
        self.multiProcessingSimulator.simulatedRead()
        self.sortingLineSimulator.simulatedRead()

    def write(self) -> None:
        self.conveyorBeltSimulator.simulatedWrite()
        self.vaccumGripperSimulator.simulatedWrite()
        self.multiProcessingSimulator.simulatedWrite()
        self.sortingLineSimulator.simulatedWrite()
   
    def reset(self) -> None:
        vg = self.vacuumGripperMachine.resetHelper()
        if vg:
            self.vaccumGripperSimulator.simulatedReset()

if __name__ == "__main__":
    logging.basicConfig(format='%(asctime)s %(levelname)-5s: %(module)-30s,%(lineno)-3s: %(message)s', 
                        level=logging.DEBUG,
                        datefmt='%Y-%m-%d %H:%M:%S')
    handler = logging.FileHandler("logfile.log")
    logFormatter = logging.Formatter("%(levelname)-5s: %(module)-30s,%(lineno)-3s: %(message)s")
    handler.setFormatter(logFormatter)
    logging.getLogger().addHandler(handler)
    # init controller
    root = SimulatedMiniFactoryController("config.yml")
    
    # start communication threads and main control loop
    root.start()