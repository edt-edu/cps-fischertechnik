import logging
from typing import Dict, Any, Callable
from typing_extensions import override

from rppmcontroller.behavior.CycleStepResult import CycleStepResult
from rppmcontroller.behavior.CycleStepResultEnum import CycleStepResultEnum
from rppmcontroller.behavior.decoratorFunctions import cycle_step_function
from rppmcontroller.machine.ConveyorState import ConveyorState
from rppmcontroller.machine.Machine import Machine
from rppmcontroller.machine.RequestedParameter import RequestedParameter
from rppmcontroller.machine.Runner import TransitioningMachine, Runner
from rppmcontroller.machine.punchingmachine.PunchingMachineConfig import PunchingMachineConfig
from rppmcontroller.protocol.decoratorFunctions import protocol_command_function


class PunchingMachine(Machine, TransitioningMachine[PunchingMachineConfig]):
    def __init__(self, id1):
        # inputs
        self.__punchingMachineSensGoods = True
        self.__punchingMachineSensMachine = True
        self.__punchingMachineSensUp = False
        self.__punchingMachineSensDown = False

        # outputs
        self.__punchingMachineActConveyorForward = False
        self.__punchingMachineActConveyorBackward = False
        self.__punchingMachineActUp = False
        self.__punchingMachineActDown = False

        dictMap = {
            RequestedParameter.LIGHTBARRIERGOODSINOUT:
                self.__punchingMachineSensGoods,
            RequestedParameter.LIGHTBARRIERPUNCHINGMACHINE:
                self.__punchingMachineSensMachine,
            RequestedParameter.SWITCHPUNCHINGMACHINEUP:
                self.__punchingMachineSensUp,
            RequestedParameter.SWITCHPUNCHINGMACHINEDOWN:
                self.__punchingMachineSensDown,
            RequestedParameter.MOTORCONVEYORBELTFORWARD:
                self.__punchingMachineActConveyorForward,
            RequestedParameter.MOTORCONVEYORBELTBACKWARD:
                self.__punchingMachineActConveyorBackward,
            RequestedParameter.MOTORPUNCHINGMACHINEUP:
                self.__punchingMachineActUp,
            RequestedParameter.MOTORPUNCHINGMACHINEDOWN:
                self.__punchingMachineActDown
            }
        super().__init__(id1, dictMap)
        TransitioningMachine.__init__(self)

        # helper variables
        self.previous_is_executing_log = None

    @property
    def isExecuting(self) -> bool:
        res = (
            self.__punchingMachineActUp or self.__punchingMachineActDown or
            self.__punchingMachineActConveyorForward or
            self.__punchingMachineActConveyorBackward)
        # log isexecuting and debug info only if message has changed
        is_executing_log = (f'isExecuting({self.id})={res} | Sensors='
                           f'{self.sensorStatusString()} | Actuators= '
                           f'{self.actuatorStatusString()}')
        if is_executing_log != self.previous_is_executing_log:
            logging.debug(is_executing_log)
            self.previous_is_executing_log = is_executing_log
        return res

    @property
    def isInitialized(self) -> bool:
        return True  # always ready, since there are no encoder actuators

    @isInitialized.setter
    def isInitialized(self, value):
        logging.warning(f"Attempted to set read-only property 'isInitialized' on {self}")
        raise AttributeError("isInitialized is a read-only property")

    # Input properties

    @property
    def punchingMachineSensGoods(self):
        return self.__punchingMachineSensGoods

    @punchingMachineSensGoods.setter
    def punchingMachineSensGoods(self, value):
        self.__punchingMachineSensGoods = value

    @property
    def punchingMachineSensMachine(self):
        return self.__punchingMachineSensMachine

    @punchingMachineSensMachine.setter
    def punchingMachineSensMachine(self, value):
        self.__punchingMachineSensMachine = value

    @property
    def punchingMachineSensUp(self):
        return self.__punchingMachineSensUp

    @punchingMachineSensUp.setter
    def punchingMachineSensUp(self, value):
        self.__punchingMachineSensUp = value

    @property
    def punchingMachineSensDown(self):
        return self.__punchingMachineSensDown

    @punchingMachineSensDown.setter
    def punchingMachineSensDown(self, value):
        self.__punchingMachineSensDown = value

    # Output properties

    @property
    def punchingMachineActConveyorForward(self):
        return self.__punchingMachineActConveyorForward

    @punchingMachineActConveyorForward.setter
    def punchingMachineActConveyorForward(self, value):
        self.__punchingMachineActConveyorForward = value

    @property
    def punchingMachineActConveyorBackward(self):
        return self.__punchingMachineActConveyorBackward

    @punchingMachineActConveyorBackward.setter
    def punchingMachineActConveyorBackward(self, value):
        self.__punchingMachineActConveyorBackward = value

    @property
    def punchingMachineActUp(self):
        return self.__punchingMachineActUp

    @punchingMachineActUp.setter
    def punchingMachineActUp(self, value):
        self.__punchingMachineActUp = value

    @property
    def punchingMachineActDown(self):
        return self.__punchingMachineActDown

    @punchingMachineActDown.setter
    def punchingMachineActDown(self, value):
        self.__punchingMachineActDown = value

    def sensorStatusString(self) -> str:
        return (f"[{self.punchingMachineSensGoods}, "
                f"{self.punchingMachineSensMachine}, "
                f"{self.punchingMachineSensUp}, "
                f"{self.punchingMachineSensDown}]")

    def actuatorStatusString(self) -> str:
        return (f"[{self.punchingMachineActConveyorForward}, "
                f"{self.punchingMachineActConveyorBackward}, "
                f"{self.punchingMachineActUp}, "
                f"{self.punchingMachineActDown}]")

    def inputStatus(self) -> Dict[str, Any]:
        return {
            "punchingMachineSensGoods": self.punchingMachineSensGoods,
            "punchingMachineSensMachine": self.punchingMachineSensMachine,
            "punchingMachineSensUp": self.punchingMachineSensUp,
            "punchingMachineSensDown": self.punchingMachineSensDown,
            }

    def outputStatus(self) -> Dict[str, Any]:
        return {
            "punchingMachineActConveyorForward":
                self.punchingMachineActConveyorForward,
            "punchingMachineActConveyorBackward":
                self.punchingMachineActConveyorBackward,
            "punchingMachineActUp": self.punchingMachineActUp,
            "punchingMachineActDown": self.punchingMachineActDown,
            }

    def internalStatus(self) -> Dict[str, Any]:
        return {
            "isExecuting": self.isExecuting,
            }

    @override
    @cycle_step_function()
    def goto_config_CycleStep(self,
                    config: PunchingMachineConfig = PunchingMachineConfig())\
        -> CycleStepResult:
        res = CycleStepResult.done()

        # conveyor belt
        self.punchingMachineActConveyorForward = (config.conveyor_state is
                                                  ConveyorState.FORWARD)
        self.punchingMachineActConveyorBackward = (config.conveyor_state is
                                                   ConveyorState.BACKWARD)

        # punching machine
        self.punchingMachineActDown = False
        self.punchingMachineActUp = False
        if config.punching and not self.punchingMachineSensDown:
            self.punchingMachineActDown = True
            res = CycleStepResult(CycleStepResultEnum.MUST_CONTINUE,
                                  "moving down punching arm")
        elif not config.punching and not self.punchingMachineSensUp:
            self.punchingMachineActUp = True
            res = CycleStepResult(CycleStepResultEnum.MUST_CONTINUE,
                                  "moving up punching arm")

        return res

    @cycle_step_function()
    def stop_CycleStep(self) -> CycleStepResult:
        self.punchingMachineActUp = False
        self.punchingMachineActDown = False
        return CycleStepResult.done()

    # methods intended for orchestrator


    @protocol_command_function()
    def punch_Command(self) -> Runner:
        runner = self.create_runner()
        config = PunchingMachineConfig()

        # transport object to machine
        config.conveyor_state = ConveyorState.FORWARD
        runner.then_goto(config, until=lambda: not self.punchingMachineSensMachine, info="transporting goods to machine")

        # punch it
        config.conveyor_state = ConveyorState.IDLE
        config.punching = True
        runner.then_goto(config, info="punching goods")

        # move goods back to start of conveyor
        config.punching = False
        config.conveyor_state = ConveyorState.BACKWARD
        runner.then_goto(config, until=lambda: not self.punchingMachineSensGoods, info="moving goods back out")

        # stop conveyor
        config.conveyor_state = ConveyorState.IDLE
        runner.then_goto(config, info="stopping")

        return runner

    @protocol_command_function(description="Command stopping all engines (incl. compressor).")
    def stop_Command(self) -> Callable[[], CycleStepResult]:
        return self.stop_CycleStep
