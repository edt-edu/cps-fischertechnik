
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
        


    def test_conveyorActForwardAndBackward(self):
        '''
            Test if the functions action the conveyor in the good direction
            These tests does not verify the logics of functions relating with sensors and counter
        '''
        logging.debug(f'{inspect.stack()[0][3]} start')

        # forward from anywhere
        logging.debug("forward from anywhere")
        self.conveyor1.forwardFromAnywhere_CycleStep()
        self.assertEqual(self.conveyor1.conveyorActForward, True)
        self.assertEqual(self.conveyor1.conveyorActBackward, False)
        self.conveyor1.stop_CycleStep()

        # backward from anywhere
        logging.debug("backward from anywhere")
        self.conveyor1.backwardFromAnywhere_CycleStep()
        self.assertEqual(self.conveyor1.conveyorActForward, False)
        self.assertEqual(self.conveyor1.conveyorActBackward, True)
        self.conveyor1.stop_CycleStep()

        # forward leave conveyor
        logging.debug("forward leave conveyor")
        self.conveyor1.forwardLeaveConveyor_CycleStep()
        self.assertEqual(self.conveyor1.conveyorActForward, True)
        self.assertEqual(self.conveyor1.conveyorActBackward, False)
        self.conveyor1.stop_CycleStep()
        
        # backward from anywhere
        logging.debug("backward leave conveyor")
        self.conveyor1.backwardLeaveConveyor_CycleStep()
        self.assertEqual(self.conveyor1.conveyorActForward, False)
        self.assertEqual(self.conveyor1.conveyorActBackward, True)
        self.conveyor1.stop_CycleStep()
        
        # forward go to
        logging.debug("forward go to")
        self.conveyor1.forwardGoto_CycleStep(3)
        self.assertEqual(self.conveyor1.conveyorActForward, True)
        self.assertEqual(self.conveyor1.conveyorActBackward, False)
        self.conveyor1.stop_CycleStep()

        # backward go to
        logging.debug("backward go to")
        self.conveyor1.backwardGoto_CycleStep(3)
        self.assertEqual(self.conveyor1.conveyorActForward, False)
        self.assertEqual(self.conveyor1.conveyorActBackward, True)
        self.conveyor1.stop_CycleStep()

if __name__ == '__main__':
    logging.basicConfig(format='%(levelname)-5s: %(module)-20s,%(lineno)-3s: %(message)s', level=logging.DEBUG)

    unittest.main()
