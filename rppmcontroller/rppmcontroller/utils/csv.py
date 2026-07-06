import csv
from typing import Any, Iterable, List


class CSVWriter:
    def __init__(self, path: str):
        self.__path = path

    @property
    def path(self) -> str:
        return self.__path

    def write(self, row: Iterable[Any]):
        with open(self.path, "a", newline="") as file:
            csv.writer(file).writerow(row)


class CSVReader:
    def __init__(self, path: str):
        self.__path = path

    @property
    def path(self) -> str:
        return self.__path

    def read(self) -> List[Any]:
        with open(self.path, "r", newline="") as file:
            return list(csv.reader(file))
