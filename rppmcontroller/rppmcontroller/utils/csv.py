import csv
import os
from typing import Any, Iterable


class CSVWriter:
    def __init__(self, path: str):
        self.__path = path

        # delete previous data to avoid data clutter
        if os.path.exists(path):
            os.remove(path)

    def write(self, row: Iterable[Any]):
        with open(self.__path, "a", newline="") as file:
            csv.writer(file).writerow(row)
