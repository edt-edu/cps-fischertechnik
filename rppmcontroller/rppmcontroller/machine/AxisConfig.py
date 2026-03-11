class AxisConfig:
    """
    Interface to update the position of an Axis
    """

    @staticmethod
    def to_end_position():
        """
        Create an AxisConfig which moves the component until the end of the axis is reached
        :return: The created config
        """
        return AxisConfig(True, 0)

    @staticmethod
    def to_counter_goal(counter_goal: int):
        """
        Creates an AxisConfig which moves the component to the specified counter position
        :param counter_goal: The position to move the component to
        :return: The created config
        """
        return AxisConfig(False, counter_goal)

    def __init__(self, end_position: bool, counter_goal: int):
        """
        Create a new AxisConfig. It is discouraged to use this constructor
        directly and instead use the static methods instead
        :param end_position: Whether to move to the end position of the axis. When setting this option, counter_goal is ignored
        :param counter_goal: The value of the counter to move to
        """
        self.__end_position = end_position
        self.__counter_goal = counter_goal

    @property
    def end_position(self):
        return self.__end_position

    @property
    def counter_goal(self) -> int:
        return self.__counter_goal if not self.end_position else 0

    @counter_goal.setter
    def counter_goal(self, value: int) -> None:
        self.__counter_goal = value
        self.__end_position = False

    def __str__(self):
        return f"AxisConfig(end_position: {self.end_position}, counter_goal: {self.counter_goal})"
