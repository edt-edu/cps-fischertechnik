
import inspect
import logging

from rppmcontroller.machine.conveyorbelt.ConveyorBelt import ConveyorBelt
from rppmcontroller.machine.conveyorbelt.ConveyorBeltConfig import ConveyorBeltConfig
from rppmcontroller.machine.Direction import Direction
from rppmcontroller.utils.PlusMinusStop import PlusMinusStop
from rppmcontroller.machine.Position import Position

import unittest

class ConveyorBeltTestCase(unittest.TestCase):

    def setUp(self):
        self.conveyor1 = ConveyorBelt(1)
    
    def test_Stop(self):
        '''
            Test if stop fucntion stop the conveyor acting forward
        '''
        logging.debug(f'{inspect.stack()[0][3]} start')
        self.conveyor1.conveyorActForward = True
        # stop
        logging.debug("stop")
        self.conveyor1.stop_CycleStep()
        self.assertEqual(self.conveyor1.conveyorActForward, False)
        self.assertEqual(self.conveyor1.conveyorActBackward, False)

if __name__ == '__main__':
    logging.basicConfig(format='%(levelname)-5s: %(module)-20s,%(lineno)-3s: %(message)s', level=logging.DEBUG)

    unittest.main()
