class ResetHelper:
    """
    Remembers whether a counter should be reset at the next opportunity
    """

    def __init__(self, is_marked_for_reset: bool = False,
                 temper_tolerance: int = 200) -> None:
        """
        Create a new ResetHelper
        :param is_marked_for_reset: Initial state of the ResetHelper
        :param temper_tolerance: The temper tolerance, defaults to 200. See getter for details.
        """
        self.__marked_for_reset = is_marked_for_reset
        """Whether this is marked for reset"""
        # ensure temper_tolerance is not negative
        self.__temper_tolerance = max(temper_tolerance, 0)
        """Temper tolerance, see getter"""

    @property
    def temper_tolerance(self) -> int:
        """
        The temper tolerance is the maximum value a counter may have, in order
        for a conditional reset to be applied.

        This is motivated by the fact, that we don't want to reset the counter
        values, when somebody manually pressed the ref-switch. Otherwise, a
        potentially harmful offset might be introduced in the robots movements.

        Example:
        .. code-block:: python

            reset_helper = ResetHelper(temper_tolerance=200)
            reset_helper.mark_for_reset_if(True, 800)  # we assume somebody tempered with the ref-switch; no mark for reset actually happens.
            reset_helper.mark_for_reset_if(True, 10)  # counter is close enough to ref-switch; helper is marked for reset
            reset_helper.mark_for_reset_if(True, -800) # this always works since we shouldn't move beyond the ref-switch
        :return: The temper tolerance
        """
        return self.__temper_tolerance

    def mark_for_reset(self) -> None:
        """
        Marks this for reset
        :return: None
        """
        self.__marked_for_reset = True

    def mark_for_reset_if(self, condition: bool,
                          current_counter_value: int = -1) -> bool:
        """
        Marks this for reset if the condition evaluates to `True`
        :param condition: An evaluated condition
        :param current_counter_value: Current encoder counter value. Relevant for temper tolerance check.
        :return: The value of condition
        """
        if condition and current_counter_value < self.temper_tolerance:
            self.mark_for_reset()
        return condition

    @property
    def is_marked_for_reset(self) -> bool:
        """
        Whether this is marked for reset
        :return: True if this is marked for reset, otherwise False
        """
        return self.__marked_for_reset

    def reset(self) -> bool:
        """
        Retrieves whether this is marked for reset and removes that mark.

        This means that consecutive calls of this function will at maximum
        return `True` on the first call.

        If you just want a peek of the current state, you should just call
        `is_marked_for_reset`.

        :return: `True` if the counter value should be reset, otherwise `False`
        """
        res = self.is_marked_for_reset
        self.__marked_for_reset = False
        return res
