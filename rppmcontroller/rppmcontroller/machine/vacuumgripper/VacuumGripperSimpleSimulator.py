import logging
from typing import Tuple
import traceback
from rppmcontroller.machine.vacuumgripper.VacuumGripper import VacuumGripper
from rppmcontroller.machine.MachineSimpleSimulator import MachineSimpleSimulator


class VacuumGripperSimpleSimulator(MachineSimpleSimulator):
    """A Simple simulator connected to a VacuumGripper Machine

    Engines are supposed to be configured as using Encoder (ie. increment and decrement depending on the engine direction)
    """


    def __init__(self, controlledVacuumGripper : VacuumGripper, 
                 initialVerticalDistToSensor : int = 250 , initialHorizontalDistToSensor : int = 250, initialRotationDistToSensor : int = 250,
                 encoderIncrement : int = 50):
        """Initialize a simulator connected to a VacuumGripper Machine

        Parameters:
            controlledVacuumGripper (VacuumGripper): VacuumGripper that is connected to this simulator
            initialVerticalDistToSensor (int): distance to the vertical sensor when starting the simulation (in number of encoder value)
            initialHorizontalDistToSensor (int): same for horizontal sensor
            initialRotationDistToSensor (int): same for rotation sensor
            encoderIncrement (int): value that will be added/removed for each cycle loop on active engines (ie. on call to simulatedRead)
        """
        self.__controlledVacuumGripper = controlledVacuumGripper
        self.initialVerticalDistToSensor = initialVerticalDistToSensor
        self.initialHorizontalDistToSensor = initialHorizontalDistToSensor
        self.initialRotationDistToSensor = initialRotationDistToSensor
        self.encoderIncrement = encoderIncrement
 
        self.hasBeenCalibrated : bool = False 
        """indicate if the setup() has been called at least once. ie. via reset()"""

        self.previous_simulatedReadLog = None
        self.previous_simulatedWriteLog = None


    @property
    def controlledVacuumGripper(self):
        return self.__controlledVacuumGripper
    
    def simulatedRead(self) -> None:        
        simulatedReadLog = f"simulatedRead  {self.controlledVacuumGripper.sensorStatusString()} "
        if simulatedReadLog != self.previous_simulatedReadLog :
            logging.debug(simulatedReadLog)
            self.previous_simulatedReadLog = simulatedReadLog   

    def simulatedWrite(self) -> None:
        if not (self.controlledVacuumGripper.vacuumActArmIn and self.controlledVacuumGripper.vacuumActArmOut):
            # if both vacuumActArmIn and vacuumActArmOut are True -> they cancel each other (no move) 
            if self.controlledVacuumGripper.vacuumActArmOut:
                self.controlledVacuumGripper.vacuumSensArmEncoderCounter += self.encoderIncrement
            if self.controlledVacuumGripper.vacuumActArmIn:
                self.controlledVacuumGripper.vacuumSensArmEncoderCounter -= self.encoderIncrement

        if not (self.controlledVacuumGripper.vacuumActVerticalUp and self.controlledVacuumGripper.vacuumActVerticalDown):
            # if both vacuumActVerticalUp and vacuumActVerticalDown are True -> they cancel each other (no move) 
            if self.controlledVacuumGripper.vacuumActVerticalDown:
                self.controlledVacuumGripper.vacuumSensVerticalEncoderCounter += self.encoderIncrement
            if self.controlledVacuumGripper.vacuumActVerticalUp:
                self.controlledVacuumGripper.vacuumSensVerticalEncoderCounter -= self.encoderIncrement

        if not (self.controlledVacuumGripper.vacuumActRotRight and self.controlledVacuumGripper.vacuumActRotLeft):
            # if both vacuumActRotRight and vacuumActRotLeft are True -> they cancel each other (no move) 
            if self.controlledVacuumGripper.vacuumActRotLeft:
                self.controlledVacuumGripper.vacuumSensRotEncoderCounter += self.encoderIncrement
            if self.controlledVacuumGripper.vacuumActRotRight:
                self.controlledVacuumGripper.vacuumSensRotEncoderCounter -= self.encoderIncrement

        # simulate sensors
        if self.hasBeenCalibrated:
            self.controlledVacuumGripper.vacuumSensArmEndIn = self.controlledVacuumGripper.vacuumSensArmEncoderCounter <= 0
            self.controlledVacuumGripper.vacuumSensVerticalEndUp = self.controlledVacuumGripper.vacuumSensVerticalEncoderCounter <= 0
            self.controlledVacuumGripper.vacuumSensRotEnd= self.controlledVacuumGripper.vacuumSensRotEncoderCounter <= 0
        else:
            self.controlledVacuumGripper.vacuumSensArmEndIn = self.controlledVacuumGripper.vacuumSensArmEncoderCounter <= -self.initialHorizontalDistToSensor
            self.controlledVacuumGripper.vacuumSensVerticalEndUp = self.controlledVacuumGripper.vacuumSensVerticalEncoderCounter <= -self.initialVerticalDistToSensor
            self.controlledVacuumGripper.vacuumSensRotEnd= self.controlledVacuumGripper.vacuumSensRotEncoderCounter <= -self.initialRotationDistToSensor

        # nothing special to do to simulate compressor and valve as there are no observable IO for them
        
        simulatedWriteLog = f"simulatedWrite  {self.controlledVacuumGripper.sensorStatusString()} "
        if simulatedWriteLog != self.previous_simulatedWriteLog :
            logging.debug(simulatedWriteLog)
            self.previous_simulatedWriteLog = simulatedWriteLog 

    def simulatedReset(self) -> None:
        self.hasBeenCalibrated = True

        # 0 means the arm is retracted, higher values means it's going outward
        self.controlledVacuumGripper.vacuumSensArmEncoderCounter = 0 
        
        # 0 means the arm is at maximum clockwise position, higher values means it's going counter-clockwise from this position
        self.controlledVacuumGripper.vacuumSensRotEncoderCounter = 0
        
        # 0 means the axis is in the uppermost position, higher values means it s going down
        self.controlledVacuumGripper.vacuumSensVerticalEncoderCounter = 0 