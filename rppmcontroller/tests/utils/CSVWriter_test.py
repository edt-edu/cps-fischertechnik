import os
from unittest import TestCase

from rppmcontroller.utils.csv import CSVWriter

PATH = "tmp.csv"


class CSVWriterTestSuite(TestCase):
    def setUp(self):
        self.writer = CSVWriter(PATH)

    def tearDown(self):
        if os.path.exists(PATH):
            os.remove(PATH)

    def test_write(self):
        self.writer.write(["name", "age"])
        self.writer.write(["John", 36])

        self.assertEqual(["name,age\n", "John,36\n"],
                         [line for line in open("tmp.csv", "r").readlines()])
