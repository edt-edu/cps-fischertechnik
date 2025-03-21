from rppmcontroller.machine.Machine import Machine
from rppmcontroller.utils.ImpulseCounter import ImpulseCounter
from rppmcontroller.utils.PlusMinusStop import PlusMinusStop
from rppmcontroller.machine.Direction import Direction
from rppmcontroller.machine.RequestedParameter import RequestedParameter
from typing import Any, Callable, Dict, Optional, Tuple
from typing_extensions import override
import logging


class ConveyorBelt(Machine):


    
    @Machine.isExecuting.getter
    def isExecuting(self) -> bool:
        res = self.__conveyorActForward or self.__conveyorActBackward
        # log isexecuting and debug info only if message has changed
        isExecuting_log = f'isExecuting({self.id})={res} | Sensors={self.sensorStatusString()} | Actuators= {self.actuatorStatusString()}'
        if isExecuting_log != self.previous_isExecuting_log :
            logging.debug(isExecuting_log)
            self.previous_isExecuting_log = isExecuting_log
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


    ### ____________ Functions intended to be called in the exLoop function of the RevPiPyMachineController ________________

    def forwardFromAnywhere_CycleStep(self) -> bool:
        """Move the package from any place on the conveyor to the right sensor
        :return: as a CycleStep, this function must return True when it is finished so it can be removed from the currentlyExecuting map"""
        self.__conveyorActForward = True
        if not self.__conveyorSensSwap:
            self.__conveyorActForward = False
            self.__isCommandRunning = False
            self.__isCommandSuccessed = True
            return True
        return False


    def backwardFromAnywhere_CycleStep(self) -> bool:
        """Move the package from any place on the conveyor to the left sensor
        :return: as a CycleStep, this function must return True when it is finished so it can be removed from the currentlyExecuting map"""
        self.__conveyorActBackward = True
        if not self.__conveyorSensFeed:
            self.__conveyorActBackward = False
            self.__isCommandRunning = False
            self.__isCommandSuccessed = True
            return True
        return False


    def forwardLeaveConveyor_CycleStep(self) -> bool:
        """Move the package from anywhere on the line to the left, until it leaves the conveyor. Then stop the conveyor.
        :return: as a CycleStep, this function must return True when it is finished so it can be removed from the currentlyExecuting map"""
        ret = False
        if not self.arrived:
            self.__conveyorActForward = True
        else:
            if self.countSteps() >= 6:
                self.__conveyorActForward = False
                self.__isCommandRunning = False
                self.__isCommandSuccessed = True
                ret = True # command final goal reached, no need to call this cycleStep again

        if not self.__conveyorSensSwap:
            self.current = 0
            self.arrived = True
        return ret


    def backwardLeaveConveyor_CycleStep(self) -> bool:
        """Move the package from anywhere on the line to the right, until it leaves the conveyor. Then stop the conveyor.
        :return: as a CycleStep, this function must return True when it is finished so it can be removed from the currentlyExecuting map"""
        ret = False
        if not self.arrived:
            self.__conveyorActBackward = True
        else:
            if self.countSteps() >= 6:
                self.__conveyorActBackward = False
                self.__isCommandRunning = False
                self.__isCommandSuccessed = True
                ret = True # command final goal reached, no need to call this cycleStep again

        if not self.__conveyorSensFeed:
            self.current = 0
            self.arrived = True
        return ret


    def forwardGoto_CycleStep(self, steps: int) -> bool:
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
            self.__isCommandRunning = False
            self.__isCommandSuccessed = True
            return True  # command final goal reached, no need to call this cycleStep again
        else:
            return False


    def backwardGoto_CycleStep(self, steps: int) -> bool:
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
            self.__isCommandRunning = False
            self.__isCommandSuccessed = True
            return True  # command final goal reached, no need to call this cycleStep again
        else:
            return False


    @override
    def stop_CycleStep(self) -> bool:
        """Stop the conveyor"""
        self.__conveyorActForward = self.__conveyorActBackward = False
        self.__isCommandRunning = False
        self.__isCommandSuccessed = True
        return True

        
    ### ____________ Functions callable from orchestrator ________________
    #   function name must be lowercase and finish with '_Command' postfix (cf. RevPiPyMachineController)

    def stop_Command(self) -> Optional[Callable[[], bool]]:
        """Stop the conveyor"""
        return lambda: self.stop_CycleStep()


    def move_out_Command(self, dir: Direction) -> Optional[Callable[[], bool]]:
        """Move the package to a given direction until it leaves the conveyor
            Args:
                dir (Direction) : the direction where to move the package

            The conveyor will stop after few steps when the package leaves the coveyor.
        """
        self.arrived = False
        self.__isCommandSuccessed = False
        self.__isCommandRunning = True
        if dir == Direction.FORWARD:
            return lambda: self.forwardLeaveConveyor_CycleStep()
        if dir == Direction.BACKWARD:
            return lambda: self.backwardLeaveConveyor_CycleStep()

    def move_nb_steps_Command(self, dir: Direction, steps: int) -> Optional[Callable[[], bool]]:
        """Move the conveyor belt to a given direction with a given number of steps
            Args:
                dir (Direction) : the direction where to move the package
                steps (int) : the number of steps you want to move the package

            There is no control of the position of the package. The conveyor wont stop until it reach the number of steps
        """
        self.__isCommandSuccessed = False
        self.__isCommandRunning = True
        self.sensed = False
        if dir == Direction.FORWARD:
            return lambda: self.forwardGoto_CycleStep(steps)
        if dir == Direction.BACKWARD:
            return lambda: self.backwardGoto_CycleStep(steps)
        else:
            logging.error(f"Invalid direction {dir}")


    def move_to_sensor_Command(self, dir: Direction) -> Optional[Callable[[], bool]]:
        """Move the package to a given direction until it is detected by the destination sensor
            Args:
                dir (Direction) : the direction where to move the package
        """
        self.__isCommandSuccessed = False
        self.__isCommandRunning = True
        if dir == Direction.FORWARD:
            return lambda: self.forwardFromAnywhere_CycleStep()
        if dir == Direction.BACKWARD:
            return lambda: self.backwardFromAnywhere_CycleStep()

