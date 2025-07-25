import logging
from typing import Dict, Any, Callable

from rppmcontroller.behavior.CycleStepResult import CycleStepResult
from rppmcontroller.behavior.CycleStepResultEnum import CycleStepResultEnum
from rppmcontroller.machine.Machine import Machine
from rppmcontroller.machine.RequestedParameter import RequestedParameter
from rppmcontroller.machine.Runner import TransitioningMachine, Runner
from rppmcontroller.machine.indexedline.IndexedLineConfig import \
    IndexedLineConfig
from rppmcontroller.protocol.decoratorFunctions import protocol_command_function


class IndexedLine(Machine, TransitioningMachine):
    def __init__(self, id1):
        # inputs
        self.__indexedLineSensSlider1Front = False
        self.__indexedLineSensSlider1Rear = False
        self.__indexedLineSensSlider2Front = False
        self.__indexedLineSensSlider2Rear = False
        self.__indexedLineSensSlider1 = True
        self.__indexedLineSensMilling = True
        self.__indexedLineSensLoading = True
        self.__indexedLineSensDrilling = True
        self.__indexedLineSensSwap = True

        # outputs
        self.__indexedLineActSlider1Forward = False
        self.__indexedLineActSlider1Backward = False
        self.__indexedLineActSlider2Forward = False
        self.__indexedLineActSlider2Backward = False
        self.__indexedLineActFeedConveyor = False
        self.__indexedLineActMillingConveyor = False
        self.__indexedLineActDrillingConveyor = False
        self.__indexedLineActSwapConveyor = False
        self.__indexedLineActMilling = False
        self.__indexedLineActDrilling = False

        dictMap = {
            RequestedParameter.PUSHBUTTONSLIDER1FRONT: self.__indexedLineSensSlider1Front,
            RequestedParameter.PUSHBUTTONSLIDER1REAR: self.__indexedLineSensSlider1Rear,
            RequestedParameter.PUSHBUTTONSLIDER2FRONT: self.__indexedLineSensSlider2Front,
            RequestedParameter.PUSHBUTTONSLIDER2REAR: self.__indexedLineSensSlider2Rear,
            RequestedParameter.LIGHTBARRIERSLIDER1: self.__indexedLineSensSlider1,
            RequestedParameter.LIGHTBARRIERMILLINGMACHINE: self.__indexedLineSensMilling,
            RequestedParameter.LIGHTBARRIERLOADINGSTATION: self.__indexedLineSensLoading,
            RequestedParameter.LIGHTBARRIERDRILLINGMACHINE: self.__indexedLineSensDrilling,
            RequestedParameter.LIGHTBARRIERSWAPSTATION: self.__indexedLineSensSwap,
            RequestedParameter.MOTORSLIDER1FORWARD: self.__indexedLineActSlider1Forward,
            RequestedParameter.MOTORSLIDER1BACKWARD: self.__indexedLineActSlider1Backward,
            RequestedParameter.MOTORSLIDER2FORWARD: self.__indexedLineActSlider2Forward,
            RequestedParameter.MOTORSLIDER2BACKWARD: self.__indexedLineActSlider2Backward,
            RequestedParameter.MOTORCONVEYORBELTFEED: self.__indexedLineActFeedConveyor,
            RequestedParameter.MOTORCONVEYORBELTMILLINGMACHINE: self.__indexedLineActMillingConveyor,
            RequestedParameter.MOTORCONVEYORBELTDRILLINGMACHINE: self.__indexedLineActDrillingConveyor,
            RequestedParameter.MOTORCONVEYORBELTSWAP: self.__indexedLineActSwapConveyor,
            RequestedParameter.MOTORMILLINGMACHINE: self.__indexedLineActMilling,
            RequestedParameter.MOTORDRILLINGMACHINE: self.__indexedLineActDrilling,
            }
        super().__init__(id1, dictMap)
        TransitioningMachine.__init__(self)

        # helper variables
        self.previous_is_executing_log = None

    @property
    def isExecuting(self) -> bool:
        res = (self.__indexedLineActSlider1Forward or
            self.__indexedLineActSlider1Backward or
            self.__indexedLineActSlider2Forward or
            self.__indexedLineActSlider2Backward or
            self.__indexedLineActFeedConveyor or
            self.__indexedLineActMillingConveyor or
            self.__indexedLineActDrillingConveyor or
            self.__indexedLineActSwapConveyor or
            self.__indexedLineActMilling or
            self.__indexedLineActDrilling or
            self.is_executing_runner)
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
        return True # no actuators, so we are always ready
    
    @isInitialized.setter
    def isInitialized(self, value):
        logging.warning(f"Attempted to set read-only property 'isInitialized' on {self}")
        raise AttributeError("isInitialized is a read-only property") 

    # ---- Input Properties ----
    @property
    def indexedLineSensSlider1Front(self):
        return self.__indexedLineSensSlider1Front

    @indexedLineSensSlider1Front.setter
    def indexedLineSensSlider1Front(self, value):
        self.__indexedLineSensSlider1Front = value

    @property
    def indexedLineSensSlider1Rear(self):
        return self.__indexedLineSensSlider1Rear

    @indexedLineSensSlider1Rear.setter
    def indexedLineSensSlider1Rear(self, value):
        self.__indexedLineSensSlider1Rear = value

    @property
    def indexedLineSensSlider2Front(self):
        return self.__indexedLineSensSlider2Front

    @indexedLineSensSlider2Front.setter
    def indexedLineSensSlider2Front(self, value):
        self.__indexedLineSensSlider2Front = value

    @property
    def indexedLineSensSlider2Rear(self):
        return self.__indexedLineSensSlider2Rear

    @indexedLineSensSlider2Rear.setter
    def indexedLineSensSlider2Rear(self, value):
        self.__indexedLineSensSlider2Rear = value

    @property
    def indexedLineSensSlider1(self):
        return self.__indexedLineSensSlider1

    @indexedLineSensSlider1.setter
    def indexedLineSensSlider1(self, value):
        self.__indexedLineSensSlider1 = value

    @property
    def indexedLineSensMilling(self):
        return self.__indexedLineSensMilling

    @indexedLineSensMilling.setter
    def indexedLineSensMilling(self, value):
        self.__indexedLineSensMilling = value

    @property
    def indexedLineSensLoading(self):
        return self.__indexedLineSensLoading

    @indexedLineSensLoading.setter
    def indexedLineSensLoading(self, value):
        self.__indexedLineSensLoading = value

    @property
    def indexedLineSensDrilling(self):
        return self.__indexedLineSensDrilling

    @indexedLineSensDrilling.setter
    def indexedLineSensDrilling(self, value):
        self.__indexedLineSensDrilling = value

    @property
    def indexedLineSensSwap(self):
        return self.__indexedLineSensSwap

    @indexedLineSensSwap.setter
    def indexedLineSensSwap(self, value):
        self.__indexedLineSensSwap = value

    # ---- Output Properties ----
    @property
    def indexedLineActSlider1Forward(self):
        return self.__indexedLineActSlider1Forward

    @indexedLineActSlider1Forward.setter
    def indexedLineActSlider1Forward(self, value):
        self.__indexedLineActSlider1Forward = value

    @property
    def indexedLineActSlider1Backward(self):
        return self.__indexedLineActSlider1Backward

    @indexedLineActSlider1Backward.setter
    def indexedLineActSlider1Backward(self, value):
        self.__indexedLineActSlider1Backward = value

    @property
    def indexedLineActSlider2Forward(self):
        return self.__indexedLineActSlider2Forward

    @indexedLineActSlider2Forward.setter
    def indexedLineActSlider2Forward(self, value):
        self.__indexedLineActSlider2Forward = value

    @property
    def indexedLineActSlider2Backward(self):
        return self.__indexedLineActSlider2Backward

    @indexedLineActSlider2Backward.setter
    def indexedLineActSlider2Backward(self, value):
        self.__indexedLineActSlider2Backward = value

    @property
    def indexedLineActFeedConveyor(self):
        return self.__indexedLineActFeedConveyor

    @indexedLineActFeedConveyor.setter
    def indexedLineActFeedConveyor(self, value):
        self.__indexedLineActFeedConveyor = value

    @property
    def indexedLineActMillingConveyor(self):
        return self.__indexedLineActMillingConveyor

    @indexedLineActMillingConveyor.setter
    def indexedLineActMillingConveyor(self, value):
        self.__indexedLineActMillingConveyor = value

    @property
    def indexedLineActDrillingConveyor(self):
        return self.__indexedLineActDrillingConveyor

    @indexedLineActDrillingConveyor.setter
    def indexedLineActDrillingConveyor(self, value):
        self.__indexedLineActDrillingConveyor = value

    @property
    def indexedLineActSwapConveyor(self):
        return self.__indexedLineActSwapConveyor

    @indexedLineActSwapConveyor.setter
    def indexedLineActSwapConveyor(self, value):
        self.__indexedLineActSwapConveyor = value

    @property
    def indexedLineActMilling(self):
        return self.__indexedLineActMilling

    @indexedLineActMilling.setter
    def indexedLineActMilling(self, value):
        self.__indexedLineActMilling = value

    @property
    def indexedLineActDrilling(self):
        return self.__indexedLineActDrilling

    @indexedLineActDrilling.setter
    def indexedLineActDrilling(self, value):
        self.__indexedLineActDrilling = value

    def sensorStatusString(self) -> str:
        return (f"[{self.indexedLineSensSlider1Front}, "
                f"{self.indexedLineSensSlider1Rear}, "
                f"{self.indexedLineSensSlider2Front}, "
                f"{self.indexedLineSensSlider2Rear}, "
                f"{self.indexedLineSensSlider1}, "
                f"{self.indexedLineSensMilling}, "
                f"{self.indexedLineSensLoading}, "
                f"{self.indexedLineSensDrilling}, "
                f"{self.indexedLineSensSwap}]")

    def actuatorStatusString(self) -> str:
        return (f"[{self.indexedLineActSlider1Forward}, "
                f"{self.indexedLineActSlider1Backward}, "
                f"{self.indexedLineActSlider2Forward}, "
                f"{self.indexedLineActSlider2Backward}, "
                f"{self.indexedLineActFeedConveyor}, "
                f"{self.indexedLineActMillingConveyor}, "
                f"{self.indexedLineActDrillingConveyor}, "
                f"{self.indexedLineActSwapConveyor}, "
                f"{self.indexedLineActMilling}, "
                f"{self.indexedLineActDrilling}]")

    def inputStatus(self) -> Dict[str, Any]:
        return {
            "indexedLineSensSlider1Front": self.indexedLineSensSlider1Front,
            "indexedLineSensSlider1Rear": self.indexedLineSensSlider1Rear,
            "indexedLineSensSlider2Front": self.indexedLineSensSlider2Front,
            "indexedLineSensSlider2Rear": self.indexedLineSensSlider2Rear,
            "indexedLineSensSlider1": self.indexedLineSensSlider1,
            "indexedLineSensMilling": self.indexedLineSensMilling,
            "indexedLineSensLoading": self.indexedLineSensLoading,
            "indexedLineSensDrilling": self.indexedLineSensDrilling,
            "indexedLineSensSwap": self.indexedLineSensSwap,
            }

    def outputStatus(self) -> Dict[str, Any]:
        return {
            "indexedLineActSlider1Forward": self.indexedLineActSlider1Forward,
            "indexedLineActSlider1Backward": self.indexedLineActSlider1Backward,
            "indexedLineActSlider2Forward": self.indexedLineActSlider2Forward,
            "indexedLineActSlider2Backward": self.indexedLineActSlider2Backward,
            "indexedLineActFeedConveyor": self.indexedLineActFeedConveyor,
            "indexedLineActMillingConveyor": self.indexedLineActMillingConveyor,
            "indexedLineActDrillingConveyor": self.indexedLineActDrillingConveyor,
            "indexedLineActSwapConveyor": self.indexedLineActSwapConveyor,
            "indexedLineActMilling": self.indexedLineActMilling,
            "indexedLineActDrilling": self.indexedLineActDrilling,
            }

    def internalStatus(self) -> Dict[str, Any]:
        return {
            "isExecuting": self.isExecuting,
            }

    def stop_CycleStep(self) -> CycleStepResult:
        self.__indexedLineActSlider1Forward = False
        self.__indexedLineActSlider1Backward = False
        self.__indexedLineActSlider2Forward = False
        self.__indexedLineActSlider2Backward = False
        self.__indexedLineActFeedConveyor = False
        self.__indexedLineActMillingConveyor = False
        self.__indexedLineActDrillingConveyor = False
        self.__indexedLineActSwapConveyor = False
        self.__indexedLineActMilling = False
        self.__indexedLineActDrilling = False
        return CycleStepResult.done()

    def goto_config(self, config: IndexedLineConfig = IndexedLineConfig()) -> CycleStepResult:
        res = CycleStepResult.done()

        # Slider1
        self.indexedLineActSlider1Forward = False
        self.indexedLineActSlider1Backward = False
        if config.slider_1_extended and not self.indexedLineSensSlider1Front:
            self.indexedLineActSlider1Forward = True
            res = CycleStepResult(CycleStepResultEnum.MUST_CONTINUE, "extending slider1")
        elif not config.slider_1_extended and not self.indexedLineSensSlider1Rear:
            self.indexedLineActSlider1Backward = True
            res = CycleStepResult(CycleStepResultEnum.MUST_CONTINUE,
                                  "retracting slider1")

        # Slider2
        self.indexedLineActSlider2Forward = False
        self.indexedLineActSlider2Backward = False
        if config.slider_2_extended and not self.indexedLineSensSlider2Front:
            self.indexedLineActSlider2Forward = True
            res = CycleStepResult(CycleStepResultEnum.MUST_CONTINUE,
                                  "extending slider2")
        elif not config.slider_2_extended and not self.indexedLineSensSlider2Rear:
            self.indexedLineActSlider2Backward = True
            res = CycleStepResult(CycleStepResultEnum.MUST_CONTINUE,
                                  "retracting slider2")

        # conveyors
        self.indexedLineActFeedConveyor = config.feed_conveyor
        self.indexedLineActMillingConveyor = config.milling_conveyor
        self.indexedLineActDrillingConveyor = config.drilling_conveyor
        self.indexedLineActSwapConveyor = config.swap_conveyor

        # milling and drilling
        self.indexedLineActMilling = config.milling
        self.indexedLineActDrilling = config.drilling

        return res

    # methods intended for orchestrator
    @protocol_command_function()
    def move_to_mill_Command(self) -> Runner:
        runner = self.create_runner()
        config = IndexedLineConfig()

        # move payload onto slider1
        config.feed_conveyor = True
        runner.then_goto(config, until=lambda: not self.indexedLineSensSlider1 or not self.indexedLineSensMilling, info="moving payload onto slider1")

        # keep moving payload onto slider, since light-barrier is way in front of that
        runner.then_goto(config, and_stay_for=0.5, info="moving payload onto slider1")

        # move payload to milling machine
        config.feed_conveyor = False
        config.milling_conveyor = True
        config.slider_1_extended = True
        runner.then_goto(config, until=lambda: not self.indexedLineSensMilling, info="move payload to milling machine")

        config.feed_conveyor = False
        config.milling_conveyor = False
        config.slider_1_extended = False
        runner.then_goto(config, info="Stopping milling conveyor")

        return runner.run()

    @protocol_command_function()
    def mill_Command(self) -> Runner:
        runner = self.create_runner()
        config = IndexedLineConfig()

        # mill for a few seconds
        config.milling = True
        runner.then_goto(config, and_stay_for=2.0, info="milling")
        config.milling = False
        runner.then_goto(config, info="Stopping mill")
        return runner.run()

    @protocol_command_function()
    def move_to_drill_Command(self) -> Runner:
        runner = self.create_runner()
        config = IndexedLineConfig()

        # move payload to drilling machine
        config.milling_conveyor = True
        config.drilling_conveyor = True
        runner.then_goto(config, until=lambda: not self.indexedLineSensDrilling, info="moving to drilling machine")

        config.milling_conveyor = False
        config.drilling_conveyor = False
        runner.then_goto(config, info="stopping conveyors")

        return runner.run()

    @protocol_command_function()
    def drill_Command(self) -> Runner:
        runner = self.create_runner()
        config = IndexedLineConfig()

        # drill for a few seconds
        config.drilling = True
        runner.then_goto(config, and_stay_for=2.0, info="drilling")

        config.drilling = False
        runner.then_goto(config, info="stopping drill")

        return runner.run()

    @protocol_command_function()
    def move_to_output_Command(self) -> Runner:
        runner = self.create_runner()
        config = IndexedLineConfig()

         # move payload onto slider2
        config.drilling_conveyor = True
        runner.then_goto(config, and_stay_for=1.5, info="moving to slider2")

        # push payload to swap station
        config.drilling_conveyor = False
        config.slider_2_extended = True
        config.swap_conveyor = True
        runner.then_goto(config, until=lambda: not self.indexedLineSensSwap, info="moving to swap station")

        # move payload to end of swap station
        runner.then_goto(config, and_stay_for=1.0, info="Moving to end of swap station")

        # stop station
        config.swap_conveyor = False
        config.slider_2_extended = False
        runner.then_goto(config, info="stopping")
        return runner.run()

    @protocol_command_function()
    def process1_Command(self) -> Runner:
        runner = self.create_runner()
        config = IndexedLineConfig()

        # wait until payload is present
        runner.then_goto(config,
                         until=lambda: not self.indexedLineSensLoading,
                         and_stay_for=0.5,
                         info="waiting for payload")

        # move to mill
        runner.then_run_runner_from(self.move_to_mill_Command, info="moving to mill")

        # mill
        runner.then_run_runner_from(self.mill_Command, info="milling")

        # move to drill
        runner.then_run_runner_from(self.move_to_drill_Command, info="moving to drill")

        # drill
        runner.then_run_runner_from(self.drill_Command, info="drilling")

        # move to output
        runner.then_run_runner_from(self.move_to_output_Command, info="moving to output")

        return runner.run()

    @protocol_command_function(description="Command stopping all engines (incl. compressor).")
    def stop_Command(self) -> Callable[[], CycleStepResult]:
        return self.stop_CycleStep
