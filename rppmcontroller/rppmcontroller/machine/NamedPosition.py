class NamedPosition:
    def __init__(self, name: str):
        self.__name = name

    @property
    def name(self):
        return self.__name

    def __str__(self):
        return self.name

    def __repr__(self):
        return f"NamedPosition({self.name})"
