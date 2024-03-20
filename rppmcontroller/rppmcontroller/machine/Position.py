class Position:

    def __init__(self, meaning, vertical, rot, horizontal):
        if (meaning is None) or (vertical is None) or (rot is None) or (horizontal is None):
            raise TypeError("Missing arguments")
        self.__meaning = meaning
        self.__vertical = vertical
        self.__rot = rot
        self.__horizontal = horizontal

    @property
    def meaning(self):
        return self.__meaning

    @property
    def vertical(self):
        return self.__vertical

    @property
    def rot(self):
        return self.__rot

    @property
    def horizontal(self):
        return self.__horizontal

    def __eq__(self, other):
        return self.__meaning == other.meaning and self.__vertical == other.vertical and self.__rot == other.rot and self.__horizontal == other.horizontal

    def __repr__(self):
        return "meaning " + str(self.__meaning) + " vertical " + str(self.__vertical) + " rot " + str(self.__rot) + " horizontal " + str(self.__horizontal)
