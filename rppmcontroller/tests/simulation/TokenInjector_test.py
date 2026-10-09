import time
import unittest

from rppmcontroller.machine.conveyorbelt.ConveyorBelt import ConveyorBelt
from rppmcontroller.machine.highbay.HighBay import HighBay
from rppmcontroller.machine.multiprocessing.MultiProcessing import MultiProcessing
from rppmcontroller.machine.sortingLine.SortingLine import SortingLine
from rppmcontroller.simulation.TokenInjector import TokenInjector


class TokenInjectorTestCase(unittest.TestCase):

    def setUp(self):
        self.sortingLine = SortingLine("SortingLine01")
        self.secondSortingLine = SortingLine("SortingLine02")
        self.conveyorBelt = ConveyorBelt("ConveyorBelt01")
        machinesByType = {
            "SortingLine": [self.sortingLine, self.secondSortingLine],
            "ConveyorBelt": [self.conveyorBelt],
            "MultiProcessing": [MultiProcessing("MultiProcessing01")],
            "HighBay": [HighBay("HighBay01")],
        }
        # no MQTT server: requests are queued and applied directly
        self.injector = TokenInjector("test-plc", None, 1883, 60, machinesByType)

    def apply(self, **request):
        self.injector.pendingRequests.put(request)
        self.injector.applyPendingRequests()

    def test_catalogHasOneAliasPerLightBarrier(self):
        self.assertEqual(
            ["sl-input", "sl-middle", "sl-white", "sl-blue", "sl-red",
             "sl2-input", "sl2-middle", "sl2-white", "sl2-blue", "sl2-red",
             "cb-feed", "cb-swap", "mps-end", "hb-inside", "hb-outside", "mps-input"],
            list(self.injector.world.spots))
        self.assertIs(self.injector.world.trackBySpotAlias["sl2-input"].machine, self.secondSortingLine)

    def test_placeAndRemoveToggleTheActiveLowLightBarrier(self):
        self.apply(action="place", sensor="cb-feed")
        self.assertFalse(self.conveyorBelt.conveyorSensFeed)

        self.apply(action="remove", sensor="cb-feed")
        self.assertTrue(self.conveyorBelt.conveyorSensFeed)

    def test_sensorCanBeGivenAsMachineIdAndAttribute(self):
        self.apply(action="place", sensor="SortingLine01/sortingLineSensMiddleLightBarrier")
        self.assertFalse(self.sortingLine.sortingLineSensMiddleLightBarrier)

    def test_pulseRemovesTheTokenAfterItsDuration(self):
        self.apply(action="pulse", sensor="sl-blue", duration=0.05)
        self.assertFalse(self.sortingLine.sortingLineSensBlueLightBarrier)

        time.sleep(0.1)
        self.injector.applyPendingRequests()
        self.assertTrue(self.sortingLine.sortingLineSensBlueLightBarrier)
        self.assertEqual([], self.injector.world.tokens)

    def test_clearRemovesEveryToken(self):
        self.apply(action="place", sensor="sl-input", color="white")
        self.apply(action="pulse", sensor="cb-swap", duration=10)
        self.apply(action="clear")

        self.assertEqual([], self.injector.world.tokens)
        self.assertTrue(self.sortingLine.sortingLineSensInputLightBarrier)
        self.assertTrue(self.conveyorBelt.conveyorSensSwap)

    def test_invalidRequestsLeaveTheMachinesUntouched(self):
        for request in [dict(action="place", sensor="unknown"),
                        dict(action="place"),
                        dict(action="place", sensor="sl-input", color="green"),
                        dict(action="pulse", sensor="sl-input", duration=0),
                        dict(action="remove", sensor="sl-input"),
                        dict(action="jump", sensor="sl-input")]:
            self.apply(**request)

        self.assertEqual([], self.injector.world.tokens)


if __name__ == "__main__":
    unittest.main()
