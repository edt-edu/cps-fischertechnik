import logging
from rppmcontroller.machine.multiprocessing.MultiProcessing import MultiProcessing
from rppmcontroller.machine.RequestedParameter import RequestedParameter
from rppmcontroller.machine.MachineSimpleSimulator import MachineSimpleSimulator


class MultiProcessingSimpleSimulator(MachineSimpleSimulator):
    """A Simple simulator connected to a SortingLine Machine """

    def __init__(self, controlledMultiProcessing : MultiProcessing):
        """Initialize a simulator connected to a SortingLine Machine

        Parameters:
            controlledSortingLine (SortingLine): SortingLine that is connected to this simulator
        """
        self.__controlledMutiProcessing = controlledMultiProcessing

    @property
    def controlledMultiProcessing(self):
        return self.__controlledMutiProcessing
    
    def simulatedRead(self) -> None:        
        logging.debug(f"simulatedRead {self.controlledMultiProcessing.sensorStatusString()} ")

    def simulatedWrite(self) -> None:
        logging.debug(f"simulatedWrite {self.controlledMultiProcessing.sensorStatusString()} ")

    def simulatedReset(self) -> None:
        pass

    def fakeSensor(self, parameter : RequestedParameter, value: bool):
        if parameter == RequestedParameter.REFERENCESWITCHTURNTABLEPOSITOINVACUUM:
            self.controlledMultiProcessing.multiProcessingSensTurntablePosVacuum = value
        elif parameter == RequestedParameter.REFERENCESWITCHTURNTABLEPOSITIONBELT:
            self.controlledMultiProcessing.multiProcessingSensTurntablePosBelt = value
        elif parameter == RequestedParameter.LIGHTBARRIERENDOFCONVEYORBELT:
            self.controlledMultiProcessing.multiProcessingSensEndConveyor = value
        elif parameter == RequestedParameter.REFERENCESWITCHTURNTABLEPOSITIONSAW:
            self.controlledMultiProcessing.multiProcessingSensTurntablePosSaw = value
        elif parameter == RequestedParameter.REFERENCESWITCHVACUUMPOSITIONTURNTABLE:
            self.controlledMultiProcessing.multiProcessingSensVacuumGripperAtTurntable = value
        elif parameter == RequestedParameter.REFERENCESWITCHOVENFEEDERINSIDE:
            self.controlledMultiProcessing.multiProcessingSensOvenFeederIn = value
        elif parameter == RequestedParameter.REFERENCESWITCHOVENFEEDEROUTSIDE:
            self.controlledMultiProcessing.multiProcessingSensOvenFeederOut = value
        elif parameter == RequestedParameter.REFERENCESWITCHVACUUMPOSITIONOVEN:
            self.controlledMultiProcessing.multiProcessingSensVacuumGripperAtOven = value
        elif parameter == RequestedParameter.LIGHTOVEN:
            self.controlledMultiProcessing.multiProcessingOvenLight = value
        else :
            logging.warning("Wrong parameter in fakeSensor function")

    def getCounter(self):
        return self.controlledMultiProcessing.current