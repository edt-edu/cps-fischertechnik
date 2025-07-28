import logging
from typing import Any, Callable, Dict, Optional

from typing_extensions import override

from rppmcontroller.behavior.CycleStepResult import CycleStepResult
from rppmcontroller.behavior.CycleStepResultEnum import CycleStepResultEnum
from rppmcontroller.behavior.decoratorFunctions import cycle_step_function
from rppmcontroller.machine.ConveyorState import ConveyorState
from rppmcontroller.machine.Direction import Direction
from rppmcontroller.machine.Machine import Machine
from rppmcontroller.machine.RequestedParameter import RequestedParameter
from rppmcontroller.machine.Runner import TransitioningMachine, Runner
from rppmcontroller.machine.conveyorbelt.ConveyorBeltConfig import \
    ConveyorBeltConfig
from rppmcontroller.protocol.decoratorFunctions import protocol_command_function
from rppmcontroller.utils.ImpulseCounter import ImpulseCounter
from rppmcontroller.utils.PlusMinusStop import PlusMinusStop


class ConveyorBelt(Machine, TransitioningMachine[ConveyorBeltConfig]):

    @property
    def isInitialized(self) -> bool:
        return True

    @isInitialized.setter
    def isInitialized(self, value):
        logging.warning(f"Attempted to set read-only property 'isInitialized' on {self}")
        raise AttributeError("isInitialized is a read-only property") 
    
    @Machine.isExecuting.getter
    def isExecuting(self) -> bool:
        res = self.__conveyorActForward or self.__conveyorActBackward
        
        if (self.executing_runner == None):
            routine = "None"
        else:
            routine = str(self.executing_runner)
        
        # log isexecuting and debug info only if message has changed
        isExecuting_log = f'\n\tisExecuting({self.id})={res}\n\tRoutine : {routine}\n\tSensors={self.sensorStatusString()}\n\tActuators= {self.actuatorStatusString()}'
        if isExecuting_log != self.previous_isExecuting_log :
            logging.debug(isExecuting_log)
            self.previous_isExecuting_log = isExecuting_log
        return res


    def __init__(self, id1):
        # inputs


        self.__conveyorSensImpulseCounterRaw = 0
        self.__conveyorSensFeed = True
        self.__conveyorSensSwap = True

        # outputs
        self.__conveyorActForward = False
        self.__conveyorActBackward = False


        dictMap = {RequestedParameter.LIGHTBARRIERFEEDSTATION: self.__conveyorSensSwap,
                   RequestedParameter.LIGHTBARRIERSWAPSTATION: self.__conveyorSensFeed,
                   RequestedParameter.MOTORCONVEYORBELTFORWARD: self.__conveyorActForward,
                   RequestedParameter.MOTORCONVEYORBELTBACKWARD: self.__conveyorActBackward}
        super().__init__(id1, dictMap)
        TransitioningMachine.__init__(self)

        # helper variables
        self.__counter = ImpulseCounter()
        self.current = 0
        self.arrived = False
        self.once = True
        self.previous_isExecuting_log = None
        self.sensed = None

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
        s = lambda bool: "T" if bool else "F"
        return f"Sensors[{s(self.conveyorSensFeed)}, {s(self.conveyorSensSwap)}, {s(self.conveyorSensImpulse)}]"

    def actuatorStatusString(self) -> str:
        s = lambda bool: "T" if bool else "F"
        return f"Conveyor[{s(self.conveyorActForward)}, {s(self.conveyorActBackward)}]"

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
        self.current  = self.__counter.compute(self.__conveyorSensImpulseCounterRaw, PlusMinusStop.PLUS)
        # logging.debug(f"Step counter : {self.current }")
        return self.current

    @override
    @cycle_step_function()
    def goto_config_CycleStep(self, config: ConveyorBeltConfig) -> CycleStepResult:
        self.conveyorActForward = config.state is ConveyorState.FORWARD
        self.conveyorActBackward = config.state is ConveyorState.BACKWARD
        return CycleStepResult(CycleStepResultEnum.DONE, "target config reached")

    ### ____________ Functions intended to be called in the exLoop function of the RevPiPyMachineController ________________

    @override
    @cycle_step_function()
    def stop_CycleStep(self) -> CycleStepResult:
        """Stop the conveyor"""
        self.__conveyorActForward = False
        self.__conveyorActBackward = False
        return CycleStepResult(CycleStepResultEnum.DONE)


    ### ____________ Functions callable from orchestrator ________________
    #   function name must be lowercase and finish with '_Command' postfix (cf. RevPiPyMachineController)

    @protocol_command_function()
    def stop_Command(self) -> Callable[[], CycleStepResult]:
        """Stop the conveyor"""
        return self.stop_CycleStep

    @protocol_command_function()
    def move_out_Command(self, direction: Direction) -> Runner:
        """Move the package to a given direction until it leaves the conveyor
            Args:
                direction (Direction) : the direction where to move the package

            The conveyor will stop after few steps when the package leaves the coveyor.
        """
        runner = self.create_runner()

        # move to sensor
        runner.then_run_runner_from(lambda: self.move_to_sensor_Command(direction), info="Move to sensor")
        # keep moving for a second or so
        runner.then_goto(ConveyorBeltConfig(state=ConveyorState.from_direction(direction)), and_stay_for=1.0, info="Sensor passed")
        # stop the belt
        runner.then_goto(ConveyorBeltConfig(), info="Conveyor stopped")
        return runner.run()

    @protocol_command_function()
    def move_nb_steps_Command(self, direction: Direction, steps: int) -> Runner:
        """Move the conveyor belt to a given direction with a given number of steps
            Args:
                direction (Direction) : the direction where to move the package
                steps (int) : the number of steps you want to move the package

            There is no control of the position of the package. The conveyor wont stop until it reach the number of steps
        """
        #Create the runner and the config
        runner = self.create_runner()
        config = ConveyorBeltConfig()

        state = ConveyorState.from_direction(direction)
        config.state = state

        self.current = self.countSteps()
        goal : int = self.current+steps

        state = ConveyorState.from_direction(direction)
        nb_cycles_reached = lambda: self.current >= goal

        runner.then_goto(ConveyorBeltConfig(state=ConveyorState.from_direction(direction)), info="Belt moving")

        def runnable()-> None:
            self.current = self.countSteps()
            logging.debug(f"goal : {goal} counter : {self.current}")
        
        config.state = state
        runner.then_run(runnable, until=nb_cycles_reached, info="Wait number of steps")

        #stop the belt
        runner.then_goto(ConveyorBeltConfig(), info="Conveyor stopped")

        logging.debug(f"Actualgoal : {goal} counter : {self.current}")

        return runner.run()

    @protocol_command_function()
    def move_to_sensor_Command(self, direction: Direction):
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
        runner.then_goto(config, until=sensor_reached, and_stay_for=0.07, info="Move to sensor")


        #stop the belt
        config.state = ConveyorState.IDLE
        runner.then_goto(config, info="Conveyor stopped")

        

        return runner.run()
