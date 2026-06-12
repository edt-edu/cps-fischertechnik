import inspect
import logging
import unittest

from rppmcontroller.machine.sortingLine.SortingLine import SortingLine


class SortingLineTestCase(unittest.TestCase):

    def setUp(self):
        self.sortingLine1 = SortingLine("1")

    def test_Stop(self):
        """
            Test if the stop-function stops the conveyor acting forward
        """
        logging.debug(f'{inspect.stack()[0][3]} start')

        self.sortingLine1.sortingLineActMotorConveyor = True
        self.sortingLine1.sortingLineActCompressorOn = True
        self.sortingLine1.sortingLineActWhiteEjector = self.sortingLine1.sortingLineActRedEjector = self.sortingLine1.sortingLineActBlueEjector = True
        # stop
        logging.debug("stop")
        self.sortingLine1.stop_CycleStep()
        self.assertEqual(self.sortingLine1.sortingLineActMotorConveyor, False)
        self.assertEqual(self.sortingLine1.sortingLineActCompressorOn, False)
        self.assertEqual(self.sortingLine1.sortingLineActWhiteEjector, False)
        self.assertEqual(self.sortingLine1.sortingLineActRedEjector, False)
        self.assertEqual(self.sortingLine1.sortingLineActBlueEjector, False)

if __name__ == '__main__':
    logging.basicConfig(format='%(levelname)-5s: %(module)-20s,%(lineno)-3s: %(message)s', level=logging.DEBUG)

    unittest.main()




