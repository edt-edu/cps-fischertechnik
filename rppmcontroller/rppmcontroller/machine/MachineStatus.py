from enum import Enum


class MachineStatus(Enum):
    UNINITIALIZED_IDLE = 0      # machine requires initialization
    UNINITIALIZED_ACTIVE = 1    # some command and or actuator is active, probably performing a setup/initialization
    INITIALIZED_IDLE = 2        # machine is ready and not performing any command and no actuator is active
    INITIALIZED_ACTIVE = 3      # machine is initialized, a command and/or actuator are active


