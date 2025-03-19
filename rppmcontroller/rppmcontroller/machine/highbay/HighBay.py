import logging
from typing import Dict, Any, Optional

from rppmcontroller.machine.Axis import AxisType, Axis
from rppmcontroller.machine.AxisConfig import AxisConfig
from rppmcontroller.machine.ConveyorState import ConveyorState, \
    conveyor_state_from_movements
from rppmcontroller.machine.Machine import Machine
from rppmcontroller.machine.RequestedParameter import \
    RequestedParameter
from rppmcontroller.machine.highbay.HighBayConfig import HighBayConfig


class HighBay(Machine):
    def __init__(self, id1):
        #  inputs
        self.__highbaySensHorizontal = False
        self.__highbaySensInside = True
        self.__highbaySensOutside = True
        self.__highbaySensVertical = False
        self.__highbaySensCantileverFront = False
        self.__highbaySensCantileverBack = False

        #  outputs
        self.__highbayActConveyorForward = False
        self.__highbayActConveyorBackward = False
        self.__highbayActHorizontalToRack = False
        self.__highbayActHorizontalToConveyor = False
        self.__highbayActDown = False
        self.__highbayActUp = False
        self.__highbayActCantileverForward = False
        self.__highbayActCantileverBackward = False

        #  encoder
        self.__highbaySensHorizontalEncoderCounter = 0
        self.__highbaySensVerticalEncoderCounter = 0
        self.__axisHorizontal = Axis(AxisType.Encoder, 20)
        self.__axisVertical = Axis(AxisType.Encoder, 20)
        dictMap = {
            RequestedParameter.REFERENCESWITCHHORIZONTALAXIS: self.__highbaySensHorizontal,
            RequestedParameter.LIGHTBARRIERINSIDE: self.__highbaySensInside,
            RequestedParameter.LIGHTBARRIEROUTSIDE: self.__highbaySensOutside,
            RequestedParameter.REFERENCESWITCHVERTICALAXIS: self.__highbaySensVertical,
            RequestedParameter.HORIZONTALAXISSTEP: self.__axisHorizontal.counterValueCurrent,
            RequestedParameter.VERTICALAXISSTEP: self.__axisVertical.counterValueCurrent,
            RequestedParameter.REFERENCESWITCHCANTILEVERFRONT: self.__highbaySensCantileverFront,
            RequestedParameter.REFERENCESWITCHCANTILEVERBACK: self.__highbaySensCantileverBack,
            RequestedParameter.MOTORCONVEYORBELTFORWARD: self.__highbayActConveyorForward,
            RequestedParameter.MOTORCONVEYORBELTBACKWARD: self.__highbayActConveyorBackward,
            RequestedParameter.MOTORHORIZONTALAXISFORWARD: self.__highbayActHorizontalToRack,
            RequestedParameter.MOTORHORIZONTALAXISBACKWARD: self.__highbayActHorizontalToConveyor,
            RequestedParameter.MOTORVERTICALAXISUPWARD: self.__highbayActUp,
            RequestedParameter.MOTORVERTICALAXISDOWNWARD: self.__highbayActDown,
            RequestedParameter.MOTORCANTILEVERFORWARD: self.__highbayActCantileverForward,
            RequestedParameter.MOTORCANTILEVERBACKWARD: self.__highbayActCantileverBackward,
        }
        super().__init__(id1, dictMap)

        self.previous_isExecuting_log = None

    def __isExecuting(self) -> bool:
        return (self.__highbayActUp or
                self.__highbayActDown or
                self.__highbayActConveyorForward or
                self.__highbayActConveyorBackward or
                self.__highbayActHorizontalToConveyor or
                self.__highbayActHorizontalToRack or
                self.__highbayActCantileverForward or
                self.__highbayActCantileverBackward or
                self.nbMinimumRequiredExecutionCycles > 0)

    @property
    def isExecuting(self) -> bool:
        res = self.__isExecuting()

        # log isExecuting and debug info only if message has changed
        isExecuting_log = f'isExecuting({self.id})={res} | Sensors={self.sensorStatusString()} | Actuators= {self.actuatorStatusString()}'
        if isExecuting_log != self.previous_isExecuting_log:
            logging.debug(isExecuting_log)
            self.previous_isExecuting_log = isExecuting_log

        return res

    # ------------------ Input Properties ------------------

    @property
    def highbaySensHorizontal(self) -> bool:
        return self.__highbaySensHorizontal

    @highbaySensHorizontal.setter
    def highbaySensHorizontal(self, value: bool) -> None:
        self.__highbaySensHorizontal = value

    @property
    def highbaySensInside(self) -> bool:
        return self.__highbaySensInside

    @highbaySensInside.setter
    def highbaySensInside(self, value: bool) -> None:
        self.__highbaySensInside = value

    @property
    def highbaySensOutside(self) -> bool:
        return self.__highbaySensOutside

    @highbaySensOutside.setter
    def highbaySensOutside(self, value: bool) -> None:
        self.__highbaySensOutside = value

    @property
    def highbaySensVertical(self) -> bool:
        return self.__highbaySensVertical

    @highbaySensVertical.setter
    def highbaySensVertical(self, value: bool) -> None:
        self.__highbaySensVertical = value

    @property
    def highbaySensCantileverFront(self) -> bool:
        return self.__highbaySensCantileverFront

    @highbaySensCantileverFront.setter
    def highbaySensCantileverFront(self, value: bool) -> None:
        self.__highbaySensCantileverFront = value

    @property
    def highbaySensCantileverBack(self) -> bool:
        return self.__highbaySensCantileverBack

    @highbaySensCantileverBack.setter
    def highbaySensCantileverBack(self, value: bool) -> None:
        self.__highbaySensCantileverBack = value

    # ------------------ Output Properties ------------------

    @property
    def highbayActConveyorForward(self) -> bool:
        return self.__highbayActConveyorForward

    @highbayActConveyorForward.setter
    def highbayActConveyorForward(self, value: bool) -> None:
        self.__highbayActConveyorForward = value

    @property
    def highbayActConveyorBackward(self) -> bool:
        return self.__highbayActConveyorBackward

    @highbayActConveyorBackward.setter
    def highbayActConveyorBackward(self, value: bool) -> None:
        self.__highbayActConveyorBackward = value

    @property
    def highbayActHorizontalToRack(self) -> bool:
        return self.__highbayActHorizontalToRack

    @highbayActHorizontalToRack.setter
    def highbayActHorizontalToRack(self, value: bool) -> None:
        self.__highbayActHorizontalToRack = value

    @property
    def highbayActHorizontalToConveyor(self) -> bool:
        return self.__highbayActHorizontalToConveyor

    @highbayActHorizontalToConveyor.setter
    def highbayActHorizontalToConveyor(self, value: bool) -> None:
        self.__highbayActHorizontalToConveyor = value

    @property
    def highbayActDown(self) -> bool:
        return self.__highbayActDown

    @highbayActDown.setter
    def highbayActDown(self, value: bool) -> None:
        self.__highbayActDown = value

    @property
    def highbayActUp(self) -> bool:
        return self.__highbayActUp

    @highbayActUp.setter
    def highbayActUp(self, value: bool) -> None:
        self.__highbayActUp = value

    @property
    def highbayActCantileverForward(self) -> bool:
        return self.__highbayActCantileverForward

    @highbayActCantileverForward.setter
    def highbayActCantileverForward(self, value: bool) -> None:
        self.__highbayActCantileverForward = value

    @property
    def highbayActCantileverBackward(self) -> bool:
        return self.__highbayActCantileverBackward

    @highbayActCantileverBackward.setter
    def highbayActCantileverBackward(self, value: bool) -> None:
        self.__highbayActCantileverBackward = value

    # ------------------ Encoder Properties ------------------

    @property
    def highbaySensHorizontalEncoderCounter(self) -> int:
        return self.__highbaySensHorizontalEncoderCounter

    @highbaySensHorizontalEncoderCounter.setter
    def highbaySensHorizontalEncoderCounter(self, value: int) -> None:
        self.__highbaySensHorizontalEncoderCounter = value

    @property
    def highbaySensVerticalEncoderCounter(self) -> int:
        return self.__highbaySensVerticalEncoderCounter

    @highbaySensVerticalEncoderCounter.setter
    def highbaySensVerticalEncoderCounter(self, value: int) -> None:
        self.__highbaySensVerticalEncoderCounter = value

    def sensorStatusString(self) -> str:
        return f"[{self.highbaySensHorizontalEncoderCounter}, {self.highbaySensVerticalEncoderCounter}], [{self.highbaySensCantileverBack}, {self.highbaySensCantileverFront}, {self.highbaySensHorizontal}, {self.highbaySensInside}, {self.highbaySensOutside}, {self.highbaySensVertical}]"

    def actuatorStatusString(self) -> str:
        return f"[{self.highbayActUp}, {self.highbayActDown}, {self.highbayActHorizontalToRack}, {self.highbayActHorizontalToConveyor}], [{self.highbayActCantileverBackward}, {self.highbayActCantileverForward}, {self.highbayActConveyorBackward}, {self.highbayActConveyorForward}]"

    def inputStatus(self) -> Dict[str, Any]:
        return {
            # TODO better adjust the names, I just made them up
            "highbaySensCantileverBack": self.__highbaySensCantileverBack,
            "highbaySensCantileverFront": self.__highbaySensCantileverFront,
            "highbaySensHorizontal": self.__highbaySensHorizontal,
            "highbaySensHorizontalEncoderCounter": self.__highbaySensHorizontalEncoderCounter,
            "highbaySensInside": self.__highbaySensInside,
            "highbaySensOutside": self.__highbaySensOutside,
            "highbaySensVertical": self.__highbaySensVertical,
            "highbaySensVerticalEncoderCounter": self.__highbaySensVerticalEncoderCounter,
        }

    def outputStatus(self) -> Dict[str, Any]:
        return {
            # TODO better adjust the names, I just made them up
            "highbayActCantileverBackward": self.__highbayActCantileverBackward,
            "highbayActCantileverForward": self.__highbayActCantileverForward,
            "highbayActConveyorBackward": self.__highbayActConveyorBackward,
            "highbayActConveyorForward": self.__highbayActConveyorForward,
            "highbayActHorizontalToRack": self.__highbayActHorizontalToRack,
            "highbayActHorizontalToConveyor": self.__highbayActHorizontalToConveyor,
            "highbayActDown": self.__highbayActDown,
            "highbayActUp": self.__highbayActUp,
        }

    def setup(self) -> bool:
        """
        Set up the highbay and calibrate the counters.
        :return: True if the setup has been completed, False if it is still running
        """
        return self.goto_config()

    def get_current_config(self) -> HighBayConfig:
        """
        Get the config describing the state in which the machine currently resides
        :return: The current config
        """
        return HighBayConfig(AxisConfig(self.highbaySensHorizontal,
                                        self.highbaySensHorizontalEncoderCounter),
                             AxisConfig(self.highbaySensVertical,
                                        self.highbaySensVerticalEncoderCounter),
                             not self.highbaySensCantileverBack,
                             conveyor_state_from_movements(
                                 self.highbayActConveyorForward,
                                 self.highbayActConveyorBackward))

    def goto_config(self,
                    config: Optional[HighBayConfig] = HighBayConfig()) -> bool:
        """
        Transfer the machine into another configuration
        :param config: The new configuration to transfer the machine to
        :return: True if the machine has reached the configuration, otherwise false
        """
        logging.debug(f"[HighBay] going to configuration {config}")

        target_config_reached = True

        # check whether arm needs to move
        arm_needs_to_move = False

        self.__axisHorizontal.update(self.highbaySensHorizontal,
                                     self.highbaySensHorizontalEncoderCounter)
        if not self.__axisHorizontal.gotoAxisConfig(
            config.horizontal_axis_config):
            arm_needs_to_move = True

        self.__axisVertical.update(self.highbaySensVertical,
                                   self.highbaySensVerticalEncoderCounter)
        if not self.__axisVertical.gotoAxisConfig(config.vertical_axis_config):
            arm_needs_to_move = True

        if arm_needs_to_move:
            target_config_reached = False

        # move cantilever; make sure it is retracted if the arm needs to move
        cantilever_is_retracted = self.highbaySensCantileverBack
        cantilever_is_extended = self.highbaySensCantileverFront
        cantilever_needs_to_be_retracted = (not config.cantilever_extended or arm_needs_to_move) and not cantilever_is_retracted
        cantilever_needs_to_be_extended = not cantilever_needs_to_be_retracted and (config.cantilever_extended and not cantilever_is_extended)
        cantilever_needs_to_move = cantilever_needs_to_be_retracted or cantilever_needs_to_be_extended

        if cantilever_needs_to_move:
            target_config_reached = False

            if cantilever_needs_to_be_retracted:
                self.highbayActCantileverBackward = True
            else:
                self.highbayActCantileverForward = True

            # make sure we are not moving the arm
            self.highbayActHorizontalToRack = False
            self.highbayActHorizontalToConveyor = False
            self.highbayActUp = False
            self.highbayActDown = False
        else:
            self.highbayActCantileverBackward = False
            self.highbayActCantileverForward = False

            # cantilever is in position; we may move the arm now
            self.highbayActHorizontalToRack = self.__axisHorizontal.outputminus
            self.highbayActHorizontalToConveyor = self.__axisHorizontal.outputplus
            self.highbayActUp = self.__axisVertical.outputminus
            self.highbayActDown = self.__axisVertical.outputplus

        # update conveyor belt state
        if config.conveyor_state == ConveyorState.IDLE:
            self.highbayActConveyorForward = False
            self.highbayActConveyorBackward = False
        elif config.conveyor_state == ConveyorState.FORWARD:
            self.highbayActConveyorForward = True
            self.highbayActConveyorBackward = False
        elif config.conveyor_state == ConveyorState.BACKWARD:
            self.highbayActConveyorForward = False
            self.highbayActConveyorBackward = True

        logging.debug(f"Config has been reached: {target_config_reached}")
        return target_config_reached

    def internalStatus(self) -> Dict[str, Any]:
        return {
            "isExecuting": self.__isExecuting()
        }

    def stop(self):
        self.highbayActUp = False
        self.highbayActDown = False
        self.highbayActHorizontalToRack = False
        self.highbayActHorizontalToConveyor = False
        self.highbayActConveyorForward = False
        self.highbayActConveyorBackward = False
        self.highbayActCantileverForward = False
        self.highbayActCantileverBackward = False
