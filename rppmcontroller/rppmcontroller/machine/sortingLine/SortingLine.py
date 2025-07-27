#from Layout.Machine import Layout.Machine #why is this Layout.Machine???
import logging
from typing import Any, Callable, Dict, Optional

from typing_extensions import override

from rppmcontroller.behavior.CycleStepResult import CycleStepResult
from rppmcontroller.behavior.CycleStepResultEnum import CycleStepResultEnum
from rppmcontroller.behavior.decoratorFunctions import cycle_step_function
from rppmcontroller.machine.Color import Color
from rppmcontroller.machine.Machine import Machine
from rppmcontroller.machine.RequestedParameter import RequestedParameter
from rppmcontroller.machine.Runner import TransitioningMachine, Runner
from rppmcontroller.machine.sortingLine.SortingLineConfig import SortingLineConfig
from rppmcontroller.machine.Timer import Timer
from rppmcontroller.protocol.decoratorFunctions import protocol_command_function
from rppmcontroller.utils.ImpulseCounter import ImpulseCounter
from rppmcontroller.utils.PlusMinusStop import PlusMinusStop


class SortingLine(Machine, TransitioningMachine):

    @property
    def isInitialized(self) -> bool:
        return True # no encoder actuators

    @isInitialized.setter
    def isInitialized(self, value):
        logging.warning(f"Attempted to set read-only property 'isInitialized' on {self}")
        raise AttributeError("isInitialized is a read-only property") 
    
    #TODO self.once: implement reset possibility from execute
    
    @Machine.isExecuting.getter
    def isExecuting(self) -> bool:
        res = (self.sortingLineActCompressorOn or
                self.sortingLineActMotorConveyor or
                self.sortingLineActWhiteEjector or
                self.sortingLineActRedEjector or
                self.sortingLineActBlueEjector or
                self.is_executing_runner)
        
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

    def __init__(self, id1: str, delay_offsets = (0.0, 0.0, 0.0)):
        # configuration
        self.__delay_offsets = delay_offsets # offset for the delay while ejecting white, red, blue tokens

        # inputs
        self.__sortingLineSensImpulseCounterRaw = 0
        self.__sortingLineSensInputLightBarrier = True
        self.__sortingLineSensMiddleLightBarrier = True
        self.__sortingLineSensWhiteLightBarrier = True
        self.__sortingLineSensBlueLightBarrier = True
        self.__sortingLineSensRedLightBarrier = True

        self.__sortingLineSensColorDetector = False
        self.__sortingLineSensBlueDetector = False
        self.__sortingLineSensRedDetector = False
        self.__sortingLineSensWhiteDetector = False

        #outputs
        self.__sortingLineActMotorConveyor = False
        self.__sortingLineActCompressorOn = False
        self.__sortingLineActWhiteEjector = False
        self.__sortingLineActRedEjector = False
        self.__sortingLineActBlueEjector = False
        dictMap = {RequestedParameter.LIGHTBARRIERINLET: self.__sortingLineSensInputLightBarrier,
                   RequestedParameter.LIGHTBARRIERBEHINDCOLORSENSOR: self.__sortingLineSensMiddleLightBarrier,
                   RequestedParameter.LIGHTBARRIERWHITE: self.__sortingLineSensWhiteLightBarrier,
                   RequestedParameter.LIGHTBARRIERRED: self.__sortingLineSensRedLightBarrier,
                   RequestedParameter.LIGHTBARRIERBLUE: self.__sortingLineSensBlueLightBarrier,
                   RequestedParameter.MOTORCONVEYORBELT: self.__sortingLineActMotorConveyor,
                   RequestedParameter.COMPRESSOR: self.__sortingLineActCompressorOn,
                   RequestedParameter.VALVEFIRSTEJECTORWHITE: self.__sortingLineActWhiteEjector,
                   RequestedParameter.VALVESECONDEJECTORRED: self.__sortingLineActRedEjector,
                   RequestedParameter.VALVETHIRDEJECTORBLUE: self.__sortingLineActBlueEjector,
                   RequestedParameter.SENSCOLORDETECTOR: self.__sortingLineSensColorDetector,
                   RequestedParameter.SENSBLUEDETECTOR: self.__sortingLineSensBlueDetector,
                   RequestedParameter.SENSREDDETECTOR: self.__sortingLineSensRedDetector,
                   RequestedParameter.SENSWHITEDETECTOR: self.__sortingLineSensWhiteDetector}
        super().__init__(id1, dictMap)
        TransitioningMachine.__init__(self)

        # helper variables
        self.__packageOnLine = False
        self.__packageCountSteps = False
        self.once = True
        self.previous_isExecuting_log = None
        self.colorToEject = Color.AUTO
        self.ejectingPayload = False
        self.waitOneCycle = False
        self.timer = None
        self.halfSecondTimer = Timer(0.5)
        # TODO(Hellwig): are the methods using the counter still used?
        self.__counter = ImpulseCounter()

    @property
    def sortingLineSensImpulseCounterRaw(self):
        return self.__sortingLineSensImpulseCounterRaw

    @sortingLineSensImpulseCounterRaw.setter
    def sortingLineSensImpulseCounterRaw(self, value):
        self.__sortingLineSensImpulseCounterRaw = value

    @property
    def sortingLineSensInputLightBarrier(self):
        return self.__sortingLineSensInputLightBarrier

    @sortingLineSensInputLightBarrier.setter
    def sortingLineSensInputLightBarrier(self, value):
        self.__sortingLineSensInputLightBarrier = value

    @property
    def sortingLineSensMiddleLightBarrier(self):
        return self.__sortingLineSensMiddleLightBarrier

    @sortingLineSensMiddleLightBarrier.setter
    def sortingLineSensMiddleLightBarrier(self, value):
        self.__sortingLineSensMiddleLightBarrier = value

    @property
    def sortingLineSensWhiteLightBarrier(self):
        return self.__sortingLineSensWhiteLightBarrier

    @sortingLineSensWhiteLightBarrier.setter
    def sortingLineSensWhiteLightBarrier(self, value):
        self.__sortingLineSensWhiteLightBarrier = value

    @property
    def sortingLineSensRedLightBarrier(self):
        return self.__sortingLineSensRedLightBarrier

    @sortingLineSensRedLightBarrier.setter
    def sortingLineSensRedLightBarrier(self, value):
        self.__sortingLineSensRedLightBarrier = value

    @property
    def sortingLineSensBlueLightBarrier(self):
        return self.__sortingLineSensBlueLightBarrier

    @sortingLineSensBlueLightBarrier.setter
    def sortingLineSensBlueLightBarrier(self, value):
        self.__sortingLineSensBlueLightBarrier = value

    @property
    def sortingLineActMotorConveyor(self):
        return self.__sortingLineActMotorConveyor

    @sortingLineActMotorConveyor.setter
    def sortingLineActMotorConveyor(self, value):
        self.__sortingLineActMotorConveyor = value

    @property
    def sortingLineActCompressorOn(self):
        return self.__sortingLineActCompressorOn

    @sortingLineActCompressorOn.setter
    def sortingLineActCompressorOn(self, value):
        self.__sortingLineActCompressorOn = value

    @property
    def sortingLineActWhiteEjector(self):
        return self.__sortingLineActWhiteEjector

    @sortingLineActWhiteEjector.setter
    def sortingLineActWhiteEjector(self, value):
        self.__sortingLineActWhiteEjector = value

    @property
    def sortingLineActRedEjector(self):
        return self.__sortingLineActRedEjector

    @sortingLineActRedEjector.setter
    def sortingLineActRedEjector(self, value):
        self.__sortingLineActRedEjector = value

    @property
    def sortingLineActBlueEjector(self):
        return self.__sortingLineActBlueEjector

    @sortingLineActBlueEjector.setter
    def sortingLineActBlueEjector(self, value):
        self.__sortingLineActBlueEjector = value

    @property
    def sortingLineSensColorDetector(self):
        return self.__sortingLineSensColorDetector

    @sortingLineSensColorDetector.setter
    def sortingLineSensColorDetector(self, value):
        self.__sortingLineSensColorDetector = value

    @property
    def sortingLineSensRedDetector(self):
        return self.__sortingLineSensRedDetector

    @sortingLineSensRedDetector.setter
    def sortingLineSensRedDetector(self, value):
        self.__sortingLineSensRedDetector = value

    @property
    def sortingLineSensBlueDetector(self):
        return self.__sortingLineSensBlueDetector

    @sortingLineSensBlueDetector.setter
    def sortingLineSensBlueDetector(self, value):
        self.__sortingLineSensBlueDetector = value

    @property
    def sortingLineSensWhiteDetector(self):
        return self.__sortingLineSensWhiteDetector

    @sortingLineSensWhiteDetector.setter
    def sortingLineSensWhiteDetector(self, value):
        self.__sortingLineSensWhiteDetector = value

    def sensorStatusString(self) -> str:
        s = lambda bool: "T" if bool else "F"

        return f"ConvSens[{s(self.sortingLineSensInputLightBarrier)}, {s(self.sortingLineSensMiddleLightBarrier)}], " + \
               f"ColoSens[{s(self.sortingLineSensWhiteLightBarrier)}, {s(self.sortingLineSensBlueLightBarrier)}, {s(self.sortingLineSensRedLightBarrier)}], " + \
               f"DeteColo[{s(self.sortingLineSensColorDetector)}, {s(self.sortingLineSensBlueDetector)}, {s(self.sortingLineSensRedDetector)}, {s(self.sortingLineSensWhiteDetector)}], " + \
               f"Counter[{self.sortingLineSensImpulseCounterRaw}]"

    def actuatorStatusString(self) -> str:
        s = lambda bool: "T" if bool else "F"

        return f"Conveyor[{s(self.sortingLineActMotorConveyor)}], " + \
               f"CompValv[{s(self.sortingLineActCompressorOn)}, {s(self.sortingLineActWhiteEjector)}, {s(self.sortingLineActRedEjector)}, {s(self.sortingLineActBlueEjector)}]"


    def inputStatus(self) -> Dict[str, Any]:
        status = {
            "sortingLineSensInputLightBarrier": self.sortingLineSensInputLightBarrier,
            "sortingLineSensMiddleLightBarrier": self.sortingLineSensMiddleLightBarrier,
            "sortingLineSensWhiteLightBarrier": self.sortingLineSensWhiteLightBarrier,
            "sortingLineSensBlueLightBarrier": self.sortingLineSensBlueLightBarrier,
            "sortingLineSensRedLightBarrier": self.sortingLineSensRedLightBarrier,
            "sortingLineSensImpulseCounterRaw": self.sortingLineSensImpulseCounterRaw,
        }
        return status

    def outputStatus(self) -> Dict[str, Any]:

        status = {
            "sortingLineActMotorConveyor": self.sortingLineActMotorConveyor,
            "sortingLineActCompressorOn": self.sortingLineActCompressorOn,
            "sortingLineActWhiteEjector": self.sortingLineActWhiteEjector,
            "sortingLineActRedEjector": self.sortingLineActRedEjector,
            "sortingLineActBlueEjector": self.sortingLineActBlueEjector,
        }
        return status

    def internalStatus(self) -> Dict[str, Any]:
        status = {
            "isExecuting": self.isExecuting,
        }
        return status

    def goto_config(self, config: SortingLineConfig) -> CycleStepResult:
        self.sortingLineActMotorConveyor = config.conveyor_active
        self.sortingLineActWhiteEjector = config.white_ejector_active
        self.sortingLineActRedEjector = config.red_ejector_active
        self.sortingLineActBlueEjector = config.blue_ejector_active
        self.sortingLineActCompressorOn = (self.sortingLineActWhiteEjector or
                                           self.sortingLineActRedEjector or
                                           self.__sortingLineActBlueEjector)
        return CycleStepResult.done()

    @override
    @cycle_step_function()
    def stop_CycleStep(self) -> CycleStepResult:
        self.__sortingLineActMotorConveyor = False
        self.__sortingLineActCompressorOn = False
        self.__sortingLineActRedEjector = self.__sortingLineActBlueEjector = self.__sortingLineActWhiteEjector = False
        self.__packageOnLine = False
        self.__packageCountSteps = False
        # self.once = True
        self.__counter.counter = 0
        return CycleStepResult(CycleStepResultEnum.DONE,
                                    f"stop_CycleStep",
                                    None)
    
    @cycle_step_function()
    def detectColor_CycleStep(self) -> CycleStepResult:
        """
        Used to detect the color and store it in attribute colorToEject
        If the detector does not work, it will send an error

        This function is a cycleStep, it is call on each controller cycle, until its goal is reached

        :return: as a CycleStep, this function must return True when it is finished so it can be removed from the currentlyExecuting map
        """
        # If a color is detected, we must set the attribute and finish the cyclestep
        if self.sortingLineSensColorDetector:
            if self.sortingLineSensRedDetector:
                self.colorToEject = Color.RED
                return CycleStepResult(CycleStepResultEnum.DONE, f"detectColorCycleStep", None)
            if self.sortingLineSensBlueDetector:
                self.colorToEject = Color.BLUE
                return CycleStepResult(CycleStepResultEnum.DONE, f"detectColorCycleStep", None)
            if self.sortingLineSensWhiteDetector:
                self.colorToEject = Color.WHITE
                return CycleStepResult(CycleStepResultEnum.DONE, f"detectColorCycleStep", None)
            
        # If the token has arrived to the middle sensor, the detector didn't work
        if not self.sortingLineSensMiddleLightBarrier:
            logging.error("The detector didn't send any signal, verify the analogic/digital converter")
            return CycleStepResult(CycleStepResultEnum.ABORTED_ERROR, f"detectColorCycleStep", None)
        
        # Else, must continue
        return CycleStepResult(CycleStepResultEnum.MUST_CONTINUE, f"detectColorCycleStep", None)
    
    @cycle_step_function()
    def ejectPayloadBySteps_CycleStep(self) -> CycleStepResult:
        """
        Used to eject a token,
        it eject the token to the appropriate colored line, it ends with a token detected in the color line.

        This function is a cycleStep, it is call on each controller cycle, until its goal is reached

        |!!!| This function is really imprecise due to steps miscalculation (cf references/precision_study) |!!!|

        :return: as a CycleStep, this function must return True when it is finished so it can be removed from the currentlyExecuting map
        """
        # Reset counter if first pass
        if not self.ejectingPayload:
            self.ejectingPayload = True
            self.__counter.compute(self.__sortingLineSensImpulseCounterRaw, PlusMinusStop.PLUS)
            self.__counter.counter = 0

        current = self.__counter.compute(self.__sortingLineSensImpulseCounterRaw, PlusMinusStop.PLUS)
        whiteCounter = 4
        redCounter = 14
        blueCounter = 24

        # If counter attained the steps needed, eject 
        if (self.colorToEject == Color.BLUE and current > blueCounter) or \
           (self.colorToEject == Color.RED and current > redCounter) or  \
           (self.colorToEject == Color.WHITE and current > whiteCounter):
            
            self.__sortingLineActCompressorOn = True
            if self.colorToEject == Color.RED:
                self.__sortingLineActRedEjector = True
            elif self.colorToEject == Color.BLUE:
                self.__sortingLineActBlueEjector = True
            elif self.colorToEject == Color.WHITE:
                self.__sortingLineActWhiteEjector = True

            # Wait 5 steps more to retract the arm and end the command 
            if (self.colorToEject == Color.BLUE and current > blueCounter+5) or \
               (self.colorToEject == Color.RED and current > redCounter+5) or   \
               (self.colorToEject == Color.WHITE and current > whiteCounter+5):
                
                self.ejectingPayload = False
                self.__sortingLineActMotorConveyor = False
                self.__sortingLineActCompressorOn = False
                self.__sortingLineActRedEjector = False
                self.__sortingLineActBlueEjector = False
                self.__sortingLineActWhiteEjector = False
                return CycleStepResult(CycleStepResultEnum.DONE, f"detectColorCycleStep", None)

        return CycleStepResult(CycleStepResultEnum.MUST_CONTINUE, f"detectColorCycleStep", None)
    
    @cycle_step_function()
    def ejectPayloadByTime_CycleStep(self) -> CycleStepResult:
        """
        Used to eject a token,
        it eject the token to the appropriate colored line, it ends with a token detected in the color line.

        This function is a cycleStep, it is call on each controller cycle, until its goal is reached

        :return: as a CycleStep, this function must return True when it is finished so it can be removed from the currentlyExecuting map
        """
        regenerateTimer = False

        # If first pass, make sure to regenerate the timer
        if not self.ejectingPayload:
            self.ejectingPayload = True
            regenerateTimer = True
        
        # Regenerate the timer according to the color of the payload
        if regenerateTimer or self.timer == None:
            if self.colorToEject == Color.RED:
                self.timer = Timer(1.55)
            elif self.colorToEject == Color.BLUE:
                self.timer = Timer(2.6)
            elif self.colorToEject == Color.WHITE:
                self.timer = Timer(0.5)
            else:
                logging.error("No color defined, command aborted")
                return CycleStepResult(CycleStepResultEnum.ABORTED_ERROR, f"detectColorCycleStep", None)
        
        # Start timer
        if not self.timer.is_started():
            self.timer.start()

        # If timer is elapsed, activate the piston and relaunch the timer
        if self.timer.is_started() and self.timer.elapsed() and not self.__sortingLineActCompressorOn:            
            self.__sortingLineActCompressorOn = True
            self.__sortingLineActMotorConveyor = False
            if self.colorToEject == Color.RED:
                self.__sortingLineActRedEjector = True
            elif self.colorToEject == Color.BLUE:
                self.__sortingLineActBlueEjector = True
            elif self.colorToEject == Color.WHITE:
                self.__sortingLineActWhiteEjector = True
            
            self.halfSecondTimer.reset(start=True)

        # If timer is elapsed, finish the command
        if self.halfSecondTimer.is_started() and self.halfSecondTimer.elapsed() and self.__sortingLineActCompressorOn: 
            
            self.ejectingPayload = False
            self.__sortingLineActCompressorOn = False
            self.__sortingLineActRedEjector = False
            self.__sortingLineActBlueEjector = False
            self.__sortingLineActWhiteEjector = False
            self.timer = None
            return CycleStepResult(CycleStepResultEnum.DONE, f"detectColorCycleStep", None)

        return CycleStepResult(CycleStepResultEnum.MUST_CONTINUE, f"detectColorCycleStep", None)

    ### ____________ Functions callable from orchestrator ________________
    #   function name must be lowercase and finish with '_Command' postfix (cf. RevPiPyMachineController)

    @protocol_command_function(description="Command stopping all engines (incl. compressor).")
    def stop_Command(self) -> Callable[[], CycleStepResult]:
        return self.stop_CycleStep

    @protocol_command_function(description="Eject the payload on specified color. If the color is set to auto, detect the color before.")
    def eject_Command(self, color: Color) -> Runner:
        """
        Eject the payload on specified color. If the color is set to auto, detect the color before.
        :param color: The color to sort the payload into
        :return: A Runner
        """
        runner = self.create_runner()
        config = SortingLineConfig()

        # start conveyor
        config.conveyor_active = True
        runner.then_goto(config, info="Going to middle sensor")
        
        # if color defined, set the attribute to the color
        if not color == Color.AUTO:
            self.colorToEject = color
        # if color set to auto, we need to detect the color before ejecting
        else :
            runner.then_run(self.detectColor_CycleStep, info="Detecting color")

        # Forward to middle sensor
        runner.then_goto(config, until=lambda: not self.sortingLineSensMiddleLightBarrier, info="Going to middle sensor")
        
        # Eject the payload in right output
        self.ejectingPayload = False
        runner.then_run(self.ejectPayloadByTime_CycleStep, info="Ejecting payload")

        return runner.run()