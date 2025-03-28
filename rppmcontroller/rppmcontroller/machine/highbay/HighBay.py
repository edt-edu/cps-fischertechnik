import logging
from enum import Enum
from typing import Dict, Any, Optional, Union

from rppmcontroller.machine.Axis import AxisType, Axis
from rppmcontroller.machine.AxisConfig import AxisConfig
from rppmcontroller.machine.ConveyorState import ConveyorState, \
    conveyor_state_from_movements
from rppmcontroller.machine.Machine import Machine
from rppmcontroller.machine.RequestedParameter import \
    RequestedParameter
from rppmcontroller.machine.highbay.HighBayConfig import HighBayConfig

PICKUP_DISTANCE = 150
"""how far up we need to move the arm, when picking up an item"""


class Column(Enum):
    CONVEYOR = 0
    RIGHT = 1
    MIDDLE = 2
    LEFT = 3

    def to_counter_goal(self) -> int:
        if self == Column.CONVEYOR:
            return 80
        elif self == Column.RIGHT:
            return 1560
        elif self == Column.MIDDLE:
            return 2700
        elif self == Column.LEFT:
            return 3900
        else:
            raise ValueError(f"no counter goal defined for {self}")


class Row(Enum):
    CONVEYOR = 0
    BOTTOM = 1
    MIDDLE = 2
    TOP = 3

    def to_counter_goal(self) -> int:
        if self == Row.CONVEYOR:
            return 1450
        elif self == Row.BOTTOM:
            return 1700
        elif self == Row.MIDDLE:
            return 900
        elif self == Row.TOP:
            return 200
        else:
            raise ValueError(f"no counter goal defined for {self}")


class State(Enum):
    MOVE_TO_CONVEYOR = 0
    WAIT_AT_CONVEYOR = 1
    PICKUP = 2
    MOVE_TO_RACK = 3
    DROP_OFF = 4


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

        # helper variables
        self.previous_isExecuting_log = None
        self.reset_rpi_encoder_counters = False
        self.next_config = None
        self.__state = None

    def __isExecuting(self) -> bool:
        return (self.__highbayActUp or
                self.__highbayActDown or
                self.__highbayActConveyorForward or
                self.__highbayActConveyorBackward or
                self.__highbayActHorizontalToConveyor or
                self.__highbayActHorizontalToRack or
                self.__highbayActCantileverForward or
                self.__highbayActCantileverBackward)

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

    @property
    def state(self) -> State:
        return self.__state

    @state.setter
    def state(self, state: Optional[State]) -> None:
        self.__state = state
        logging.debug(f"state: {state}")

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
        logging.debug(f"going to {config}")

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

        class ArmMovement(Enum):
            IDLE = 0
            """The arm doesn't need to move"""
            MINOR = 1
            """The arm needs to move a little bit, for picking up or dropping of an item"""
            MAYOR = 2
            """The arm needs to move alot and the cantilever should be retracted for that"""

        arm_movement = ArmMovement.IDLE

        # vertical axis
        self.__axisVertical.update(self.highbaySensVertical,
                                   self.highbaySensVerticalEncoderCounter)
        if not self.__axisVertical.gotoAxisConfig(config.vertical_axis_config):
            # only allow small vertical movements for pickup
            distance_to_move = self.__axisVertical.counterValueCurrent - (
                config.vertical_axis_config.counter_goal or 0)
            if abs(
                distance_to_move) > PICKUP_DISTANCE + self.__axisVertical.tolerance:
                arm_movement = ArmMovement.MAYOR
            else:
                arm_movement = ArmMovement.MINOR

        # horizontal axis
        self.__axisHorizontal.update(self.highbaySensHorizontal,
                                     self.highbaySensHorizontalEncoderCounter)
        if not self.__axisHorizontal.gotoAxisConfig(
            config.horizontal_axis_config):
            arm_movement = ArmMovement.MAYOR

        logging.debug(f"arm_movement: {arm_movement}")
        if arm_movement is ArmMovement.MAYOR and not self.highbaySensCantileverBack:
            self.highbayActCantileverForward = False
            self.highbayActCantileverBackward = True
            self.highbayActHorizontalToRack = False
            self.highbayActHorizontalToConveyor = False
            self.highbayActUp = False
            self.highbayActDown = False
        elif arm_movement is not ArmMovement.IDLE:
            self.highbayActCantileverForward = False
            self.highbayActCantileverBackward = False
            self.highbayActHorizontalToRack = self.__axisHorizontal.outputplus
            self.highbayActHorizontalToConveyor = self.__axisHorizontal.outputminus
            self.highbayActUp = self.__axisVertical.outputminus
            self.highbayActDown = self.__axisVertical.outputplus
        elif config.cantilever_extended and not self.highbaySensCantileverFront:
            self.highbayActCantileverForward = True
            self.highbayActCantileverBackward = False
            self.highbayActHorizontalToRack = False
            self.highbayActHorizontalToConveyor = False
            self.highbayActUp = False
            self.highbayActDown = False
        elif not config.cantilever_extended and not self.highbaySensCantileverBack:
            self.highbayActCantileverForward = False
            self.highbayActCantileverBackward = True
            self.highbayActHorizontalToRack = False
            self.highbayActHorizontalToConveyor = False
            self.highbayActUp = False
            self.highbayActDown = False
        else:
            self.highbayActCantileverForward = False
            self.highbayActCantileverBackward = False
            self.highbayActHorizontalToRack = False
            self.highbayActHorizontalToConveyor = False
            self.highbayActUp = False
            self.highbayActDown = False
            return True

        return False

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

    def edit_and_goto_config(self, config_editor):
        """
        Edits the current config and goes to it.
        :param config_editor: A consumer of the current config, which edits it
        :return: A lambda with the goto_config call, intended for the controller
        """
        config = self.get_current_config()
        config_editor(config)
        runnable = lambda: self.goto_config(config)
        runnable()
        return runnable

    def create_next_config(self) -> HighBayConfig:
        """
        Sets the next_config to the current config and returns the
        config object for editing
        :return: The new next_config
        """
        self.next_config = self.get_current_config()
        return self.next_config

    def goto_next_config(self):
        """
        Goes to the next_config and returns a lambda going to that config
        :return:
        """
        runnable = lambda: self.goto_config(self.next_config)
        runnable()
        return runnable

    # methods intended for orchestrator
    # those methods need to return a lambda pointing to themselves

    def setup(self):
        """
        Set up the highbay and calibrate the counters.
        :return: A lambda rerunning this function
        """
        # going to the default config is the setup
        setup_finished = self.goto_config()
        if setup_finished:
            # setup is finished
            self.reset_rpi_encoder_counters = True
        return lambda: self.setup()

    def conveyor_forward(self):
        self.create_next_config().conveyor_state = ConveyorState.FORWARD
        return self.goto_next_config()

    def conveyor_backward(self):
        self.create_next_config().conveyor_state = ConveyorState.BACKWARD
        return self.goto_next_config()

    def conveyor_stop(self):
        self.create_next_config().conveyor_state = ConveyorState.IDLE
        return self.goto_next_config()

    def cantilever_forward(self):
        self.create_next_config().cantilever_extended = True
        return self.goto_next_config()

    def cantilever_backward(self):
        self.create_next_config().cantilever_extended = False
        return self.goto_next_config()

    def horizontal_to(self, counter_goal: int):
        self.create_next_config().horizontal_axis_config = AxisConfig.to_counter_goal(
            counter_goal)
        return self.goto_next_config()

    def vertical_to(self, counter_goal: int):
        self.create_next_config().vertical_axis_config = AxisConfig.to_counter_goal(
            counter_goal)
        return self.goto_next_config()

    def goto_column(self, column: Union[Column, int]):
        if isinstance(column, int):
            column = Column(column)

        return self.horizontal_to(column.to_counter_goal())

    def goto_row(self, row: Union[Row, int]):
        if isinstance(row, int):
            row = Row(row)

        return self.vertical_to(row.to_counter_goal())

    def store_to(self, row: Union[Row, int], column: Union[Column, int]):
        if isinstance(row, int):
            row = Row(row)
        if isinstance(column, int):
            column = Column(column)

        # logging.debug(f"Storing item at {row}, {column}...")

        if self.state is None:
            self.state = State.MOVE_TO_CONVEYOR

        # define the reference config for the next few steps
        horizontal_axis_config = AxisConfig.to_counter_goal(
            Column.CONVEYOR.to_counter_goal())
        vertical_axis_config = AxisConfig.to_counter_goal(
            Row.CONVEYOR.to_counter_goal())
        config = HighBayConfig(horizontal_axis_config, vertical_axis_config,
                               True)

        if self.state is State.MOVE_TO_CONVEYOR:
            if self.goto_config(config):
                self.state = State.WAIT_AT_CONVEYOR

        config.conveyor_state = ConveyorState.BACKWARD

        if self.state is State.WAIT_AT_CONVEYOR:
            self.goto_config(config)
            if not self.highbaySensInside:
                self.state = State.PICKUP

        config.conveyor_state = ConveyorState.IDLE
        config.vertical_axis_config.counter_goal -= PICKUP_DISTANCE

        if self.state is State.PICKUP:
            if self.goto_config(config):
                self.state = State.MOVE_TO_RACK

        horizontal_axis_config = AxisConfig.to_counter_goal(
            column.to_counter_goal())
        vertical_axis_config = AxisConfig.to_counter_goal(
            row.to_counter_goal() - PICKUP_DISTANCE)
        config = HighBayConfig(horizontal_axis_config, vertical_axis_config,
                               True)

        if self.state is State.MOVE_TO_RACK:
            if self.goto_config(config):
                self.state = State.DROP_OFF

        config.vertical_axis_config.counter_goal += PICKUP_DISTANCE

        if self.state is State.DROP_OFF:
            if self.goto_config(config):
                self.state = None
                self.stop()

        return lambda: self.store_to(row, column)

    def pickup_from(self, row: Union[Row, int], column: Union[Column, int]):
        if isinstance(row, int):
            row = Row(row)
        if isinstance(column, int):
            column = Column(column)

        if self.state is None:
            self.state = State.MOVE_TO_RACK

        # define the reference config for the next few steps
        horizontal_axis_config = AxisConfig.to_counter_goal(
            column.to_counter_goal())
        vertical_axis_config = AxisConfig.to_counter_goal(
            row.to_counter_goal())
        config = HighBayConfig(horizontal_axis_config, vertical_axis_config,
                               True)

        if self.state is State.MOVE_TO_RACK:
            if self.goto_config(config):
                self.state = State.PICKUP

        config.vertical_axis_config.counter_goal -= PICKUP_DISTANCE

        if self.state is State.PICKUP:
            if self.goto_config(config):
                self.state = State.MOVE_TO_CONVEYOR

        horizontal_axis_config = AxisConfig.to_counter_goal(
            Column.CONVEYOR.to_counter_goal())
        vertical_axis_config = AxisConfig.to_counter_goal(
            Row.CONVEYOR.to_counter_goal() - PICKUP_DISTANCE)
        config = HighBayConfig(horizontal_axis_config,
                               vertical_axis_config,
                               True)

        if self.state is State.MOVE_TO_CONVEYOR:
            if self.goto_config(config):
                self.state = State.DROP_OFF

        config.vertical_axis_config.counter_goal += PICKUP_DISTANCE
        config.conveyor_state = ConveyorState.FORWARD

        if self.state is State.DROP_OFF:
            if self.goto_config(config):
                self.state = State.WAIT_AT_CONVEYOR

        if self.state is State.WAIT_AT_CONVEYOR:
            self.goto_config(config)
            if not self.highbaySensOutside:
                self.state = None
                self.stop()

        return lambda: self.pickup_from(row, column)
