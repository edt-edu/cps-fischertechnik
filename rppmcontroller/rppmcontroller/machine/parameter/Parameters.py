import typing
from typing import Generic

P = typing.TypeVar('P')


class IntParameter(Generic[P]):
    """
    Represents a parameter of the type `int`
    """

    def __init__(self, machine_parameters_instance: P, default_value: int):
        """
        Create a new IntParameter.

        :param machine_parameters_instance: The machine parameters instance holding the parameter
        :param default_value: The default value of the parameter
        """
        self.__parameters = machine_parameters_instance
        self.__value = default_value

    def get(self) -> int:
        """
        Get the value of the parameter

        :return: The value of the parameter
        """
        return self.__value

    def set(self, value: int) -> P:
        """
        Set the absolute value of the parameter.

        :param value: The new absolute value for the parameter
        :return: The machine parameters instance holding this parameter
        """
        self.__value = max(0, value)
        return self.__parameters

    def add(self, offset: int) -> P:
        """
        Add an offset to the value of this parameter.

        :param offset: An offset to add to the value of this parameter
        :return: The machine parameters instance holding this parameter
        """
        return self.set(self.get() + offset)

    def __iadd__(self, offset: int) -> None:
        """
        Convenience overload for adding an offset, see `add` method.
        :param offset: An offset to add
        :return: The machine parameters instance holding this parameter
        """
        self.add(offset)

    def __call__(self, *args, **kwargs) -> int:
        """
        Convenience overload for getting the parameter value, see `get` method.

        :param args: ignored
        :param kwargs: ignored
        :return: The machine parameters instance holding this parameter
        """
        return self.get()
