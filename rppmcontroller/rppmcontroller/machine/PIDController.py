from __future__ import annotations


class PIDController:
    def __init__(self,
                 kp: float,
                 ki: float,
                 kd: float,
                 sample_time: float,
                 deadband: int = 1,
                 min_pwm: int = 15,
                 max_pwm: int = 100,
                 integral_limit: float | None = 100,
                 derivative_filter_reaction_seconds: float = 0.02):
        """
        Creates a new PID controller.
        :param kp: proportional gain
        :param ki: integral gain
        :param kd: derivative gain
        :param sample_time: in what interval this controller is called,
            usually the main loop delay
        :param deadband: encoder tolerance; |error| <= deadband => output = 0
        :param min_pwm: Minimal absolute output
        :param max_pwm: Maximal absolute output
        :param integral_limit: A limit to stop the integral term from growing,
        if the output is maxed out for longer periods of time.
            Omitting this limit with an integral gain above zero may cause
            overshooting.
        :param derivative_filter_reaction_seconds: How long it takes for the
            derivative low-pass filter to react to changes in the derivative.
        """
        if kp < 0:
            raise ValueError(
                f"Proportional gain must be non-negative (found {kp})")
        if ki < 0:
            raise ValueError(
                f"Integral gain must be non-negative (found {ki})")
        if kd < 0:
            raise ValueError(
                f"Derivative gain must be non-negative (found {kd})")
        if sample_time <= 0:
            raise ValueError(
                f"Sample time must be greater than zero (found {sample_time}sec)")
        if min_pwm < 0:
            raise ValueError(
                f"Minimal output must be an absolute value (found {min_pwm})")
        if max_pwm < 0:
            raise ValueError(
                f"Maximal output must be an absolute value (found {max_pwm})")
        if min_pwm > max_pwm:
            raise ValueError(
                f"Minimal output must be less or equal to maximal output (found {min_pwm} > {max_pwm})")

        self.kp, self.ki, self.kd = kp, ki, kd
        self.dt = sample_time
        self.deadband = deadband
        self.min_output, self.max_output = min_pwm, max_pwm
        self.integral_limit = integral_limit

        self.derivative_filter_alpha = self.dt / (
                self.dt + derivative_filter_reaction_seconds)
        """The coefficient for the derivative low-pass filter"""
        self.integral: float = 0
        """The integral term of this"""
        self.target: int = 0
        """The current target value to reach"""
        self.prev_measurement: int | None = None
        """The value that was measured in the last tick"""
        self.filtered_derivative: float = 0
        """The first order filtered derivative term"""

    def reset(self) -> None:
        """
        Reset the controller
        :return: None
        """
        self.integral = 0
        self.prev_measurement = None
        self.filtered_derivative = 0

    def update(self, measurement: int) -> int:
        """
        Updates the controller with the current value while calculating the
        next output value to reach the target value.
        :param measurement: The currently measured value
        :return: The next recommended pid value to reach the target value
        """
        error = self.target - measurement
        if abs(error) <= self.deadband:
            # half the integral since we might need it when gravity or any other imbalance pushes us out of the deadband
            self.integral /= 2
            self.prev_measurement = measurement
            return 0

        # proportional
        proportional = self.kp * error

        # differential
        step = measurement - self.prev_measurement
        raw_derivative = -self.kd * step / self.dt if self.prev_measurement is not None else 0
        self.filtered_derivative += self.derivative_filter_alpha * (raw_derivative - self.filtered_derivative)
        self.prev_measurement = measurement

        # incremental
        tentative_integral = self.integral + self.ki * error * self.dt
        if self.integral_limit is not None:
            tentative_integral = min(max(tentative_integral, -self.integral_limit), self.integral_limit)


        return 0  # TODO
