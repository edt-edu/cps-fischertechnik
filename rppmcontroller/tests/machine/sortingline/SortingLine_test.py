import inspect
import logging

from rppmcontroller.machine.sortingLine.SortingLine import SortingLine
from rppmcontroller.machine.Colour import Colour

import unittest

class SortingLineTestCase(unittest.TestCase):
    
    def setUp(self):
        self.sortingLine1 = SortingLine(1)

    def test_Stop(self):
        '''
            Test if stop fucntion stop the conveyor acting forward
        '''
        logging.debug(f'{inspect.stack()[0][3]} start')
        
        self.sortingLine1.sortingLineActMotorConveyor = True
        self.sortingLine1.sortingLineActCompressorOn = True
        self.sortingLine1.sortingLineActWhiteEjector = self.sortingLine1.sortingLineActRedEjector = self.sortingLine1.sortingLineActBlueEjector = True     
        # stop
        logging.debug("stop")
        self.sortingLine1.stop()
        self.assertEqual(self.sortingLine1.sortingLineActMotorConveyor, False)
        self.assertEqual(self.sortingLine1.sortingLineActCompressorOn, False)
        self.assertEqual(self.sortingLine1.sortingLineActWhiteEjector, False)
        self.assertEqual(self.sortingLine1.sortingLineActRedEjector, False)
        self.assertEqual(self.sortingLine1.sortingLineActBlueEjector, False)


    def test_Eject(self):
        '''
            Test if eject function start the conveyor
            These tests does not verify the logics of functions relating with sensors and counter
        '''
        logging.debug(f'{inspect.stack()[0][3]} start')

        # white
        self.sortingLine1.eject(Colour.WHITE)
        self.assertEqual(self.sortingLine1.sortingLineActMotorConveyor, True)
        self.sortingLine1.stop()
        # blue
        self.sortingLine1.eject(Colour.BLUE)
        self.assertEqual(self.sortingLine1.sortingLineActMotorConveyor, True)
        self.sortingLine1.stop()
        # red
        self.sortingLine1.eject(Colour.RED)
        self.assertEqual(self.sortingLine1.sortingLineActMotorConveyor, True)
        self.sortingLine1.stop()

if __name__ == '__main__':
    logging.basicConfig(format='%(levelname)-5s: %(module)-20s,%(lineno)-3s: %(message)s', level=logging.DEBUG)

    unittest.main()
        
        
    
        
