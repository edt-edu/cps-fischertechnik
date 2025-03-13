import logging
from typing import Dict, Any, Optional

from rppmcontroller.machine.Axis import AxisType, Axis
from rppmcontroller.machine.MovingMachine import MovingMachine
from rppmcontroller.machine.Position import Position
from rppmcontroller.machine.RequestedParameter import \
    RequestedParameter
from rppmcontroller.machine.highbay.HighBayConfig import HighBayConfig


class HighBay(MovingMachine):
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
                self.nbMinimumRequiredExecutionCycles > 0 or
                self.hasRemainingMove())

    @property
    def isExecuting(self) -> bool:
        res = self.__isExecuting()

        # log isExecuting and debug info only if message has changed
        isExecuting_log = f'isExecuting({self.id})={res} | pc={self.pc}/nbMove={self.nbMove()} | Sensors={self.sensorStatusString()} | Actuators= {self.actuatorStatusString()}'
        if isExecuting_log != self.previous_isExecuting_log:
            logging.debug(isExecuting_log)
            self.previous_isExecuting_log = isExecuting_log

        return res

    # ------------------ Input Properties ------------------

    @property
    def highbaySensHorizontal(self) -> bool:
        return self.__highbaySensHorizontal

    @highbaySensHorizontal.setter
    def highbaySensHorizontal(self, value):
        self.__highbaySensHorizontal = value

    @property
    def highbaySensInside(self) -> bool:
        return self.__highbaySensInside

    @highbaySensInside.setter
    def highbaySensInside(self, value):
        self.__highbaySensInside = value

    @property
    def highbaySensOutside(self) -> bool:
        return self.__highbaySensOutside

    @highbaySensOutside.setter
    def highbaySensOutside(self, value):
        self.__highbaySensOutside = value

    @property
    def highbaySensVertical(self) -> bool:
        return self.__highbaySensVertical

    @highbaySensVertical.setter
    def highbaySensVertical(self, value):
        self.__highbaySensVertical = value

    @property
    def highbaySensCantileverFront(self) -> bool:
        return self.__highbaySensCantileverFront

    @highbaySensCantileverFront.setter
    def highbaySensCantileverFront(self, value):
        self.__highbaySensCantileverFront = value

    @property
    def highbaySensCantileverBack(self) -> bool:
        return self.__highbaySensCantileverBack

    @highbaySensCantileverBack.setter
    def highbaySensCantileverBack(self, value):
        self.__highbaySensCantileverBack = value

    # ------------------ Output Properties ------------------

    @property
    def highbayActConveyorForward(self) -> bool:
        return self.__highbayActConveyorForward

    @highbayActConveyorForward.setter
    def highbayActConveyorForward(self, value):
        self.__highbayActConveyorForward = value

    @property
    def highbayActConveyorBackward(self) -> bool:
        return self.__highbayActConveyorBackward

    @highbayActConveyorBackward.setter
    def highbayActConveyorBackward(self, value):
        self.__highbayActConveyorBackward = value

    @property
    def highbayActHorizontalToRack(self) -> bool:
        return self.__highbayActHorizontalToRack

    @highbayActHorizontalToRack.setter
    def highbayActHorizontalToRack(self, value):
        self.__highbayActHorizontalToRack = value

    @property
    def highbayActHorizontalToConveyor(self) -> bool:
        return self.__highbayActHorizontalToConveyor

    @highbayActHorizontalToConveyor.setter
    def highbayActHorizontalToConveyor(self, value):
        self.__highbayActHorizontalToConveyor = value

    @property
    def highbayActDown(self) -> bool:
        return self.__highbayActDown

    @highbayActDown.setter
    def highbayActDown(self, value):
        self.__highbayActDown = value

    @property
    def highbayActUp(self) -> bool:
        return self.__highbayActUp

    @highbayActUp.setter
    def highbayActUp(self, value):
        self.__highbayActUp = value

    @property
    def highbayActCantileverForward(self) -> bool:
        return self.__highbayActCantileverForward

    @highbayActCantileverForward.setter
    def highbayActCantileverForward(self, value):
        self.__highbayActCantileverForward = value

    @property
    def highbayActCantileverBackward(self) -> bool:
        return self.__highbayActCantileverBackward

    @highbayActCantileverBackward.setter
    def highbayActCantileverBackward(self, value):
        self.__highbayActCantileverBackward = value

    # ------------------ Encoder Properties ------------------

    @property
    def highbaySensHorizontalEncoderCounter(self) -> int:
        return self.__highbaySensHorizontalEncoderCounter

    @highbaySensHorizontalEncoderCounter.setter
    def highbaySensHorizontalEncoderCounter(self, value):
        self.__highbaySensHorizontalEncoderCounter = value

    @property
    def highbaySensVerticalEncoderCounter(self) -> int:
        return self.__highbaySensVerticalEncoderCounter

    @highbaySensVerticalEncoderCounter.setter
    def highbaySensVerticalEncoderCounter(self, value):
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

    def generateTransferMoveList(self, numPickup: Position,
                                 numPlace: Position) -> list:
        pass # TODO

    def setup(self) -> bool:
        # retract cantilever, move up and towards conveyor

        # make sure all other engines are stopped
        self.highbayActHorizontalToRack = False
        self.highbayActCantileverForward = False
        self.highbayActDown = False
        self.highbayActConveyorForward = False
        self.highbayActConveyorBackward = False

        # make sure cantilever is retracted before moving around
        if self.highbaySensCantileverBack:
            self.highbayActCantileverBackward = False
        else:
            self.highbayActCantileverBackward = True
            return False

        moving = False

        # move up
        if self.highbaySensVertical:
            self.highbayActUp = False
        else:
            self.highbayActUp = True
            moving = True

        # move towards conveyor
        if self.highbaySensHorizontal:
            self.highbayActHorizontalToConveyor = False
        else:
            self.highbayActHorizontalToConveyor = True
            moving = True

        self.setupFinished = not moving
        self.setupFinishedHelper = self.setupFinished

        # no idea why other implementations return a lambda here but documentations states that this method returns a bool
        return self.setupFinished

    def gotoconfig(self, config: Optional[HighBayConfig] = HighBayConfig()) -> bool:

        self.__axisHorizontal.update(self.highbaySensHorizontal, self.highbaySensHorizontalEncoderCounter)


        pass

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
