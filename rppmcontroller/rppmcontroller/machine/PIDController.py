from __future__ import annotations

import logging
from abc import ABC, abstractmethod
from dataclasses import dataclass
from enum import Enum
from typing import Callable, TypeVar, Generic
from typing_extensions import override

from rppmcontroller.machine.MachineParameters import PIDParameters

T = TypeVar('T')
I = TypeVar('I')
O = TypeVar('O')
R = TypeVar('R')
M = TypeVar('M')


class SignalProcessingBlock(ABC, Generic[I, O]):
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
    def process(self, value: I) -> O:
        """
        Processes a value
        :param value: The value to process.
        :return: The processed value.
        """
        pass

    def __or__(self, other: SignalProcessingBlock[O, R]) -> \
        SignalProcessingBlock[I, R]:
        """
        Pipes this block's output into another block
        :param other: The block that processes the outputs of this block
        :return: A block that connects this and the other block with a pipe
        """
        return SimplePipe(self, other)


class OutPort(Generic[T]):
    """
    An abstraction of a port that outputs a value to the outside world.
    """

    def __init__(self, read: Callable[[], T]):
        self.__get: Callable[[], T] = read

    def get(self) -> T:
        return self.__get()


class InPort(OutPort[T]):
    """
    An abstraction of a port that receives input from the outside world.
    """

    def __init__(self, read: Callable[[], T], on_set: Callable[[T], None]):
        """
        Create a new In-Port
        :param on_set: A callback to be executed when the value of the port changes
        """
        super().__init__(read)
        self.__set: Callable[[T], None] = on_set

    def set(self, value: T) -> None:
        """
        Apply a new value to this port
        :param value: The value to apply
        :return: None
        """
        self.__set(value)


@dataclass(frozen=True)
class DiscretePIDController(SignalProcessingBlock[int, int]):
    """
    A wrapper around a discrete control-loop block that implements
    a PID control and a `target` port to steer the controller.
    """
    block: SignalProcessingBlock[int, int]
    """A discrete control-loop block that implements a PID control"""
    target: InPort[int]
    """A port exposing the target value to steer the controller"""

    @staticmethod
    def from_opt_parameters(parameters: PIDParameters | None) -> DiscretePIDController | None:
        """
        Creates a DiscretePIDController from the given PIDParameters or returns None if the passed parameters are None.
        :param parameters: PIDParameters to create a DiscretePIDController from.
        :return: The created DiscretePIDController, if any
        """
        return DiscretePIDController.from_parameters(parameters) if parameters is not None else None

    @staticmethod
    def from_parameters(parameters: PIDParameters) -> DiscretePIDController:
        """
        Creates a new DiscretePIDController for pwm control from the given
        parameters.
        :param parameters: A PIDParameters instance
        :return: The created pid controller
        """
        return DiscretePIDController.for_pwm_control(
            rppm_controller_delay=parameters.main_loop_delay,
            decimation_factor=parameters.decimation_factor,
            smoothing_delay_seconds=parameters.smoothing_delay_seconds,
            error_deadband=parameters.error_deadband,
            kp=parameters.kp,
            ki=parameters.ki,
            kd=parameters.kd,
            integral_limit=parameters.integral_limit,
        )

    @staticmethod
    def for_pwm_control(rppm_controller_delay: float,
                        *,
                        decimation_factor: int = 10,
                        smoothing_delay_seconds: float = 0.08,
                        error_deadband: float = 3.0,
                        kp: float = 1,
                        ki: float = 0,
                        kd: float = 0,
                        integral_limit: float | None = 25) -> DiscretePIDController:
        """
        Creates a new DiscretePIDController suitable for PWM control.

        The controller will return output values in the range of [-100, 100],
        with negative output values implying a movement towards the ref-switch.

        **How to calibrate a PID controller**:

        1. With ki, kd, and error_deadband set to 0, raise kd until the arm to
           control barely oscillates, then half that value.
        2. Raise ki slowly until the steady state error is gone.
        3. Add kd at around half the value of kp and raise slowly until noise
           is eliminated.
        4. Set the error_deadband back to the smallest value that prevents
           inaccuracies from the encoder counter.

        :param rppm_controller_delay: The delay between two consecutive calls
            to the controller.
        :param decimation_factor: The factor by how much the pid controller is
            slowed down compared to the rppm controller.
            A PID control on 1kHz is probably not helpful.
        :param smoothing_delay_seconds: How many seconds delay will be added
            through buffering to smooth the input signal.
        :param error_deadband: How large the absolute deviation between the
            target position and the current position may be, before a control
            action is triggered.
        :param kp: The proportional gain.
        :param ki: The integral gain.
        :param kd: The derivative gain.
        :param integral_limit: Cap for the pid integral. Should be around
            25% max output.
        :return: The discrete PID controller.
        """
        control_period_seconds = rppm_controller_delay * decimation_factor
        lowpass_sample_weight = control_period_seconds / (
            smoothing_delay_seconds + control_period_seconds)
        output_limit = 100

        lowpass = LowPassFilter(lowpass_sample_weight)
        pid = PIDController(kp,
                            ki,
                            kd,
                            control_period_seconds,
                            error_deadband,
                            output_limit=output_limit,
                            integral_limit=integral_limit)
        target_value_port = InPort[int](lambda: int(pid.target_value),
                                        lambda value: pid.set_target(value))
        dual_band_clamp = DualBandClamp(out_min=15, out_max=output_limit)
        rounder = RoundToInt()

        pipeline = lowpass | pid | dual_band_clamp | rounder
        decimator = Decimator(pipeline, decimation_factor)
        return DiscretePIDController(decimator, target_value_port)

    def reset(self) -> None:
        self.block.reset()

    def process(self, value: I) -> O:
        return self.block.process(value)


class Decimator(SignalProcessingBlock[I, O]):
    """
    Decimates the number of times a signal is processed by a given factor,
    returning a cached value every other time.
    """

    def __init__(self,
                 signal_processor: SignalProcessingBlock[I, O],
                 N: int = 10):
        """
        Create a new int-signal decimator
        :param signal_processor: The function that does the actual signal processing
        :param N: The factor by which the processing of the signal is skipped with the last computed value
        """
        self.__signal_processor: SignalProcessingBlock[I, O] = signal_processor
        self.__N: int = N
        # None as default value is fine, since a correct value will always be
        # computed on the first call of the process method
        self.__cached_value: O = None
        """The most recent computed value"""
        self.__count: int = 0
        """How many times the process method has been called since the last computation"""

    @override
    def reset(self) -> None:
        self.__cached_value = 0
        self.__count = 0
        self.__signal_processor.reset()

    @override
    def process(self, value: I) -> O:
        """
        Process the given value, but only every N-th time
        :param value: The value to process
        :return: The processed value, or the previously computed value if processing was skipped
        """
        if self.__count % self.__N == 0:
            self.__cached_value = self.__signal_processor.process(value)
            self.__count = 0

        self.__count += 1
        return self.__cached_value


class LowPassFilter(SignalProcessingBlock[int, float]):
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


class DualBandClampZeroPolicy(Enum):
    """
    Decides how a DualBandClamp will handle a zero
    """
    LIFT = 0,
    """Lift the zero to the positive band"""
    LOWER = 1,
    """Lower the zero to the negative band"""
    PASSTHROUGH = 2,
    """Pass the zero to the output"""


class DualBandClamp(SignalProcessingBlock[float, float]):
    """
    Clamps and lifts a signal onto a symmetric dual-band. Zero will always
    be lifted onto the positive band.
    """

    def __init__(self,
                 *,
                 out_min: float = 15,
                 out_max: float = 100,
                 zero_policy: DualBandClampZeroPolicy = DualBandClampZeroPolicy.PASSTHROUGH):
        """
        Create a new dual-band clamp
        :param out_min: The absolute minimum that a value will be lifted to
        :param out_max: The absolute maximum that a value will be clamped to
        :param zero_policy: The zero policy to use
        """
        self.__out_min: float = out_min
        self.__out_max: float = out_max
        self.__zero_policy: DualBandClampZeroPolicy = zero_policy

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
        if value == 0:
            if self.__zero_policy == DualBandClampZeroPolicy.PASSTHROUGH:
                return 0
            if self.__zero_policy == DualBandClampZeroPolicy.LIFT:
                return self.out_min
            if self.__zero_policy == DualBandClampZeroPolicy.LOWER:
                return -self.out_min

        clamped = clamp(value, self.__out_max)
        return lift(clamped, self.__out_min)


class RoundToInt(SignalProcessingBlock[float, int]):
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


class PIDController(SignalProcessingBlock[float, float]):
    """
    A simple PID controller with anti-windup protection
    """

    def __init__(self,
                 kp: float,
                 ki: float,
                 kd: float,
                 control_period_seconds: float,
                 error_deadband: float,
                 *,
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

    def set_target(self, value: float) -> None:
        self.target_value = value

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
        logging.debug(f"error({error}) = target({self.target_value}) - value({value})")

        # error deadband
        if abs(error) <= self.__error_deadband:
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
        output = proportional_term + new_integral_term + derivative_term
        logging.debug(f"o({output}) = p({proportional_term}) + i({new_integral_term}) + d({derivative_term})")

        pushing_high = self.__output_limit is not None and output > self.__output_limit and error > 0
        pushing_low = self.__output_limit is not None and output < -self.__output_limit and error < 0
        if not pushing_high and not pushing_low:
            self.__integral_term = new_integral_term

        return output


class SimplePipe(SignalProcessingBlock[I, O]):
    """
    A simple pipe that connects two signal processing blocks
    """

    def __init__(self,
                 blockInput: SignalProcessingBlock[I, M],
                 blockOutput: SignalProcessingBlock[M, O]):
        """
        Creates a new pipe
        :param blockInput: The block that processes inputs
        :param blockOutput: The block that processes the outputs of the first block
        """
        self.__input = blockInput
        self.__output = blockOutput

    def reset(self) -> None:
        self.__input.reset()
        self.__output.reset()

    def process(self, value: I) -> O:
        return self.__output.process(self.__input.process(value))


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
