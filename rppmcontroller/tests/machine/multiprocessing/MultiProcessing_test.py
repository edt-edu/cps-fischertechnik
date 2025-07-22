import inspect
import logging

from rppmcontroller.behavior.CycleStepResultEnum import CycleStepResultEnum
from rppmcontroller.machine.multiprocessing.MultiProcessing import MultiProcessing
from rppmcontroller.machine.multiprocessing.MultiProcessingConfig import MultiProcessingConfig
from rppmcontroller.machine.Direction import Direction


import unittest

class MultiProcessingTestCase(unittest.TestCase):
    def setUp(self):
        self.multiProcessing1 = MultiProcessing(1)

    
    def test_Stop(self):
        '''
            Test if stop function stop the multi processing station
        '''
        logging.debug(f'{inspect.stack()[0][3]} start')

        self.multiProcessing1.multiProcessingActGripperToOven = True
        self.multiProcessing1.multiProcessingActGripperToTurntable = True
        self.multiProcessing1.multiProcessingActRotCounterclockwise = True
        self.multiProcessing1.multiProcessingActRotClockwise = True
        self.multiProcessing1.multiProcessingActOvenInward = True
        self.multiProcessing1.multiProcessingActOvenOutward = True
        self.multiProcessing1.multiProcessingActSaw = True
        self.multiProcessing1.multiProcessingCompressor = True
        self.multiProcessing1.multiProcessingValveOvenDoor = True
        self.multiProcessing1.multiProcessingValveFeeder = True
        self.multiProcessing1.multiProcessingValveVacuum = True
        #stop
        logging.debug("stop")
        self.multiProcessing1.stop_CycleStep()
        self.assertEqual(self.multiProcessing1.multiProcessingActGripperToOven, False)
        self.assertEqual(self.multiProcessing1.multiProcessingActGripperToTurntable, False)
        self.assertEqual(self.multiProcessing1.multiProcessingActRotCounterclockwise, False)
        self.assertEqual(self.multiProcessing1.multiProcessingActRotClockwise, False)
        self.assertEqual(self.multiProcessing1.multiProcessingActOvenInward, False)
        self.assertEqual(self.multiProcessing1.multiProcessingActOvenOutward, False)
        self.assertEqual(self.multiProcessing1.multiProcessingOvenLight, False)
        self.assertEqual(self.multiProcessing1.multiProcessingActSaw, False)
        self.assertEqual(self.multiProcessing1.multiProcessingCompressor, False)
        self.assertEqual(self.multiProcessing1.multiProcessingValveOvenDoor, False)
        self.assertEqual(self.multiProcessing1.multiProcessingValveFeeder, False)
        self.assertEqual(self.multiProcessing1.multiProcessingValveVacuum, False)
        self.assertEqual(self.multiProcessing1.isExecuting, False)


if __name__ == '__main__':
    logging.basicConfig(format='%(levelname)-5s: %(module)-20s,%(lineno)-3s: %(message)s', level=logging.DEBUG)

    unittest.main()



