import csv
from typing import Any, Iterable


class CSVWriter:
    def __init__(self, path: str):
        self.__path = path

    @property
    def path(self) -> str:
        return self.__path

    def write(self, row: Iterable[Any]):
        with open(self.path, "a", newline="") as file:
            csv.writer(file).writerow(row)
