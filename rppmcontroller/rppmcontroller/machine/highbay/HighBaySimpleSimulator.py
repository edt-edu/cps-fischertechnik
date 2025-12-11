import logging
from rppmcontroller.machine.highbay.HighBay import HighBay
from rppmcontroller.machine.RequestedParameter import RequestedParameter
from rppmcontroller.machine.MachineSimpleSimulator import MachineSimpleSimulator


class HighBaySimpleSimulator(MachineSimpleSimulator):
    """A Simple simulator connected to a High Bay Warehouse  Machine """

    def __init__(self, controlledMachine : HighBay,
                 initialVerticalDistToSensor : int = 250 , initialHorizontalDistToSensor : int = 250,
                 encoderIncrement : int = 20):
        """Initialize a simulator connected to a High Bay Warehouse Machine

        Parameters:
            controlledMachine (HighBay): High Bay Warehouse that is connected to this simulator
            initialVerticalDistToSensor (int): distance to the vertical sensor when starting the simulation (in number of encoder value)
            initialHorizontalDistToSensor (int): same for horizontal sensor
            encoderIncrement (int): value that will be added/removed for each cycle loop on active engines (ie. on call to simulatedRead)
        """
        self.__controlledMachine = controlledMachine
        self.initialVerticalDistToSensor = initialVerticalDistToSensor
        self.initialHorizontalDistToSensor = initialHorizontalDistToSensor
        self.encoderIncrement = encoderIncrement

        self.hasBeenCalibratedHorizontally: bool = False
        """Indicates whether a horizontal reset has happened at least once, i.e. via simulatedHorizontalReset()"""
        self.hasBeenCalibratedVertically: bool = False
        """Indicates whether a vertical reset has happened at least once, i.e. via simulatedVerticalReset()"""

        #  inputs (light barriers are True when there is no object in front of the sensor, and button are False when not pressed)
        self.controlledMachine.highbaySensHorizontal = False
        self.controlledMachine.highbaySensInside = True
        self.controlledMachine.highbaySensOutside = True
        self.controlledMachine.highbaySensVertical = False
        self.controlledMachine.highbaySensCantileverFront = False
        self.controlledMachine.highbaySensCantileverBack = False

        #  outputs
        self.controlledMachine.highbayActConveyorForward = False
        self.controlledMachine.highbayActConveyorBackward = False
        self.controlledMachine.highbayActHorizontalToRack = False
        self.controlledMachine.highbayActHorizontalToConveyor = False
        self.controlledMachine.highbayActDown = False
        self.controlledMachine.highbayActUp = False
        self.controlledMachine.highbayActCantileverForward = False
        self.controlledMachine.highbayActCantileverBackward = False

        #  encoder
        self.controlledMachine.highbaySensHorizontalEncoderCounter = 0
        self.controlledMachine.highbaySensVerticalEncoderCounter = 0

        self.previous_simulatedReadLog = None
        self.previous_simulatedWriteLog = None

    @property
    def hasBeenCalibrated(self) -> bool:
        """indicate if the setup() has been called at least once. ie. via reset()"""
        return self.hasBeenCalibratedHorizontally and self.hasBeenCalibratedVertically

    @property
    def controlledMachine(self):
        return self.__controlledMachine

    def simulatedRead(self) -> None:
        simulatedReadLog = f"simulatedRead  {self.controlledMachine.sensorStatusString()} "
        if simulatedReadLog != self.previous_simulatedReadLog :
            logging.debug(simulatedReadLog)
            self.previous_simulatedReadLog = simulatedReadLog

    def simulatedWrite(self) -> None:

        # if moving increase counters
        if not (self.controlledMachine.highbayActHorizontalToRack and self.controlledMachine.highbayActHorizontalToConveyor):
            # if both highbayActHorizontalToRack and highbayActHorizontalToConveyor are True -> they cancel each other (no move)
            if self.controlledMachine.highbayActHorizontalToConveyor:
                self.controlledMachine.highbaySensHorizontalEncoderCounter += self.encoderIncrement
            if self.controlledMachine.highbayActHorizontalToRack:
                self.controlledMachine.highbaySensHorizontalEncoderCounter -= self.encoderIncrement

        if not (self.controlledMachine.highbayActDown and self.controlledMachine.highbayActUp):
            # if both highbayActDown and highbayActUp are True -> they cancel each other (no move)
            if self.controlledMachine.highbayActUp:
                self.controlledMachine.highbaySensVerticalEncoderCounter += self.encoderIncrement
            if self.controlledMachine.highbayActDown:
                self.controlledMachine.highbaySensVerticalEncoderCounter -= self.encoderIncrement

        # simulate sensors
        if self.hasBeenCalibratedHorizontally:
            self.controlledMachine.highbaySensHorizontal = self.controlledMachine.highbaySensHorizontalEncoderCounter <= 0
        else:
            self.controlledMachine.highbaySensHorizontal = self.controlledMachine.highbaySensHorizontalEncoderCounter <= -self.initialHorizontalDistToSensor

        if self.hasBeenCalibratedVertically:
            self.controlledMachine.highbaySensVertical = self.controlledMachine.highbaySensVerticalEncoderCounter <= 0
        else:
            self.controlledMachine.highbaySensVertical = self.controlledMachine.highbaySensVerticalEncoderCounter <= -self.initialVerticalDistToSensor

        # Should we try to simulate highbayActCantileverForward and highbayActCantileverBackward action on highbaySensCantileverFront and highbaySensCantileverBack


        simulatedWriteLog = f"simulatedWrite  {self.controlledMachine.sensorStatusString()} "
        if simulatedWriteLog != self.previous_simulatedWriteLog :
            logging.debug(simulatedWriteLog)
            self.previous_simulatedWriteLog = simulatedWriteLog

    def simulatedReset(self) -> None:
        self.simulatedHorizontalReset()
        self.simulatedVerticalReset()

    def simulatedHorizontalReset(self) -> None:
        self.hasBeenCalibratedHorizontally = True
        # 0 means the arm is retracted, higher values means it's going outward
        self.controlledMachine.highbaySensHorizontalEncoderCounter = 0

    def simulatedVerticalReset(self) -> None:
        self.hasBeenCalibratedVertically = True
        # 0 means the axis is in the uppermost position, higher values means it s going down
        self.controlledMachine.highbaySensVerticalEncoderCounter = 0

    def fakeSensor(self, parameter : RequestedParameter, value: bool):
        if parameter == RequestedParameter.REFERENCESWITCHHORIZONTALAXIS:
            self.controlledMachine.highbaySensHorizontal = value
        elif parameter == RequestedParameter.LIGHTBARRIERINSIDE:
            self.controlledMachine.highbaySensInside = value
        elif parameter == RequestedParameter.LIGHTBARRIEROUTSIDE:
            self.controlledMachine.highbaySensOutside = value
        elif parameter == RequestedParameter.REFERENCESWITCHVERTICALAXIS:
            self.controlledMachine.highbaySensVertical = value
        elif parameter == RequestedParameter.HORIZONTALAXISSTEP:
            self.controlledMachine.highbaySensHorizontalEncoderCounter = value
        elif parameter == RequestedParameter.VERTICALAXISSTEP:
            self.controlledMachine.highbaySensVerticalEncoderCounter = value
        elif parameter == RequestedParameter.REFERENCESWITCHCANTILEVERFRONT:
            self.controlledMachine.highbaySensCantileverFront = value
        elif parameter == RequestedParameter.REFERENCESWITCHCANTILEVERBACK:
            self.controlledMachine.highbaySensCantileverBack = value
        else :
            logging.warning("Wrong parameter in fakeSensor function")

