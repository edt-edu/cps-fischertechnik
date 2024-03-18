import unittest
from rppmcontroller.utils.PlusMinusStop import PlusMinusStop
from rppmcontroller.utils.ImpulseCounter import ImpulseCounter
from rppmcontroller.utils.Counter import Counter


class ImpulseCounterTestCase(unittest.TestCase):
    def setUp(self):
        self.counter = ImpulseCounter()
        self.cc = Counter()

    def testCounter(self):
        num = self.counter.compute(10, PlusMinusStop.PLUS)
        self.assertEqual(num, 10)
        num = self.counter.compute(20, PlusMinusStop.PLUS)
        self.assertEqual(num, 20)
        num = self.counter.compute(30, PlusMinusStop.MINUS)
        self.assertEqual(num, 10)
        num = self.counter.compute(70, PlusMinusStop.PLUS)
        self.assertEqual(num, 50)
        num = self.counter.compute(70, PlusMinusStop.MINUS)
        self.assertEqual(num, 50)
        num = self.counter.compute(70, PlusMinusStop.PLUS)
        self.assertEqual(num, 50)
        self.counter.counter = 0
        num = self.counter.compute(70, PlusMinusStop.PLUS)
        self.assertEqual(num, 0)
        num = self.counter.compute(80, PlusMinusStop.PLUS)
        self.assertEqual(num, 10)
        num = self.counter.compute(100, PlusMinusStop.MINUS)
        self.assertEqual(num, -10)

if __name__ == '__main__':
    unittest.main()
