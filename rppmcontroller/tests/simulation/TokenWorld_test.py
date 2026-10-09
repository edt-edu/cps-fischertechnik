import unittest

from rppmcontroller.machine.conveyorbelt.ConveyorBelt import ConveyorBelt
from rppmcontroller.machine.sortingLine.SortingLine import SortingLine
from rppmcontroller.machine.vacuumgripper.VacuumGripper import VacuumGripper
from rppmcontroller.simulation.TokenWorld import CB_LENGTH, TokenWorld

CYCLE = 0.1


class TokenWorldTestCase(unittest.TestCase):
    """Moves tokens by hand-set actuators, one 0.1 s cycle at a time."""

    def setUp(self):
        self.sortingLine = SortingLine("SortingLine01")
        self.conveyorBelt = ConveyorBelt("ConveyorBelt01")
        self.vacuumGripper = VacuumGripper("VacuumGripper01")
        self.world = TokenWorld({"SortingLine": [self.sortingLine], "ConveyorBelt": [self.conveyorBelt],
                                 "VacuumGripper": [self.vacuumGripper]})

    def run_cycles(self, count):
        for _ in range(count):
            self.world.step(CYCLE)

    # ------------------------------------------------------------ SortingLine

    def test_tokenStaysPutWhileTheMotorIsOff(self):
        token = self.world.place(self.world.spots["sl-input"], "RED")
        self.run_cycles(20)
        self.assertEqual(0.0, token.position)
        self.assertFalse(self.sortingLine.sortingLineSensInputLightBarrier)

    def test_runningBeltCarriesTheTokenPastTheColorSensorToTheMiddleBarrier(self):
        self.world.place(self.world.spots["sl-input"], "BLUE")
        self.sortingLine.sortingLineActMotorConveyor = True

        self.run_cycles(3)
        self.assertTrue(self.sortingLine.sortingLineSensInputLightBarrier, "token left the input")
        self.assertTrue(self.sortingLine.sortingLineSensColorDetector)
        self.assertTrue(self.sortingLine.sortingLineSensBlueDetector)
        self.assertFalse(self.sortingLine.sortingLineSensRedDetector)

        self.run_cycles(4)
        self.assertFalse(self.sortingLine.sortingLineSensColorDetector, "token left the color sensor")
        self.assertFalse(self.sortingLine.sortingLineSensMiddleLightBarrier, "token at the middle barrier")

    def test_ejectorPushesTheTokenInFrontOfItIntoItsChute(self):
        token = self.world.place(self.world.spots["sl-input"], "RED")
        track = token.track
        token.position = track.ejectorPositions["RED"]

        self.sortingLine.sortingLineActRedEjector = True
        self.run_cycles(1)

        self.assertEqual("sl-red", token.spot.alias)
        self.assertFalse(self.sortingLine.sortingLineSensRedLightBarrier)

        # the chute doesn't move with the belt
        self.sortingLine.sortingLineActRedEjector = False
        self.sortingLine.sortingLineActMotorConveyor = True
        self.run_cycles(20)
        self.assertEqual("sl-red", token.spot.alias)

    def test_ejectorFiringAwayFromTheTokenMissesIt(self):
        token = self.world.place(self.world.spots["sl-input"], "RED")
        token.position = token.track.ejectorPositions["WHITE"]

        self.sortingLine.sortingLineActRedEjector = True
        self.run_cycles(1)

        self.assertIsNone(token.spot)
        self.assertIn(token, self.world.tokens)

    def test_tokenFallsOffTheEndOfTheSortingLine(self):
        self.world.place(self.world.spots["sl-input"], None)
        self.sortingLine.sortingLineActMotorConveyor = True
        self.run_cycles(60)
        self.assertEqual([], self.world.tokens)

    # ------------------------------------------------------------ ConveyorBelt

    def test_forwardCarriesTheTokenFromFeedToSwap(self):
        token = self.world.place(self.world.spots["cb-feed"], None)
        self.conveyorBelt.conveyorActForward = True

        self.run_cycles(2)
        self.assertTrue(self.conveyorBelt.conveyorSensFeed, "token left the feed")

        self.run_cycles(int(CB_LENGTH / CYCLE) - 2)
        self.assertEqual("cb-swap", token.spot.alias)
        self.assertFalse(self.conveyorBelt.conveyorSensSwap)

    def test_backwardCarriesTheTokenFromSwapToFeed(self):
        token = self.world.place(self.world.spots["cb-swap"], None)
        self.conveyorBelt.conveyorActBackward = True
        self.run_cycles(int(CB_LENGTH / CYCLE))
        self.assertEqual("cb-feed", token.spot.alias)

    def test_bothDirectionsCancelEachOther(self):
        token = self.world.place(self.world.spots["cb-feed"], None)
        self.conveyorBelt.conveyorActForward = True
        self.conveyorBelt.conveyorActBackward = True
        self.run_cycles(10)
        self.assertEqual(0.0, token.position)

    def test_tokenFallsOffPastTheSwapEnd(self):
        self.world.place(self.world.spots["cb-swap"], None)
        self.conveyorBelt.conveyorActForward = True
        self.run_cycles(10)
        self.assertEqual([], self.world.tokens)
        self.assertTrue(self.conveyorBelt.conveyorSensSwap)


    # ------------------------------------------------------------ VacuumGripper

    def moveGripperTo(self, rotation, arm):
        self.vacuumGripper.vacuumSensRotEncoderCounter = rotation
        self.vacuumGripper.vacuumSensArmEncoderCounter = arm

    def setVacuum(self, on):
        self.vacuumGripper.vacuumActValve = on
        self.vacuumGripper.vacuumActCompressorOn = on
        self.run_cycles(1)

    def test_vacuumOverASlotPicksItsTokenAndReleasingOverAnotherSlotPlacesIt(self):
        token = self.world.place(self.world.spots["sl-red"], "RED")

        self.moveGripperTo(2270 + 10, 900 - 10)
        self.setVacuum(True)
        self.assertEqual("vgr1-gripper", token.track.name)
        self.assertTrue(self.sortingLine.sortingLineSensRedLightBarrier)

        self.moveGripperTo(1870, 1400)
        self.setVacuum(False)
        self.assertEqual("cb-feed", token.spot.alias)
        self.assertFalse(self.conveyorBelt.conveyorSensFeed)

    def test_vacuumAwayFromTheTokenPicksNothing(self):
        token = self.world.place(self.world.spots["sl-red"], "RED")
        self.moveGripperTo(2135, 1500)  # over the blue chute
        self.setVacuum(True)
        self.assertEqual("sl-red", token.spot.alias)

    def test_releasingOutsideAnySlotDropsTheToken(self):
        self.world.place(self.world.spots["sl-white"], "WHITE")
        self.moveGripperTo(2400, 500)
        self.setVacuum(True)
        self.moveGripperTo(0, 0)
        self.setVacuum(False)
        self.assertEqual([], self.world.tokens)


    def test_gripperSlotsFollowItsNamedPositions(self):
        from rppmcontroller.machine.Position import Position
        from rppmcontroller.machine.vacuumgripper.VacuumGripperParameters import VacuumGripperParameters
        calibrated = VacuumGripper("VacuumGripper01", VacuumGripperParameters(named_positions={
            "SL_OUTPUT_RED": Position(meaning="any", vertical=1400, rot=2270, horizontal=970),
            "CB": Position(meaning="any", vertical=1200, rot=1870, horizontal=1460),
        }))
        world = TokenWorld({"SortingLine": [SortingLine("SortingLine01")], "ConveyorBelt": [ConveyorBelt("ConveyorBelt01")],
                            "VacuumGripper": [calibrated]})
        token = world.place(world.spots["sl-red"], "RED")

        calibrated.vacuumSensRotEncoderCounter, calibrated.vacuumSensArmEncoderCounter = 2270, 970
        calibrated.vacuumActValve = calibrated.vacuumActCompressorOn = True
        world.step(CYCLE)
        self.assertEqual("vgr1-gripper", token.track.name)

        calibrated.vacuumSensRotEncoderCounter, calibrated.vacuumSensArmEncoderCounter = 1870, 1460
        calibrated.vacuumActValve = calibrated.vacuumActCompressorOn = False
        world.step(CYCLE)
        self.assertEqual("cb-feed", token.spot.alias)

    def test_secondGripperPlacingAtTheMpsInputBreaksTheOvenLightBarrier(self):
        from rppmcontroller.machine.multiprocessing.MultiProcessing import MultiProcessing
        multiProcessing = MultiProcessing("MultiProcessing01")
        vgr1, vgr2 = VacuumGripper("VacuumGripper01"), VacuumGripper("VacuumGripper02")
        world = TokenWorld({"ConveyorBelt": [ConveyorBelt("ConveyorBelt01")], "MultiProcessing": [multiProcessing],
                            "VacuumGripper": [vgr1, vgr2]})
        token = world.place(world.spots["cb-swap"], None)
        self.assertTrue(multiProcessing.multiProcessingSensOven)

        vgr2.vacuumSensRotEncoderCounter, vgr2.vacuumSensArmEncoderCounter = 1155, 1810  # over the CB swap
        vgr2.vacuumActValve = vgr2.vacuumActCompressorOn = True
        world.step(CYCLE)
        vgr2.vacuumSensRotEncoderCounter, vgr2.vacuumSensArmEncoderCounter = 2050, 1890  # over the MPS input
        vgr2.vacuumActValve = vgr2.vacuumActCompressorOn = False
        world.step(CYCLE)

        self.assertEqual("mps-input", token.spot.alias)
        self.assertFalse(multiProcessing.multiProcessingSensOven, "light barriers are active-low")


if __name__ == "__main__":
    unittest.main()
