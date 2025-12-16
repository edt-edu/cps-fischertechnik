from dataclasses import dataclass
from typing import Self


@dataclass
class HighBayParameters:
    """
    All configurable parameters of the HighBay machine.
    """

    pickup_distance: int = 150
    """How far up the arm will be moved, when picking up an item"""
    conveyor_column: int = 70
    """The encoder value of the horizontal conveyor position"""
    right_column: int = 1550
    """The encoder value of the horizontal position of the column closest to the conveyor"""
    middle_column: int = 2700
    """The encoder value of the horizontal position of the middle column"""
    left_column: int = 3900
    """The encoder value of the horizontal position of the column furthest from the conveyor"""
    conveyor_row: int = 1450
    """The encoder value of the vertical conveyor position"""
    bottom_row: int = 1700
    """The encoder value of the vertical position of the bottom row"""
    middle_row: int = 900
    """The encoder value of the vertical position of the middle row"""
    top_row: int = 200
    """The encoder value of the vertical position of the top row"""
    # TODO vertical_safety_pos
    # TODO horizontal_safety_pos
    # TODO pwm_parameters

    def add_horizontal_offset(self, offset: int) -> Self:
        """
        Add an offset to all columns.

        :param offset: A relative offset
        :return: self
        """
        self.conveyor_column += offset
        self.right_column += offset
        self.middle_column += offset
        self.left_column += offset
        return self

    def add_vertical_offset(self, offset: int) -> Self:
        """
        Add an offset to all rows.

        :param offset: A relative offset
        :return: self
        """
        self.conveyor_row += offset
        self.bottom_row += offset
        self.middle_row += offset
        self.top_row += offset
        return self

    # ---- pickup_distance ----

    def with_pickup_distance(self, pickup_distance: int) -> Self:
        """
        Set a value for the pickup_distance.

        :param pickup_distance: The new absolute pickup_distance
        :return: self
        """
        self.pickup_distance = max(0, pickup_distance)
        return self

    def add_pickup_distance_offset(self, offset: int) -> Self:
        """
        Adjust the pickup_distance by a relative offset.

        :param offset: The relative adjustment to apply
        :return: self
        """
        return self.with_pickup_distance(self.pickup_distance + offset)

    # ---- conveyor_column ----

    def with_conveyor_column(self, conveyor_column: int) -> Self:
        """
        Set a value for the conveyor_column.

        :param conveyor_column: The new absolute conveyor_column
        :return: self
        """
        self.conveyor_column = max(0, conveyor_column)
        return self

    def add_conveyor_column_offset(self, offset: int) -> Self:
        """
        Adjust the conveyor_column by a relative offset.

        :param offset: The relative adjustment to apply
        :return: self
        """
        return self.with_conveyor_column(self.conveyor_column + offset)

    # ---- right_column ----

    def with_right_column(self, right_column: int) -> Self:
        """
        Set a value for the right_column.

        :param right_column: The new absolute right_column
        :return: self
        """
        self.right_column = max(0, right_column)
        return self

    def add_right_column_offset(self, offset: int) -> Self:
        """
        Adjust the right_column by a relative offset.

        :param offset: The relative adjustment to apply
        :return: self
        """
        return self.with_right_column(self.right_column + offset)

    # ---- middle_column ----

    def with_middle_column(self, middle_column: int) -> Self:
        """
        Set a value for the middle_column.

        :param middle_column: The new absolute middle_column
        :return: self
        """
        self.middle_column = max(0, middle_column)
        return self

    def add_middle_column_offset(self, offset: int) -> Self:
        """
        Adjust the middle_column by a relative offset.

        :param offset: The relative adjustment to apply
        :return: self
        """
        return self.with_middle_column(self.middle_column + offset)

    # ---- left_column ----

    def with_left_column(self, left_column: int) -> Self:
        """
        Set a value for the left_column.

        :param left_column: The new absolute left_column
        :return: self
        """
        self.left_column = max(0, left_column)
        return self

    def add_left_column_offset(self, offset: int) -> Self:
        """
        Adjust the left_column by a relative offset.

        :param offset: The relative adjustment to apply
        :return: self
        """
        return self.with_left_column(self.left_column + offset)

    # ---- conveyor_row ----

    def with_conveyor_row(self, conveyor_row: int) -> Self:
        """
        Set a value for the conveyor_row.

        :param conveyor_row: The new absolute conveyor_row
        :return: self
        """
        self.conveyor_row = max(0, conveyor_row)
        return self

    def add_conveyor_row_offset(self, offset: int) -> Self:
        """
        Adjust the conveyor_row by a relative offset.

        :param offset: The relative adjustment to apply
        :return: self
        """
        return self.with_conveyor_row(self.conveyor_row + offset)

    # ---- bottom_row ----

    def with_bottom_row(self, bottom_row: int) -> Self:
        """
        Set a value for the bottom_row.

        :param bottom_row: The new absolute bottom_row
        :return: self
        """
        self.bottom_row = max(0, bottom_row)
        return self

    def add_bottom_row_offset(self, offset: int) -> Self:
        """
        Adjust the bottom_row by a relative offset.

        :param offset: The relative adjustment to apply
        :return: self
        """
        return self.with_bottom_row(self.bottom_row + offset)

    # ---- middle_row ----

    def with_middle_row(self, middle_row: int) -> Self:
        """
        Set a value for the middle_row.

        :param middle_row: The new absolute middle_row
        :return: self
        """
        self.middle_row = max(0, middle_row)
        return self

    def add_middle_row_offset(self, offset: int) -> Self:
        """
        Adjust the middle_row by a relative offset.

        :param offset: The relative adjustment to apply
        :return: self
        """
        return self.with_middle_row(self.middle_row + offset)

    # ---- top_row ----

    def with_top_row(self, top_row: int) -> Self:
        """
        Set a value for the top_row.

        :param top_row: The new absolute top_row
        :return: self
        """
        self.top_row = max(0, top_row)
        return self

    def add_top_row_offset(self, offset: int) -> Self:
        """
        Adjust the top_row by a relative offset.

        :param offset: The relative adjustment to apply
        :return: self
        """
        return self.with_top_row(self.top_row + offset)
