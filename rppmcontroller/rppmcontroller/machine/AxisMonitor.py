from __future__ import annotations

import logging
from dataclasses import dataclass
from enum import Enum
from math import sqrt
from typing import TypeVar

from rppmcontroller.machine.Axis import Axis
from rppmcontroller.machine.MachineParameters import AxisMonitorParameters


@dataclass
class NamedAxisMonitor:
    """
    A wrapper around an AxisMonitor that also wraps the monitored Axis and
    offers functionality for debugging and decision-making on action-taking.
    """

    monitor: AxisMonitor
    """The AxisMonitor"""
    parameters: AxisMonitorParameters
    """The configuration of the AxisMonitor"""
    axis: Axis
    """The monitored Axis"""
    axis_name: str
    """The name of the Axis, for debugging purposes"""

    @staticmethod
    def new(parameters: AxisMonitorParameters,
            axis: Axis,
            axis_name: str) -> NamedAxisMonitor:
        """
        Create a new AxisContainingMonitor
        :param parameters: Configuration for the AxisMonitor
        :param axis: The Axis to monitor
        :param axis_name: The name of the Axis, for debugging purposes
        :return: The created AxisContainingMonitor
        """
        return NamedAxisMonitor(AxisMonitor.new(parameters),
                                parameters,
                                axis,
                                axis_name)

    def record_and_action_is_required(self, current_pwm_value: int) -> bool:
        """
        Records the current axis value for the given pwm value and checks
        whether the controller should take action in response to the recorded
        deviations.
        :param current_pwm_value: The pwm value to record for the axis
        :return: Whether action should be taken.
        """
        self.record(current_pwm_value)
        return self.is_action_required()

    def is_action_required(self) -> bool:
        """
        Determines whether the total penalty for all recorded deviations is
        larger or equal to the penalty threshold.
        :return: `True` if the controller should take action in response to the
            recorded deviations
        """
        return (self.calculate_total_penalty() >=
                self.parameters.penalty_threshold)

    def calculate_total_penalty(self) -> int:
        """
        Calculates the total penalty for all recorded deviations.
        :return: The total penalty
        """
        penalty = 0
        for deviation in self.monitor.get_recorded_deviations():
            if deviation == Deviation.SMALL:
                penalty += self.parameters.minor_deviation_penalty
            elif deviation == Deviation.HIGH:
                penalty += self.parameters.major_deviation_penalty
        logging.debug(f"Total penalty: {penalty}")
        return penalty

    def record(self, current_pwm_value) -> Deviation:
        """
        Records the current counter-value of the axis for the given pwm value.
        For more details see AxisMonitor.record()
        :param current_pwm_value: The current pwm value with which the axis
            is moved
        :return: The recorded deviation
        """
        deviation = self.monitor.record(self.axis.counterValueCurrent,
                                        current_pwm_value)
        if deviation != Deviation.NONE:
            logging.debug(f"Recorded {deviation} on {self.axis_name}")
        return deviation


class AxisMonitor:
    """
    Monitors the movement of an Axis over time
    """

    @staticmethod
    def new(parameters: AxisMonitorParameters) -> AxisMonitor:
        """
        Create a new AxisMonitor from AxisMonitorParameters and an Axis
        :param parameters: The AxisMonitorParameters to configure the
            AxisMonitor
        :return: The created AxisMonitor
        """
        return AxisMonitor(parameters.cycles_to_monitor,
                           parameters.required_cycles_to_average,
                           parameters.movement_tolerance)

    def __init__(self,
                 cycles_to_monitor: int,
                 required_cycles_to_average: int,
                 movement_tolerance: int):
        """
        Create a new AxisMonitor
        :param cycles_to_monitor: Number of cycles for that will be stored,
            whether axis movement was successful. Must be at least 1.
        :param required_cycles_to_average: Number of cycles of the recorded
            cycles, which must contain movement data for the current pwm
            value,
            that are required to create an average movement distance.
            If not enough cycles provide data, then no small deviations can be
            detected, only high ones. This value must not exceed the number of
            cycles to monitor.
        :param movement_tolerance: Maximum deviation of a target value for
        which
            the axis will not be moving, or in other words: Movement below
            this threshold will be treated like the axis didn't move at all.
        """

        self.__buffer: list[CycleData] = []
        """Buffer containing the last cycle data"""
        self.__buffer_head = 0
        """Index of the position in the buffer where the next data will be
        written to"""
        self.__max_buffer_len = cycles_to_monitor
        """Maximum length of the buffer, must be at least 1"""
        self.__required_cycles_to_average = required_cycles_to_average
        """Number of cycles of the recorded cycles which must contain movement
        data for the current pwm value that are required to create an average
        movement distance. If not enough cycles provide data, then no small
        deviations can be detected, only high ones. This value must not exceed
        the number of cycles to monitor."""
        self.__movement_tolerance = movement_tolerance
        """Movement below this threshold will be
        treated like the axis didn't move at all."""

        if cycles_to_monitor < 1:
            raise ValueError("At least one cycle must be monitored")

        if required_cycles_to_average > cycles_to_monitor:
            raise ValueError("Required cycles to average must not exceed the "
                             "number of cycles to monitor")

    def record(self,
               current_counter_value: int,
               current_pwm_value: int) -> Deviation:
        """
        Records the current state of the axis.

        While recording, it will also be determined whether the recorded value
        deviates from the expected axis movement. The recorded deviation will
        be returned but can also be accessed via the
        `get_recorded_deviations()` method.
        :param current_counter_value: The current value of the axis encoder
            counter.
        :param current_pwm_value: The current pwm value with which the axis
            will move now. Set to 0 to indicate that the axis will not move.
        :return: The recorded deviation
        """
        direct_previous_data = self.__get_last_pushed_data()
        deviation = None
        moved_distance = None

        if direct_previous_data is not None:
            moved_distance = abs(current_counter_value -
                                 direct_previous_data.counter_value)
            deviation = self.__calculate_deviation(moved_distance,
                                                   current_pwm_value) \
                if direct_previous_data.current_pwm_value > 0 else None
            if deviation is not None and deviation is not Deviation.NONE:
                logging.debug(f"got {deviation} for distance of "
                              f"{moved_distance}")

        deviation = deviation if deviation is not None else Deviation.NONE

        new_data = CycleData(current_counter_value,
                             current_pwm_value,
                             moved_distance,
                             deviation)
        self.__push_to_buffer(new_data)
        return deviation

    def get_recorded_deviations(self) -> list[Deviation]:
        """
        Gets all recorded deviations of the monitored cycles.

        Evaluation example:
        .. code-block:: python

            if monitor.get_recorded_deviations().count(Deviation.HIGH) >= 3:
                abort()

        :return: All recorded deviations of the monitored cycles
        """
        return [data.deviation for data in self.__buffer]

    def clear(self) -> None:
        """
        Drop all recorded data
        :return: None
        """
        self.__buffer.clear()
        self.__buffer_head = 0

    def __calculate_deviation(self,
                              moved_distance: int,
                              recorded_pwm_value: int) -> Deviation:
        """
        Assuming that a movement should've happened, calculated how strong
        the deviation of the recorded movement is.

        Movement below the axis tolerance is considered high deviation, since
        the axis should've moved.

        Movement below the double variance is considered a small
        deviation, as long as a variance and mean value can be
        calculated from the available datapoints.
        If the variance happens to be smaller than the axis-tolerance,
        it is bumped to that value for the calculation.

        In all other cases no deviation is detected.

        :param moved_distance: How much distance was actually moved,
            as absolute value
        :param recorded_pwm_value: At which pwm value the movement was
            recorded. Datapoints for a different pwm value will be ignored.
        :return: The determined deviation
        """

        # movement below axis tolerance is treated as no movement and thus
        # implies high deviation, since we assume movement
        if moved_distance < self.__movement_tolerance:
            return Deviation.HIGH

        # check whether we have enough data points to detect small deviations
        relevant_data_points = [point for point in self.__buffer if
                                point.current_pwm_value == recorded_pwm_value
                                and point.deviation == Deviation.NONE
                                and point.moved_distance is not None]
        if len(relevant_data_points) < self.__required_cycles_to_average:
            return Deviation.NONE

        # note: not_none() is safe here since since presence of value has
        # been assured previously
        moved_distance_data_points = \
            [not_none(point.moved_distance) for point in relevant_data_points]
        mean = calculate_mean(moved_distance_data_points)
        variance = calculate_variance(moved_distance_data_points)
        # a variance smaller than the axis tolerance is not really helpful
        variance = max(variance, self.__movement_tolerance)

        if moved_distance < mean - 2 * variance:
            logging.debug(f"minor deviation detected: "
                          f"moved_distance={moved_distance}, "
                          f"mean={mean}, "
                          f"variance={variance}")
            return Deviation.SMALL
        else:
            return Deviation.NONE

    def __get_last_pushed_data(self) -> CycleData | None:
        """
        Retrieves the last data pushed to the buffer.

        If the buffer is empty, `None` is returned.
        :return: The previous pushed data or `None`
        """
        if len(self.__buffer) == 0:
            return None

        if self.__buffer_head == 0:
            previous_buffer_head = self.__max_buffer_len - 1
        else:
            previous_buffer_head = self.__buffer_head - 1

        return self.__buffer[previous_buffer_head]

    def __push_to_buffer(self, data: CycleData) -> None:
        """
        Pushed data into the buffer, ensuring its maximum length is not
        exceeded
        :param data: Data to push
        :return: None
        """
        if len(self.__buffer) < self.__max_buffer_len:
            self.__buffer.append(data)
        else:
            self.__buffer[self.__buffer_head] = data
        self.__advance_buffer_head()

    def __advance_buffer_head(self) -> None:
        """
        Increments the buffer head to the next position, optionally wrapping it
        around to the beginning of the buffer
        :return: None
        """
        next_buffer_head = self.__buffer_head + 1
        self.__buffer_head = next_buffer_head % self.__max_buffer_len


@dataclass(frozen=True)
class CycleData:
    """
    Stores data about an axis which was only available during a specific cycle
    """
    counter_value: int
    """The counter value of the axis during the cycle"""
    current_pwm_value: int
    """The pwm value which is selected for the cycle. Will be set to `0` if
    the axis is not supposed to move during the cycle."""
    moved_distance: int | None
    """The moved distance since the last cycle"""
    deviation: Deviation
    """How much the moved distance deviates from the expected movement"""


class Deviation(Enum):
    """
    Categorization for how much the moved distance of an axis deviates from
    the expected movement
    """
    NONE = 0
    """No deviation"""
    SMALL = 1
    """Deviation which is bigger than the standard deviation"""
    HIGH = 2
    """Movement below what the axis tolerates as movement"""

    def __str__(self) -> str:
        return f"{self.name.lower()} deviation"


def calculate_mean(values: list[float]) -> float:
    """
    Calculates the average of a list of numeric values
    :param values: A list of numeric values
    :return: The average value
    """
    return sum(values) / len(values)


def calculate_variance(values: list[float]) -> float:
    """
    Calculates the variance of a list of numeric values using Bessel's
    correction.

    If less than two values are provided, 0 is returned.
    :param values: A list of numeric values
    :return: The variance or 0 if less than two values were provided
    """
    sample_size = len(values)
    if sample_size < 2:
        return 0

    mean = calculate_mean(values)
    v2 = sum((value - mean) ** 2 for value in values) / (sample_size - 1)
    return sqrt(v2)


T = TypeVar('T')


def not_none(obj: T | None) -> T:
    if obj is None:
        raise ValueError("Object must not be None")
    return obj
