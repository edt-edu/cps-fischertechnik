from typing import Dict, Any

from rppmcontroller.rppmcontroller.machine.Axis import AxisType, Axis
from rppmcontroller.rppmcontroller.machine.MovingMachine import MovingMachine
from rppmcontroller.rppmcontroller.machine.RequestedParameter import \
    RequestedParameter


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

    # ------------------ Input Properties ------------------

    @property
    def highbaySensHorizontal(self):
        return self.__highbaySensHorizontal

    @highbaySensHorizontal.setter
    def highbaySensHorizontal(self, value):
        self.__highbaySensHorizontal = value

    @property
    def highbaySensInside(self):
        return self.__highbaySensInside

    @highbaySensInside.setter
    def highbaySensInside(self, value):
        self.__highbaySensInside = value

    @property
    def highbaySensOutside(self):
        return self.__highbaySensOutside

    @highbaySensOutside.setter
    def highbaySensOutside(self, value):
        self.__highbaySensOutside = value

    @property
    def highbaySensVertical(self):
        return self.__highbaySensVertical

    @highbaySensVertical.setter
    def highbaySensVertical(self, value):
        self.__highbaySensVertical = value

    @property
    def highbaySensCantileverFront(self):
        return self.__highbaySensCantileverFront

    @highbaySensCantileverFront.setter
    def highbaySensCantileverFront(self, value):
        self.__highbaySensCantileverFront = value

    @property
    def highbaySensCantileverBack(self):
        return self.__highbaySensCantileverBack

    @highbaySensCantileverBack.setter
    def highbaySensCantileverBack(self, value):
        self.__highbaySensCantileverBack = value

    # ------------------ Output Properties ------------------

    @property
    def highbayActConveyorForward(self):
        return self.__highbayActConveyorForward

    @highbayActConveyorForward.setter
    def highbayActConveyorForward(self, value):
        self.__highbayActConveyorForward = value

    @property
    def highbayActConveyorBackward(self):
        return self.__highbayActConveyorBackward

    @highbayActConveyorBackward.setter
    def highbayActConveyorBackward(self, value):
        self.__highbayActConveyorBackward = value

    @property
    def highbayActHorizontalToRack(self):
        return self.__highbayActHorizontalToRack

    @highbayActHorizontalToRack.setter
    def highbayActHorizontalToRack(self, value):
        self.__highbayActHorizontalToRack = value

    @property
    def highbayActHorizontalToConveyor(self):
        return self.__highbayActHorizontalToConveyor

    @highbayActHorizontalToConveyor.setter
    def highbayActHorizontalToConveyor(self, value):
        self.__highbayActHorizontalToConveyor = value

    @property
    def highbayActDown(self):
        return self.__highbayActDown

    @highbayActDown.setter
    def highbayActDown(self, value):
        self.__highbayActDown = value

    @property
    def highbayActUp(self):
        return self.__highbayActUp

    @highbayActUp.setter
    def highbayActUp(self, value):
        self.__highbayActUp = value

    @property
    def highbayActCantileverForward(self):
        return self.__highbayActCantileverForward

    @highbayActCantileverForward.setter
    def highbayActCantileverForward(self, value):
        self.__highbayActCantileverForward = value

    @property
    def highbayActCantileverBackward(self):
        return self.__highbayActCantileverBackward

    @highbayActCantileverBackward.setter
    def highbayActCantileverBackward(self, value):
        self.__highbayActCantileverBackward = value

    # ------------------ Encoder Properties ------------------

    @property
    def highbaySensHorizontalEncoderCounter(self):
        return self.__highbaySensHorizontalEncoderCounter

    @highbaySensHorizontalEncoderCounter.setter
    def highbaySensHorizontalEncoderCounter(self, value):
        self.__highbaySensHorizontalEncoderCounter = value

    @property
    def highbaySensVerticalEncoderCounter(self):
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
            "highbayActCantileverBackward": self.__highbayActCantileverBackward,
            "highbayActCantileverForward": self.__highbayActCantileverForward,
            "highbayActConveyorBackward": self.__highbayActConveyorBackward,
            "highbayActConveyorForward": self.__highbayActConveyorForward,
            # TODO add remainig actuators
        }
