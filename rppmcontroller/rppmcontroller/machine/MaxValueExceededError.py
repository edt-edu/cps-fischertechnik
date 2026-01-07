class MaxValueExceededError(Exception):
    """
    Indicates that a move to a specific configuration would exceed the
    maximum counter-value
    """

    def __init__(self, requested_counter_value: int, max_counter_value: int):
        self.__requested_counter_value = requested_counter_value
        self.__max_counter_value = max_counter_value

    @property
    def message(self) -> str:
        return (f"Movement to counter-value {self.__requested_counter_value} "
                f"would exceed maximum counter-value of {self.__max_counter_value}")

    def __bool__(self):
        """
        Evaluates to `True` so that this can be used in an if-statement
        :return: `True`
        """
        return True

    def __str__(self):
        return self.message
