import logging
from typing import Any, Dict

from rppmcontroller.machine.ConveyorState import ConveyorState
from rppmcontroller.machine.Direction import Direction
from rppmcontroller.machine.Machine import Machine
from rppmcontroller.machine.RequestedParameter import RequestedParameter
from rppmcontroller.machine.Runner import TransitioningMachine
from rppmcontroller.machine.conveyorbelt.ConveyorBeltConfig import \
    ConveyorBeltConfig
from rppmcontroller.utils.ImpulseCounter import ImpulseCounter
from rppmcontroller.utils.PlusMinusStop import PlusMinusStop


class ConveyorBelt(Machine, TransitioningMachine):


    def __isExecuting(self) -> bool:
        return self.__conveyorActForward or self.__conveyorActBackward

    @property
    def isExecuting(self) -> bool:
        res = self.__isExecuting()
        #logging.debug(f"Is executing ! {self.__conveyorActForward or self.__conveyorActBackward}")

        # log isexecuting and debug info only if message has changed
        isExecuting_log = f'isExecuting({self.id})={res} | Sensors={self.sensorStatusString()} | Actuators= {self.actuatorStatusString()}'
        if isExecuting_log != self.previous_isExecuting_log :
            logging.debug(isExecuting_log)
            self.previous_isExecuting_log = isExecuting_log

        return res


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
        self.arrived = False
        self.once = True
        self.__isExecutingCount = 0
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
            "isExecuting": self.__isExecuting(),
        }
        return status


    def forwardFromAnywhere(self):
        """Move the package from any place on the conveyor to the right sensor"""
        self.__conveyorActForward = True
        if not self.__conveyorSensSwap:
            self.__conveyorActForward = False
            return True
        return False

    def backwardFromAnywhere(self):
        """Move the package from any place on the conveyor to the left sensor"""
        self.__conveyorActBackward = True
        if not self.__conveyorSensFeed:
            self.__conveyorActBackward = False
            return True
        return False

    def forwardLeaveConveyor(self):
        """Move the package from anywhere on the line to the left, until it leaves the conveyor. Then stop the conveyor."""
        if not self.arrived:
            self.__conveyorActForward = True
        else:
            if self.countSteps() >= 6:
                self.__conveyorActForward = False
        if not self.__conveyorSensSwap:
            self.__counter.counter = 0
            self.arrived = True

    def backwardLeaveConveyor(self):
        """Move the package from anywhere on the line to the right, until it leaves the conveyor. Then stop the conveyor."""
        if not self.arrived:
            self.__conveyorActBackward = True
        else:
            if self.countSteps() >= 6:
                self.__conveyorActBackward = False
        if not self.__conveyorSensFeed:
            self.__counter.counter = 0
            self.arrived = True

    def forwardGoto(self, steps: int):
        """Move the package to the right, with a given number of steps
        Args:
            steps (int) : the number of steps you want to move the package
        """
        self.__conveyorActForward = True
        self.current = self.countSteps()
        logging.debug(f"maxStep : {steps} counter : {self.current}")
        if self.current >= steps :
            self.__conveyorActForward = False
            self.current = 0

    def backwardGoto(self, steps: int):
        """Move the package to the left, with a given number of steps
            Args:
                steps (int) : the number of steps you want to move the package
        """
        self.__conveyorActBackward = True
        self.current = self.countSteps()
        logging.debug(f"maxStep : {steps} counter : {self.current}")
        if self.current >= steps :
            self.__conveyorActBackward = False
            self.current = 0

    def countSteps(self):
        """Count the number of steps when the conveyor is moving """
        self.current  = self.current + self.__counter.compute(self.__conveyorSensImpulseCounterRaw, PlusMinusStop.PLUS)
        # logging.debug(f"Step counter : {self.current }")
        return self.current



    ### ____________ Functions callable from orchestrator ________________

    def goto_config(self, config: ConveyorBeltConfig) -> bool:
        self.conveyorActForward = config.state is ConveyorState.FORWARD
        self.conveyorActBackward = config.state is ConveyorState.BACKWARD
        return True

    #   function name must be lowercase (cf. RevPiPyMachineController)
    def stop(self):
        """Stop the conveyor"""
        self.__conveyorActForward = False
        self.__conveyorActBackward = False


    def move_out(self, direction: Direction):
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

    def move_nb_steps(self, dir: Direction, steps: int):
        """Move the conveyor belt to a given direction with a given number of steps
            Args:
                dir (Direction) : the direction where to move the package
                steps (int) : the number of steps you want to move the package

            There is no control of the position of the package. The conveyor wont stop until it reach the number of steps
        """
        self.sensed = False
        self.__counter.counter = 0
        if dir == Direction.FORWARD:
            return lambda: self.forwardGoto(steps)
        if dir == Direction.BACKWARD:
            return lambda: self.backwardGoto(steps)
        else:
            logging.error(f"Invalid direction {dir}")
            raise ValueError("Invalid direction")

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

