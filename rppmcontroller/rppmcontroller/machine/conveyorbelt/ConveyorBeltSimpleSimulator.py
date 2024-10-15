import logging
from rppmcontroller.machine.conveyorbelt.ConveyorBelt import ConveyorBelt
from rppmcontroller.machine.RequestedParameter import RequestedParameter
from rppmcontroller.machine.MachineSimpleSimulator import MachineSimpleSimulator


class ConveyorBeltSimpleSimulator(MachineSimpleSimulator):
    """A Simple simulator connected to a ConveyorBelt Machine """

    def __init__(self, controlledConveyorBelt : ConveyorBelt):
        """Initialize a simulator connected to a ConveyorBelt Machine

        Parameters:
            controlledConveyorBelt (ConveyorBelt): ConveyorBelt that is connected to this simulator
        """
        self.__controlledConveyorBelt = controlledConveyorBelt
        #True is the value when there is no object in front of light sensors
        self.controlledConveyorBelt.conveyorSensSwap = True
        self.controlledConveyorBelt.conveyorSensFeed = True

        self.previous_simulatedReadLog = None
        self.previous_simulatedWriteLog = None

    @property
    def controlledConveyorBelt(self):
        return self.__controlledConveyorBelt
    
    def simulatedRead(self) -> None:  
        simulatedReadLog = f"simulatedRead  {self.controlledConveyorBelt.sensorStatusString()} "
        if simulatedReadLog != self.previous_simulatedReadLog :
            logging.debug(simulatedReadLog)
            self.previous_simulatedReadLog = simulatedReadLog      


    def simulatedWrite(self) -> None:
        if not (self.controlledConveyorBelt.conveyorActForward and self.controlledConveyorBelt.conveyorActBackward):
            # if both conveyorActForward and conveyorActBackward are True -> they cancel each other (no move) 
            # if moving (either direction) alternate impulse signal
            if self.controlledConveyorBelt.conveyorActForward:
                self.controlledConveyorBelt.conveyorSensImpulse += 1
            if self.controlledConveyorBelt.conveyorActBackward:
                self.controlledConveyorBelt.conveyorSensImpulse += 1
        
        
        simulatedWriteLog = f"simulatedWrite  {self.controlledConveyorBelt.sensorStatusString()} "
        if simulatedWriteLog != self.previous_simulatedWriteLog :
            logging.debug(simulatedWriteLog)
            self.previous_simulatedWriteLog = simulatedWriteLog 

    def simulatedReset(self) -> None:
        pass

    def fakeSensor(self, parameter : RequestedParameter, value: bool):
        if parameter == RequestedParameter.LIGHTBARRIERFEEDSTATION:
            self.controlledConveyorBelt.conveyorSensFeed = value
        if parameter == RequestedParameter.LIGHTBARRIERSWAPSTATION:
            self.controlledConveyorBelt.conveyorSensSwap = value
        if parameter == RequestedParameter.PULSECOUNTER:
            self.controlledConveyorBelt.conveyorSensImpulse = value
        else :
            logging.warning("Wrong parameter in fakeSensor function")