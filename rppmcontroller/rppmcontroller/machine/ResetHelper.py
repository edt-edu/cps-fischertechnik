class ResetHelper:
    """
    Remembers whether a counter should be reset at the next opportunity
    """

    def __init__(self, is_marked_for_reset: bool = False) -> None:
        """
        Create a new ResetHelper
        :param is_marked_for_reset: Initial state of the ResetHelper
        """
        self.__marked_for_reset = is_marked_for_reset
        """Whether this is marked for reset"""

    def mark_for_reset(self) -> None:
        """
        Marks this for reset
        :return: None
        """
        self.__marked_for_reset = True

    def mark_for_reset_if(self, condition: bool) -> bool:
        """
        Marks this for reset if the condition evaluates to `True`
        :param condition: An evaluated condition
        :return: The value of condition
        """
        if condition:
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
