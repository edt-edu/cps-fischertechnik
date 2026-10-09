import logging

from rppmcontroller.machine.MachineSimpleSimulator import \
    MachineSimpleSimulator
from rppmcontroller.machine.vacuumgripper.VacuumGripper import VacuumGripper


class VacuumGripperSimpleSimulator(MachineSimpleSimulator):
    """A Simple simulator connected to a VacuumGripper Machine

    Engines are supposed to be configured as using Encoder (ie. increment and decrement depending on the engine direction)
    """

    def __init__(self, controlledVacuumGripper: VacuumGripper,
                 initialVerticalDistToSensor: int = 250,
                 initialHorizontalDistToSensor: int = 250,
                 initialRotationDistToSensor: int = 250,
                 encoderIncrement: int = 50):
        """Initialize a simulator connected to a VacuumGripper Machine

        Parameters:
            controlledVacuumGripper (VacuumGripper): VacuumGripper that is connected to this simulator
            initialVerticalDistToSensor (int): distance to the vertical sensor when starting the simulation (in number of encoder value)
            initialHorizontalDistToSensor (int): same for horizontal sensor
            initialRotationDistToSensor (int): same for rotation sensor
            encoderIncrement (int): value that will be added/removed for each cycle loop on active engines (ie. on call to simulatedRead)

        An axis that reverses while moving (it overshot its goal) halves its increment, like a motor settling on a
        position, so it always ends within the axis tolerance even when encoderIncrement is larger than it.
        """
        self.__controlledVacuumGripper = controlledVacuumGripper
        self.initialVerticalDistToSensor = initialVerticalDistToSensor
        self.initialHorizontalDistToSensor = initialHorizontalDistToSensor
        self.initialRotationDistToSensor = initialRotationDistToSensor
        self.encoderIncrement = encoderIncrement

        self.hasBeenCalibratedOnArm: bool = False
        """Indicates whether a reset of the arm has happened at least once, i.e. via simulatedArmReset()"""
        self.hasBeenCalibratedRotational: bool = False
        """Indicates whether a rotational reset has happened at least once, i.e. via simulatedRotationReset()"""
        self.hasBeenCalibratedVertically: bool = False
        """Indicates whether a vertical reset has happened at least once, i.e. via simulatedVerticalReset()"""

        # All encoder values start at zero, even if that doesn't represent their "physical" location
        self.vertical_encoder_value = 0
        """The value of the vertical encoder counter, as seen by the RevPi"""
        self.horizontal_encoder_value = 0
        """The value of the horizontal encoder counter, as seen by the RevPi"""
        self.rotational_encoder_value = 0
        """The value of the rotational encoder counter, as seen by the RevPi"""

        self.vertical_offset = 0
        """
        Artificial error between the zero position and the ref-switch of the
        vertical axis.
        Is reset, when the axis is reset.
        """
        self.horizontal_offset = 0
        """
        Artificial error between the zero position and the ref-switch of the
        horizontal axis.
        Is reset, when the axis is reset.
        """
        self.rotational_offset = 0
        """
        Artificial error between the zero position and the ref-switch of the
        rotational axis.
        Is reset, when the axis is reset.
        """

        self.__next_horizontal_encoder_increment = 0
        """The increment of the horizontal encoder counter in the next read"""
        self.__next_vertical_encoder_increment = 0
        """The increment of the vertical encoder counter in the next read"""
        self.__next_rotational_encoder_increment = 0
        """The increment of the rotational encoder counter in the next read"""

        self.__axis_direction = {"vertical": 0, "horizontal": 0, "rotational": 0}
        """Direction of each axis in the previous cycle: 1, -1, or 0 when stopped"""
        self.__axis_increment = {axis: encoderIncrement for axis in self.__axis_direction}
        """Current increment of each axis, halved on each reversal while moving"""

        self.previous_simulatedReadLog = None
        self.previous_simulatedWriteLog = None

    @property
    def hasBeenCalibrated(self) -> bool:
        """indicate if the setup() has been called at least once. ie. via reset()"""
        return self.hasBeenCalibratedOnArm and self.hasBeenCalibratedRotational and self.hasBeenCalibratedVertically

    @property
    def controlledVacuumGripper(self):
        return self.__controlledVacuumGripper

    def simulatedRead(self) -> None:
        # update internal values
        self.vertical_encoder_value += self.__next_vertical_encoder_increment
        self.__next_vertical_encoder_increment = 0
        self.horizontal_encoder_value += self.__next_horizontal_encoder_increment
        self.__next_horizontal_encoder_increment = 0
        self.rotational_encoder_value += self.__next_rotational_encoder_increment
        self.__next_rotational_encoder_increment = 0

        # update vgr encoders
        self.controlledVacuumGripper.vacuumSensVerticalEncoderCounter = self.vertical_encoder_value
        self.controlledVacuumGripper.vacuumSensArmEncoderCounter = self.horizontal_encoder_value
        self.controlledVacuumGripper.vacuumSensRotEncoderCounter = self.rotational_encoder_value

        # simulate sensors
        self.controlledVacuumGripper.vacuumSensVerticalEndUp = self.vertical_encoder_value + self.vertical_offset <= (
            0 if self.hasBeenCalibratedVertically else -self.initialVerticalDistToSensor)
        self.controlledVacuumGripper.vacuumSensArmEndIn = self.horizontal_encoder_value + self.horizontal_offset <= (
            0 if self.hasBeenCalibratedOnArm else -self.initialHorizontalDistToSensor)
        self.controlledVacuumGripper.vacuumSensRotEnd = self.rotational_encoder_value + self.rotational_offset <= (
            0 if self.hasBeenCalibratedRotational else -self.initialRotationDistToSensor)

        # no need to simulate compressor and valve

        # log new values
        simulatedReadLog = f"simulatedRead  {self.controlledVacuumGripper.sensorStatusString()} "
        if simulatedReadLog != self.previous_simulatedReadLog:
            logging.debug(simulatedReadLog)
            self.previous_simulatedReadLog = simulatedReadLog

    def __axis_step(self, axis: str, plus: bool, minus: bool) -> int:
        """Encoder increment of an axis for this cycle, given its plus / minus motor actuators"""
        direction = int(plus) - int(minus)
        if direction == 0:
            # stopped: the next move starts at full speed, whatever its direction
            self.__axis_increment[axis] = self.encoderIncrement
        elif direction == -self.__axis_direction[axis]:
            # reversed without stopping: it overshot its goal, settle with smaller steps
            self.__axis_increment[axis] = max(1, self.__axis_increment[axis] // 2)
        self.__axis_direction[axis] = direction
        return direction * self.__axis_increment[axis]

    def simulatedWrite(self) -> None:
        vgr = self.controlledVacuumGripper
        self.__next_horizontal_encoder_increment += self.__axis_step("horizontal", vgr.vacuumActArmOut, vgr.vacuumActArmIn)
        self.__next_vertical_encoder_increment += self.__axis_step("vertical", vgr.vacuumActVerticalDown, vgr.vacuumActVerticalUp)
        self.__next_rotational_encoder_increment += self.__axis_step("rotational", vgr.vacuumActRotLeft, vgr.vacuumActRotRight)

        # compressor and valve are not simulated

        simulatedWriteLog = f"simulatedWrite  {self.controlledVacuumGripper.sensorStatusString()} "
        if simulatedWriteLog != self.previous_simulatedWriteLog:
            logging.debug(simulatedWriteLog)
            self.previous_simulatedWriteLog = simulatedWriteLog

    def simulatedReset(self) -> None:
        self.simulatedArmReset()
        self.simulatedRotationReset()
        self.simulatedVerticalReset()

    def simulatedArmReset(self) -> None:
        self.hasBeenCalibratedOnArm = True
        # 0 means the arm is retracted, higher values means it's going outward
        self.horizontal_encoder_value = 0
        self.horizontal_offset = 0

    def simulatedRotationReset(self) -> None:
        self.hasBeenCalibratedRotational = True
        # 0 means the arm is at maximum clockwise position, higher values means it's going counter-clockwise from this position
        self.rotational_encoder_value = 0
        self.rotational_offset = 0

    def simulatedVerticalReset(self) -> None:
        self.hasBeenCalibratedVertically = True
        # 0 means the axis is in the uppermost position, higher values means it s going down
        self.vertical_encoder_value = 0
        self.vertical_offset = 0
