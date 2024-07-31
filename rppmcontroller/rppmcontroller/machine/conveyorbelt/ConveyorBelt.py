from rppmcontroller.machine.Machine import Machine
from rppmcontroller.utils.ImpulseCounter import ImpulseCounter
from rppmcontroller.utils.PlusMinusStop import PlusMinusStop
from rppmcontroller.machine.Direction import Direction
from rppmcontroller.machine.RequestedParameter import RequestedParameter
import logging


class ConveyorBelt(Machine):

    @Machine.isExecuting.getter
    def isExecuting(self) -> bool:
        #logging.debug(f"Is executing ! {self.__conveyorActForward or self.__conveyorActBackward}")
        return self.__conveyorActForward or self.__conveyorActBackward
    
    @Machine.isCommandSuccessed.getter
    def isCommandSuccessed(self) -> bool:
        return self.__isCommandSuccessed

    @Machine.isCommandRunning.getter
    def isCommandRunning(self) -> bool:
        return self.__isCommandRunning
    
    @Machine.isCommandTimedOut.getter
    def isCommandTimedOut(self) -> bool:
        return self.__isCommandTimedOut


    def __init__(self, id1):
        self.__conveyorSensImpulseCounterRaw = 0
        self.__conveyorSensFeed = self.__conveyorSensSwap = True #True is the value when there is no object in front of the sensor
        self.__conveyorActForward = self.__conveyorActBackward = False
        self.__counter = ImpulseCounter()
        self.__isCommandSuccessed = False
        self.__isCommandRunning = False
        self.__isCommandTimedOut = False
        self.current = 0
        self.sensed = False
        dictMap = {RequestedParameter.LIGHTBARRIERFEEDSTATION: self.__conveyorSensSwap,
                   RequestedParameter.LIGHTBARRIERSWAPSTATION: self.__conveyorSensFeed,
                   # TODO update self.current to actually count the steps according to the conveyor direction
                   RequestedParameter.PULSECOUNTER: self.current,
                   RequestedParameter.MOTORCONVEYORBELTFORWARD: self.__conveyorActForward,
                   RequestedParameter.MOTORCONVEYORBELTBACKWARD: self.__conveyorActBackward}
        super().__init__(id1, dictMap)

        self.arrived = False
        self.once = True
        self.__isExecutingCount = 0

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

    def forwardFromAnywhere(self):
        """Move the package from any place on the conveyor to the right sensor"""
        self.__conveyorActForward = True
        if not self.__conveyorSensSwap:
            self.__conveyorActForward = False
            self.__isCommandRunning = False
            self.__isCommandSuccessed = True
            return True
        return False


    def backwardFromAnywhere(self):
        """Move the package from any place on the conveyor to the left sensor"""
        self.__conveyorActBackward = True
        if not self.__conveyorSensFeed:
            self.__conveyorActBackward = False
            self.__isCommandRunning = False
            self.__isCommandSuccessed = True
            return True
        return False


    def forwardLeaveConveyor(self):
        """Move the package from anywhere on the line to the left, until it leaves the conveyor. Then stop the conveyor."""
        if not self.arrived:
            self.__conveyorActForward = True
        else:
            if self.countSteps() >= 6:
                self.__conveyorActForward = False
                self.__isCommandRunning = False
                self.__isCommandSuccessed = True

        if not self.__conveyorSensSwap:
            self.current = 0
            self.arrived = True


    def backwardLeaveConveyor(self):
        """Move the package from anywhere on the line to the right, until it leaves the conveyor. Then stop the conveyor."""
        if not self.arrived:
            self.__conveyorActBackward = True
        else:
            if self.countSteps() >= 6:
                self.__conveyorActBackward = False
                self.__isCommandRunning = False
                self.__isCommandSuccessed = True

        if not self.__conveyorSensFeed:
            self.current = 0
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
            self.__isCommandRunning = False
            self.__isCommandSuccessed = True


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
            self.__isCommandRunning = False
            self.__isCommandSuccessed = True


    def countSteps(self):
        """Count the number of steps when the conveyor is moving """
        self.current  = self.current + self.__counter.compute(self.__conveyorSensImpulseCounterRaw, PlusMinusStop.PLUS)
        logging.debug(f"Step counter : {self.current }")
        return self.current 


    def stop(self):
        """Stop the conveyor"""
        self.__conveyorActForward = self.__conveyorActBackward = False
        self.__isCommandRunning = False
        self.__isCommandSuccessed = True
        return None


    def move(self, dir: Direction):
        """Move the package to a given direction until it leaves the conveyor
            Args:
                dir (Direction) : the direction where to move the package

            The conveyor will stop after few steps when the package leaves the coveyor.
        """
        self.arrived = False
        self.__isCommandSuccessed = False
        self.__isCommandRunning = True
        if dir == Direction.FORWARD:
            return lambda: self.forwardLeaveConveyor()
        if dir == Direction.BACKWARD:
            return lambda: self.backwardLeaveConveyor()


    def gotoconfig(self, dir: Direction, steps: int):
        """Move the package to a given direction with a given number of steps
            Args:
                dir (Direction) : the direction where to move the package
                steps (int) : the number of steps you want to move the package

            There is no control of the position of the package. The conveyor wont stop until it reach the number of steps
        """
        self.__isCommandSuccessed = False
        self.__isCommandRunning = True
        self.sensed = False
        if dir == Direction.FORWARD:
            return lambda: self.forwardGoto(steps)
        if dir == Direction.BACKWARD:
            return lambda: self.backwardGoto(steps)
        else:
            logging.error(f"Invalid direction {dir}")


    def movelb(self, dir: Direction):
        """Move the package to a given direction until it is detected in the station
            Args:
                dir (Direction) : the direction where to move the package
        """
        self.__isCommandSuccessed = False
        self.__isCommandRunning = True
        if dir == Direction.FORWARD:
            return lambda: self.forwardFromAnywhere()
        if dir == Direction.BACKWARD:
            return lambda: self.backwardFromAnywhere()

