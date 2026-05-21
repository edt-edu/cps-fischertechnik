
import inspect
import logging
import unittest

from rppmcontroller.machine.conveyorbelt.ConveyorBelt import ConveyorBelt


class ConveyorBeltTestCase(unittest.TestCase):

    def setUp(self):
        self.conveyor1 = ConveyorBelt("1")

    def test_Stop(self):
        """
            Test if the stop-function stops the conveyor acting forward
        """
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
