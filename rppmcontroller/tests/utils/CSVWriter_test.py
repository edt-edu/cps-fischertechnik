from unittest import TestCase

from rppmcontroller.utils.csv import CSVWriter


class CSVWriterTestSuite(TestCase):
    def setUp(self):
        self.writer = CSVWriter("tmp.csv")

    def test_write(self):
        self.writer.write(["name", "age"])
        self.writer.write(["John", 36])
