import inspect
import logging

from rppmcontroller.machine.multiprocessing.MultiProcessing import MultiProcessing
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
        self.multiProcessing1.stop_cycleStep()
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

    def test_moveTurntableToSaw_firstMovement_vacuumPosition(self):
        # Simulate first movement from vacuum position
        self.multiProcessing1.multiProcessingSensTurntablePosVacuum = True

        self.multiProcessing1.moveTurntableToSaw()

        self.assertEqual(self.multiProcessing1.turnTableDirection, Direction.CLOCKWISE)
        self.assertTrue(self.multiProcessing1.multiProcessingActRotClockwise)
        self.assertFalse(self.multiProcessing1.multiProcessingActRotCounterclockwise)

    def test_moveTurntableToSaw_firstMovement_beltPosition(self):
        # Simulate first movement from belt position
        self.multiProcessing1.multiProcessingSensTurntablePosBelt = True

        self.multiProcessing1.moveTurntableToSaw()

        self.assertEqual(self.multiProcessing1.turnTableDirection, Direction.COUNTERCLOKWISE)
        self.assertFalse(self.multiProcessing1.multiProcessingActRotClockwise)
        self.assertTrue(self.multiProcessing1.multiProcessingActRotCounterclockwise)

    def test_moveTurntableToSaw_alreadyMovingClockwise(self):
        # Simulate ongoing movement in clockwise direction
        self.multiProcessing1.turnTableDirection = Direction.CLOCKWISE

        self.multiProcessing1.moveTurntableToSaw()

        self.assertTrue(self.multiProcessing1.multiProcessingActRotClockwise)
        self.assertFalse(self.multiProcessing1.multiProcessingActRotCounterclockwise)

    def test_moveTurntableToSaw_alreadyMovingCounterclockwise(self):
        # Simulate ongoing movement in counterclockwise direction
        self.multiProcessing1.turnTableDirection = Direction.COUNTERCLOKWISE

        self.multiProcessing1.moveTurntableToSaw()

        self.assertFalse(self.multiProcessing1.multiProcessingActRotClockwise)
        self.assertTrue(self.multiProcessing1.multiProcessingActRotCounterclockwise)

    def test_moveTurntableToSaw_reachesSawPosition(self):
        # Simulate reaching the saw position
        self.multiProcessing1.multiProcessingSensTurntablePosSaw = True
        self.multiProcessing1.turnTableDirection = Direction.CLOCKWISE

        self.multiProcessing1.moveTurntableToSaw()

        self.assertEqual(self.multiProcessing1.turnTableDirection, Direction.NONE)
        self.assertFalse(self.multiProcessing1.multiProcessingActRotClockwise)
        self.assertFalse(self.multiProcessing1.multiProcessingActRotCounterclockwise)
        self.assertEqual(self.multiProcessing1.actionDone, 1)
    
    def test_moveTurntableToConveyor_moving(self):
        # Simulate that the turntable is not yet at the conveyor position
        self.multiProcessing1.multiProcessingSensTurntablePosBelt = False

        self.multiProcessing1.moveTurntableToConveyor()

        self.assertTrue(self.multiProcessing1.multiProcessingActRotClockwise)
        self.assertFalse(self.multiProcessing1.multiProcessingActRotCounterclockwise)
        self.assertEqual(self.multiProcessing1.actionDone, 0)

    def test_moveTurntableToConveyor_reached(self):
        # Simulate that the turntable has reached the conveyor position
        self.multiProcessing1.multiProcessingSensTurntablePosBelt = True

        self.multiProcessing1.moveTurntableToConveyor()

        self.assertFalse(self.multiProcessing1.multiProcessingActRotClockwise)
        self.assertEqual(self.multiProcessing1.actionDone, 1)

    def test_moveTurntableToVacuum_moving(self):
        # Simulate that the turntable is not yet at the vacuum position
        self.multiProcessing1.multiProcessingSensTurntablePosVacuum = False

        self.multiProcessing1.moveTurntableToVacuum()

        self.assertTrue(self.multiProcessing1.multiProcessingActRotCounterclockwise)
        self.assertFalse(self.multiProcessing1.multiProcessingActRotClockwise)
        self.assertEqual(self.multiProcessing1.actionDone, 0)

    def test_moveTurntableToVacuum_reached(self):
        # Simulate that the turntable has reached the vacuum position
        self.multiProcessing1.multiProcessingSensTurntablePosVacuum = True

        self.multiProcessing1.moveTurntableToVacuum()

        self.assertFalse(self.multiProcessing1.multiProcessingActRotCounterclockwise)
        self.assertEqual(self.multiProcessing1.actionDone, 1)
    
    def test_useSaw_start(self):
        """Test if the saw starts correctly when the turntable is in the saw position"""
        # Simulate that the turntable is at the saw position
        self.multiProcessing1.multiProcessingSensTurntablePosSaw = True

        # Call the useSaw method
        self.multiProcessing1.useSaw()

        # Check that the saw is activated
        self.assertTrue(self.multiProcessing1.multiProcessingActSaw)
        # Check that sawCount is incremented
        self.assertEqual(self.multiProcessing1.sawCount, 1)
        # Check that actionDone is not incremented
        self.assertEqual(self.multiProcessing1.actionDone, 0)

    def test_useSaw_not_at_saw_position(self):
        """Test that the saw does not start when the turntable is not in the saw position"""
        # Simulate that the turntable is not at the saw position
        self.multiProcessing1.multiProcessingSensTurntablePosSaw = False

        # Call the useSaw method
        self.multiProcessing1.useSaw()

        # Check that the saw is not activated
        self.assertFalse(self.multiProcessing1.multiProcessingActSaw)
        # Check that sawCount is not incremented
        self.assertEqual(self.multiProcessing1.sawCount, 0)
        # Check that actionDone is incremented
        self.assertEqual(self.multiProcessing1.actionDone, 1)


    ###____________ Conveyor belt ______________
    def test_moveConveyorToEnd_moving(self):
        """Test that the conveyor continues moving when the package has not reached the light barrier"""
        # Simulate that the package has not reached the light barrier
        self.multiProcessing1.multiProcessingSensEndConveyor = True

        # Call the moveConveyorToEnd method
        self.multiProcessing1.moveConveyorToEnd()

        # Check that the conveyor is moving forward
        self.assertTrue(self.multiProcessing1.multiProcessingActConveyorForward)
        # Check that actionDone is not incremented
        self.assertEqual(self.multiProcessing1.actionDone, 0)

    def test_moveConveyorToEnd_reached_end(self):
        """Test that the conveyor stops when the package reaches the light barrier"""
        # Simulate that the package has reached the light barrier
        self.multiProcessing1.multiProcessingSensEndConveyor = False

        # Call the moveConveyorToEnd method
        self.multiProcessing1.moveConveyorToEnd()

        # Check that the conveyor is not moving forward
        self.assertFalse(self.multiProcessing1.multiProcessingActConveyorForward)
        # Check that actionDone is incremented
        self.assertEqual(self.multiProcessing1.actionDone, 1)


    ###____________ Oven _______________
    def test_heatProduct_initial(self):
        """Test heating product initially"""
       # Simulate that the package has reached the light barrier
        self.multiProcessing1.multiProcessingSensOvenFeederIn = True

        self.multiProcessing1.heatProduct()

        # Check that ovenCount is incremented
        self.assertEqual(self.multiProcessing1.ovenCount, 1)

        # Check that the oven light is toggled on
        self.assertTrue(self.multiProcessing1.multiProcessingOvenLight)

    def test_moveFeederIn_initial(self):
        """Test moving feeder inside the oven initially"""
        self.multiProcessing1.moveFeederIn()

        # Check that the compressor is activated
        self.assertTrue(self.multiProcessing1.multiProcessingCompressor)

        # Check that the oven door valve is activated
        self.assertTrue(self.multiProcessing1.multiProcessingValveOvenDoor)

        # Check that the oven is moving inward
        self.assertTrue(self.multiProcessing1.multiProcessingActOvenInward)

        # Check that actionDone is not incremented
        self.assertEqual(self.multiProcessing1.actionDone, 0)

    def test_moveFeederIn_completed(self):
        """Test moving feeder inside the oven when already in"""
        self.multiProcessing1.multiProcessingSensOvenFeederIn = True
        self.multiProcessing1.moveFeederIn()

        # Check that the oven movement is stopped
        self.assertFalse(self.multiProcessing1.multiProcessingActOvenInward)

        # Check that the compressor is deactivated
        self.assertFalse(self.multiProcessing1.multiProcessingCompressor)

        # Check that the oven door valve is deactivated
        self.assertFalse(self.multiProcessing1.multiProcessingValveOvenDoor)

        # Check that actionDone is incremented
        self.assertEqual(self.multiProcessing1.actionDone, 1)

    def test_moveFeederOut_initial(self):
        """Test moving feeder outside the oven initially"""
        self.multiProcessing1.moveFeederOut()

        # Check that the compressor is activated
        self.assertTrue(self.multiProcessing1.multiProcessingCompressor)

        # Check that the oven door valve is activated
        self.assertTrue(self.multiProcessing1.multiProcessingValveOvenDoor)

        # Check that the oven is moving outward
        self.assertTrue(self.multiProcessing1.multiProcessingActOvenOutward)

        # Check that actionDone is not incremented
        self.assertEqual(self.multiProcessing1.actionDone, 0)

    def test_moveFeederOut_completed(self):
        """Test moving feeder outside the oven when already out"""
        self.multiProcessing1.multiProcessingSensOvenFeederOut = True
        self.multiProcessing1.moveFeederOut()

        # Check that the oven movement is stopped
        self.assertFalse(self.multiProcessing1.multiProcessingActOvenOutward)

        # Check that the compressor is deactivated
        self.assertFalse(self.multiProcessing1.multiProcessingCompressor)

        # Check that the oven door valve is deactivated
        self.assertFalse(self.multiProcessing1.multiProcessingValveOvenDoor)

        # Check that actionDone is incremented
        self.assertEqual(self.multiProcessing1.actionDone, 1)        


    ###____________ Vacuum gripper _______________
    def test_moveVacuumToOven_initial(self):
        """Test moving vacuum gripper to the oven initially"""
        self.multiProcessing1.moveVacuumToOven()

        # Check that the gripper movement to the oven is activated
        self.assertTrue(self.multiProcessing1.multiProcessingActGripperToOven)

        # Check that actionDone is not incremented
        self.assertEqual(self.multiProcessing1.actionDone, 0)

    def test_moveVacuumToOven_completed(self):
        """Test moving vacuum gripper to the oven when already at the oven"""
        self.multiProcessing1.multiProcessingSensVacuumGripperAtOven = True
        self.multiProcessing1.moveVacuumToOven()

        # Check that the gripper movement to the oven is stopped
        self.assertFalse(self.multiProcessing1.multiProcessingActGripperToOven)

        # Check that actionDone is incremented
        self.assertEqual(self.multiProcessing1.actionDone, 1)

    def test_gripProduct_initial(self):
        """ Simulate the vacuum gripper inital action """
        ###other actions are not tested because the value of vacuumCount might change in the future
        self.multiProcessing1.multiProcessingSensVacuumGripperAtTurntable = True
        self.multiProcessing1.multiProcessingSensTurntablePosVacuum = True

        self.multiProcessing1.gripProduct()
        self.assertTrue(self.multiProcessing1.multiProcessingCompressor)
        self.assertTrue(self.multiProcessing1.multiProcessingActLowerValve)
        self.assertEqual(self.multiProcessing1.vacuumCount, 1)
        self.assertEqual(self.multiProcessing1.actionDone, 0)


if __name__ == '__main__':
    logging.basicConfig(format='%(levelname)-5s: %(module)-20s,%(lineno)-3s: %(message)s', level=logging.DEBUG)

    unittest.main()



