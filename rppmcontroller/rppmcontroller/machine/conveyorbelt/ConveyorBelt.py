import logging
from typing import Any, Callable, Dict, Optional

from typing_extensions import override

from rppmcontroller.behavior.CycleStepResult import CycleStepResult
from rppmcontroller.behavior.CycleStepResultEnum import CycleStepResultEnum
from rppmcontroller.machine.ConveyorState import ConveyorState
from rppmcontroller.machine.Direction import Direction
from rppmcontroller.machine.Machine import Machine
from rppmcontroller.machine.RequestedParameter import RequestedParameter
from rppmcontroller.machine.Runner import TransitioningMachine, Runner
from rppmcontroller.machine.conveyorbelt.ConveyorBeltConfig import \
    ConveyorBeltConfig
from rppmcontroller.utils.ImpulseCounter import ImpulseCounter
from rppmcontroller.utils.PlusMinusStop import PlusMinusStop


class ConveyorBelt(Machine, TransitioningMachine):



    @Machine.isExecuting.getter
    def isExecuting(self) -> bool:
        res = self.__conveyorActForward or self.__conveyorActBackward
        # log isexecuting and debug info only if message has changed
        isExecuting_log = f'isExecuting({self.id})={res} | Sensors={self.sensorStatusString()} | Actuators= {self.actuatorStatusString()}'
        if isExecuting_log != self.previous_isExecuting_log :
            logging.debug(isExecuting_log)
            self.previous_isExecuting_log = isExecuting_log
        return self.__conveyorActForward or self.__conveyorActBackward


    def __init__(self, id1):
        # inputs

        self.__conveyorSensImpulseCounterRaw = 0
        self.__conveyorSensFeed = True
        self.__conveyorSensSwap = True

        #outputs
        self.__conveyorActForward = False
        self.__conveyorActBackward = False


        dictMap = {RequestedParameter.LIGHTBARRIERFEEDSTATION: self.__conveyorSensSwap,
                   RequestedParameter.LIGHTBARRIERSWAPSTATION: self.__conveyorSensFeed,
                   RequestedParameter.MOTORCONVEYORBELTFORWARD: self.__conveyorActForward,
                   RequestedParameter.MOTORCONVEYORBELTBACKWARD: self.__conveyorActBackward}
        super().__init__(id1, dictMap)
        TransitioningMachine.__init__(self)

        #helper variables
        self.__counter = ImpulseCounter()
        self.current = 0
        self.arrived = False
        self.once = True
        self.isInitialized = True       # Conveyor doesn't require initialization process
        self.previous_isExecuting_log = None

    @property
    def conveyorSensFeed(self) -> bool:
        return self.__conveyorSensFeed

    @conveyorSensFeed.setter
    def conveyorSensFeed(self, value: bool):
        self.__conveyorSensFeed = value

    @property
    def conveyorSensSwap(self) -> bool:
        return self.__conveyorSensSwap

    @conveyorSensSwap.setter
    def conveyorSensSwap(self, value: bool):
        self.__conveyorSensSwap = value

    @property
    def conveyorSensImpulse(self):
        return self.__conveyorSensImpulseCounterRaw

    @conveyorSensImpulse.setter
    def conveyorSensImpulse(self, value):
        self.__conveyorSensImpulseCounterRaw = value

    @property
    def conveyorActForward(self) -> bool:
        return self.__conveyorActForward

    @conveyorActForward.setter
    def conveyorActForward(self, value: bool):
        self.__conveyorActForward = value

    @property
    def conveyorActBackward(self) -> bool:
        return self.__conveyorActBackward

    @conveyorActBackward.setter
    def conveyorActBackward(self, value: bool):
        self.__conveyorActBackward = value

    @property
    def conveyorCounterValue(self):
        return self.__counter.counter

    def sensorStatusString(self) -> str:
        return f"[{self.conveyorSensFeed}, {self.conveyorSensSwap}], {self.conveyorSensImpulse}"

    def actuatorStatusString(self) -> str:
        return f"[{self.conveyorActForward}, {self.conveyorActBackward}]"

    def inputStatus(self) -> Dict[str, Any]:
        status = {
            "conveyorSensFeed": self.conveyorSensFeed,
            "conveyorSensSwap": self.conveyorSensSwap,
            "conveyorSensImpulse": self.conveyorSensImpulse,
        }
        return status

    def outputStatus(self) -> Dict[str, Any]:

        status = {
            "conveyorActForward": self.conveyorActForward,
            "conveyorActBackward": self.conveyorActBackward,
        }
        return status

    def internalStatus(self) -> Dict[str, Any]:
        status = {
            "isExecuting": self.isExecuting,
        }
        return status


    def countSteps(self):
        """Count the number of steps when the conveyor is moving """
        self.current  = self.current + self.__counter.compute(self.__conveyorSensImpulseCounterRaw, PlusMinusStop.PLUS)
        # logging.debug(f"Step counter : {self.current }")
        return self.current

    @override
    def goto_config(self, config: ConveyorBeltConfig) -> CycleStepResult:
        self.conveyorActForward = config.state is ConveyorState.FORWARD
        self.conveyorActBackward = config.state is ConveyorState.BACKWARD
        return CycleStepResult(CycleStepResultEnum.DONE, "target config reached")

    ### ____________ Functions intended to be called in the exLoop function of the RevPiPyMachineController ________________

    def forwardFromAnywhere_CycleStep(self) -> CycleStepResult:
        """Move the package from any place on the conveyor to the right sensor
        :return: as a CycleStep, this function must return True when it is finished so it can be removed from the currentlyExecuting map"""
        self.__conveyorActForward = True
        if not self.__conveyorSensSwap:
            self.__conveyorActForward = False
            return CycleStepResult(CycleStepResultEnum.DONE)
        else:
            return CycleStepResult(CycleStepResultEnum.MUST_CONTINUE)


    def backwardFromAnywhere_CycleStep(self) -> CycleStepResult:
        """Move the package from any place on the conveyor to the left sensor
        :return: as a CycleStep, this function must return True when it is finished so it can be removed from the currentlyExecuting map"""
        self.__conveyorActBackward = True
        if not self.__conveyorSensFeed:
            self.__conveyorActBackward = False
            return CycleStepResult(CycleStepResultEnum.DONE)
        else:
            return CycleStepResult(CycleStepResultEnum.MUST_CONTINUE)


    def forwardLeaveConveyor_CycleStep(self) -> CycleStepResult:
        """Move the package from anywhere on the line to the left, until it leaves the conveyor. Then stop the conveyor.
        :return: as a CycleStep, this function must return True when it is finished so it can be removed from the currentlyExecuting map"""
        ret = False
        if not self.arrived:
            self.__conveyorActForward = True
        else:
            if self.countSteps() >= 6:
                self.__conveyorActForward = False
                ret = True # command final goal reached, no need to call this cycleStep again

        if not self.__conveyorSensSwap:
            self.current = 0
            self.arrived = True
        if ret:
            return CycleStepResult(CycleStepResultEnum.DONE)
        else:
            return CycleStepResult(CycleStepResultEnum.MUST_CONTINUE)


    def backwardLeaveConveyor_CycleStep(self) -> CycleStepResult:
        """Move the package from anywhere on the line to the right, until it leaves the conveyor. Then stop the conveyor.
        :return: as a CycleStep, this function must return True when it is finished so it can be removed from the currentlyExecuting map"""
        ret = False
        if not self.arrived:
            self.__conveyorActBackward = True
        else:
            if self.countSteps() >= 6:
                self.__conveyorActBackward = False
                ret = True # command final goal reached, no need to call this cycleStep again

        if not self.__conveyorSensFeed:
            self.current = 0
            self.arrived = True
        if ret:
            return CycleStepResult(CycleStepResultEnum.DONE)
        else:
            return CycleStepResult(CycleStepResultEnum.MUST_CONTINUE)


    def forwardGoto_CycleStep(self, steps: int) -> CycleStepResult:
        """Move the package to the right, with a given number of steps
        Args:
            steps (int) : the number of steps you want to move the package
        :return: as a CycleStep, this function must return True when it is finished so it can be removed from the currentlyExecuting map
        """
        self.__conveyorActForward = True
        self.current = self.countSteps()
        logging.debug(f"maxStep : {steps} counter : {self.current}")
        if self.current >= steps :
            self.__conveyorActForward = False
            self.current = 0
            return CycleStepResult(CycleStepResultEnum.DONE)
        else:
            return CycleStepResult(CycleStepResultEnum.MUST_CONTINUE)


    def backwardGoto_CycleStep(self, steps: int) -> CycleStepResult:
        """Move the package to the left, with a given number of steps
        Args:
            steps (int) : the number of steps you want to move the package
        :return: as a CycleStep, this function must return True when it is finished so it can be removed from the currentlyExecuting map
        """
        self.__conveyorActBackward = True
        self.current = self.countSteps()
        logging.debug(f"maxStep : {steps} counter : {self.current}")
        if self.current >= steps :
            self.__conveyorActBackward = False
            self.current = 0
            return CycleStepResult(CycleStepResultEnum.DONE)
        else:
            return CycleStepResult(CycleStepResultEnum.MUST_CONTINUE,
                                    f"backwardGoto_CycleStep",
                                    None)

    @override
    def stop_CycleStep(self) -> CycleStepResult:
        """Stop the conveyor"""
        self.__conveyorActForward = False
        self.__conveyorActBackward = False
        return CycleStepResult(CycleStepResultEnum.DONE)


    ### ____________ Functions callable from orchestrator ________________
    #   function name must be lowercase and finish with '_Command' postfix (cf. RevPiPyMachineController)

    def stop_Command(self) -> Optional[Callable[[], CycleStepResult]]:
        """Stop the conveyor"""
        return lambda: self.stop_CycleStep()

    def move_out_Command(self, direction: Direction) -> Runner:
        """Move the package to a given direction until it leaves the conveyor
            Args:
                direction (Direction) : the direction where to move the package

            The conveyor will stop after few steps when the package leaves the coveyor.
        """
        runner = self.create_runner()

        # move to sensor
        runner.then_run_runner_from(lambda: self.move_to_sensor(direction))
        # keep moving for a second or so
        runner.then_goto(ConveyorBeltConfig(state=ConveyorState.from_direction(direction)), and_stay_for=1.0)
        # stop the belt
        runner.then_goto(ConveyorBeltConfig())
        return runner.run()

    def move_nb_steps_Command(self, dir: Direction, steps: int) -> Optional[Callable[[], CycleStepResult]]:
        """Move the conveyor belt to a given direction with a given number of steps
            Args:
                dir (Direction) : the direction where to move the package
                steps (int) : the number of steps you want to move the package

            There is no control of the position of the package. The conveyor wont stop until it reach the number of steps
        """
        self.sensed = False
        if dir == Direction.FORWARD:
            return lambda: self.forwardGoto_CycleStep(steps)
        if dir == Direction.BACKWARD:
            return lambda: self.backwardGoto_CycleStep(steps)
        else:
            logging.error(f"Invalid direction {dir}")


    def move_to_sensor_Command(self, direction: Direction) -> Optional[Callable[[], CycleStepResult]]:
        """Move the package to a given direction until it is detected by the destination sensor
            Args:
                direction (Direction) : the direction where to move the package
        """
        if dir == Direction.FORWARD:
            return lambda: self.forwardFromAnywhere_CycleStep()
        if dir == Direction.BACKWARD:
            return lambda: self.backwardFromAnywhere_CycleStep()

    def move_to_sensor(self, direction: Direction):
        """Move the package to a given direction until it is detected by the destination sensor
            Args:
                direction (Direction) : the direction where to move the package
        """
        runner = self.create_runner()
        config = ConveyorBeltConfig()

        state = ConveyorState.from_direction(direction)
        if state is ConveyorState.FORWARD:
            sensor_reached = lambda: not self.conveyorSensSwap
        elif state is ConveyorState.BACKWARD:
            sensor_reached = lambda: not self.conveyorSensFeed
        else:
            raise ValueError(f"cannot move into that direction: {direction}")
        config.state = state
        runner.then_goto(config, until=sensor_reached)

        #stop the belt
        config.state = ConveyorState.IDLE
        runner.then_goto(config)

        return runner.run()
