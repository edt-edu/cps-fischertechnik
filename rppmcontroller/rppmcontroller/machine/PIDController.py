from __future__ import annotations

from abc import ABC, abstractmethod
from typing import Callable, override


class SignalProcessingBlock(ABC):
    """
    A block in a control loop that processes a signal
    """

    @abstractmethod
    def reset(self) -> None:
        """
        Resets all internal state of this block
        :return: None
        """
        pass

    @abstractmethod
    def process(self, value):
        """
        Processes a value
        :param value: The value to process. The input type may vary for each block.
        :return: The processed value. The output type may vary for each block.
        """
        pass


class Decimator(SignalProcessingBlock):
    """
    Decimates the number of times a signal is processed by a given factor,
    returning a cached value every other time.
    """

    def __init__(self,
                 signal_processor: Callable[[int], int],
                 N: int = 10):
        """
        Create a new int-signal decimator
        :param signal_processor: The function that does the actual signal processing
        :param N: The factor by which the processing of the signal is skipped with the last computed value
        """
        self.__signal_processor: Callable[[int], int] = signal_processor
        self.__N: int = N
        # 0 as default value is fine, since a correct value will always be
        # computed on the first call of the process method
        self.__cached_value: int = 0
        """The most recent computed value"""
        self.__count: int = 0
        """How many times the process method has been called since the last computation"""

    @override
    def reset(self) -> None:
        self.__cached_value = 0
        self.__count = 0

    @override
    def process(self, value: int) -> int:
        """
        Process the given value, but only every N-th time
        :param value: The value to process
        :return: The processed value, or the previously computed value if processing was skipped
        """
        if self.__count >= self.__N:
            self.__cached_value = self.__signal_processor(value)
            self.__count = 0

        self.__count += 1
        return self.__cached_value


class LowPassFilter(SignalProcessingBlock):
    """
    A simple first-order low-pass filter
    """

    def __init__(self, new_sample_weight: float):
        """
        Create a new filter block
        :param new_sample_weight: How much influence a new sample should have. Must be between 0 and 1.
        """
        if new_sample_weight < 0 or 1 < new_sample_weight:
            raise ValueError("new_sample_weight must be between 0 and 1")

        self.__new_sample_weight: float = new_sample_weight
        self.__filtered_value: float | None = None
        """The last value that was returned by the process method"""

    @override
    def reset(self) -> None:
        self.__filtered_value = None

    def process(self, value: int) -> float:
        """
        Smoothens the signal
        :param value: The current value
        :return:
        """
        if self.__filtered_value is None:
            new_filtered_value = float(value)
        else:
            alpha = self.__new_sample_weight
            beta = 1 - alpha
            new_filtered_value = alpha * value + beta * self.__filtered_value

        self.__filtered_value = new_filtered_value
        return new_filtered_value


class DualBandClamp(SignalProcessingBlock):
    """
    Clamps and lifts a signal onto a symmetric dual-band. Zero will always
    be lifted onto the positive band.
    """

    def __init__(self, out_min: float = 15, out_max: float = 100):
        """
        Create a new dual-band clamp
        :param out_min: The absolute minimum that a value will be lifted to
        :param out_max: The absolute maximum that a value will be clamped to
        """
        self.__out_min: float = out_min
        self.__out_max: float = out_max

    @property
    def out_min(self) -> float:
        return self.__out_min

    @property
    def out_max(self) -> float:
        return self.__out_max

    def reset(self) -> None:
        pass

    def process(self, value: float) -> float:
        """
        Clamps the value between the minimum and maximum, retaining the sign.
        Zero will always be lifted onto the positive minimum.
        :param value: A value to clamp or lift
        :return: The value clamped and lifted to the dual-band
        """
        clamped = clamp(value, self.__out_max)
        return lift(clamped, self.__out_min)


class RoundToInt(SignalProcessingBlock):
    """
    Discretizes a signal by rounding it to the nearest integer
    """

    def reset(self) -> None:
        pass

    def process(self, value: float) -> int:
        """
        Rounds the value to the nearest integer
        :param value: A float
        :return: The nearest integer
        """
        return int(round(value))


class PIDController(SignalProcessingBlock):
    """
    A simple PID controller with anti-windup protection
    """

    def __init__(self,
                 kp: float,
                 ki: float,
                 kd: float,
                 control_period_seconds: float,
                 error_deadband: float,
                 output_limit: float | None = None,
                 integral_limit: float | None = None):
        """
        Creates a new PID controller
        :param kp: Proportional gain
        :param ki: Integral gain
        :param kd: Derivative gain
        :param control_period_seconds: The time between each call to process()
        :param error_deadband: The minimum error to trigger a control action
        :param output_limit: The maximum the actuators can deliver - output will not be clamped regardless!
        :param integral_limit: The maximum integral value, by default, the same as the output limit
        """
        self.__kp: float = kp
        self.__ki: float = ki
        self.__kd: float = kd
        self.__control_period_seconds: float = control_period_seconds
        self.__error_deadband: float = error_deadband
        self.__output_limit: float | None = output_limit
        self.__integral_limit: float | None = integral_limit if integral_limit is not None else output_limit

        self.__integral_term: float = 0
        """The stored integral value"""
        self.__previous_input: float | None = None
        """The previous processed input value"""
        self.target_value: float = 0
        """The desired value to reach"""

    def reset(self) -> None:
        self.__integral_term = 0
        self.__previous_input = None
        self.target_value = 0

    def process(self, value: float) -> float:
        """
        Calculates the recommended output value to reach the target value,
        based on the current input value.

        Does not clamp the output!
        :param value: The current input value
        :return: The recommended output value
        """

        # error
        error = self.target_value - value

        # error deadband
        if abs(error) < self.__error_deadband:
            self.__integral_term *= 0.5  # bleed integral for better input-spike-resistance
            self.__previous_input = value
            return 0

        # proportional
        proportional_term = self.__kp * error

        # differential
        velocity = (
                       value - self.__previous_input) / self.__control_period_seconds if self.__previous_input is not None else 0
        self.__previous_input = value
        derivative_term = -self.__kd * velocity  # negative since we want to dampen -> add a velocity in the opposed direction

        # integral
        unclamped_integral_term = self.__integral_term + self.__ki * error * self.__control_period_seconds
        new_integral_term = clamp(unclamped_integral_term,
                                  self.__integral_limit) if self.__integral_limit is not None else unclamped_integral_term

        # anti-windup; freeze the integral if pushing over limit
        output = proportional_term + self.__integral_term + derivative_term
        pushing_high = self.__output_limit is not None and output > self.__output_limit and error > 0
        pushing_low = self.__output_limit is not None and output < -self.__output_limit and error < 0
        if not pushing_high and not pushing_low:
            self.__integral_term = new_integral_term

        return output


def clamp(value: float, bound: float) -> float:
    """
    Clamps a value between -bound and bound
    :param value: The value to clamp
    :param bound: The bound to clamp to
    :return: The clamped value
    """
    return max(min(value, bound), -bound)


def lift(value: float, minimum: float) -> float:
    """
    Lift a value out of `-minimum` and `minimum`.
    Zero will be lifted to the positive minimum.
    :param value: The value to lift
    :param minimum: The minimum value to lift to
    :return: The lifted value
    """
    return max(value, minimum) if value >= 0 else min(value, -minimum)
