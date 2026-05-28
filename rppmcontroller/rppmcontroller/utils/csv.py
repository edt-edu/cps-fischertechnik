import csv
from typing import Any, Iterable


class CSVWriter:
    def __init__(self, path: str):
        file = open(path, "w", newline="")

        self.__file = file
        self.__writer = csv.writer(file)

    def write(self, row: Iterable[Any]):
        self.__writer.writerow(row)
        self.__file.flush()
