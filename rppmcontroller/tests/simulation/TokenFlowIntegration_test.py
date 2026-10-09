import os
import unittest

import tests.controllerTestHelper as ctHelper
from rppmcontroller.RevPiPyMachineController import RevPiPyMachineController
from rppmcontroller.machine.Color import Color
from rppmcontroller.machine.Direction import Direction
from rppmcontroller.machine.NamedPosition import NamedPosition
from rppmcontroller.machine.Position import Position
from rppmcontroller.machine.vacuumgripper.VacuumGripperParameters import VacuumGripperParameters
from rppmcontroller.machine.conveyorbelt.ConveyorBelt import ConveyorBelt
from rppmcontroller.machine.conveyorbelt.ConveyorBeltSimpleSimulator import ConveyorBeltSimpleSimulator
from rppmcontroller.machine.sortingLine.SortingLine import SortingLine
from rppmcontroller.machine.sortingLine.SortingLineSimpleSimulator import SortingLineSimpleSimulator
from rppmcontroller.machine.vacuumgripper.VacuumGripper import VacuumGripper
from rppmcontroller.machine.vacuumgripper.VacuumGripperSimpleSimulator import VacuumGripperSimpleSimulator
from rppmcontroller.protocol.MachineCommand import MachineCommand
from rppmcontroller.simulation.TokenInjector import TokenInjector


class TokenFlowController(RevPiPyMachineController):
    """SortingLine, ConveyorBelt and VacuumGripper simulators with the TokenInjector wired like
    SimulatedMachines_Controller (same VGR encoder increment)."""

    def __init__(self, configurationFile):
        super().__init__(configurationFile=configurationFile)
        self.sortingLine = SortingLine("SortingLine01")
        self.conveyorBelt = ConveyorBelt("ConveyorBelt01")
        # calibrated like the VIRTUAL_1C1H1M1S2V_01 PLC1 config (REN_1S1V_1C1V_1H1M_02 RevPi01 named positions)
        self.vacuumGripper = VacuumGripper("VacuumGripper01", VacuumGripperParameters(named_positions={
            "SL_OUTPUT_BLUE": Position(meaning="any", vertical=1400, rot=2135, horizontal=1500),
            "CB": Position(meaning="any", vertical=1200, rot=1870, horizontal=1460),
        }))
        self.vacuumGripperSimulator = VacuumGripperSimpleSimulator(controlledVacuumGripper=self.vacuumGripper,
                                                                   encoderIncrement=33)
        self.machines = [self.sortingLine, self.conveyorBelt, self.vacuumGripper]
        self.simulators = [SortingLineSimpleSimulator(self.sortingLine), ConveyorBeltSimpleSimulator(self.conveyorBelt),
                           self.vacuumGripperSimulator]
        self.currentlyExecuting = {m: None for m in self.machines}
        self.machineFeedback = {m: None for m in self.machines}
        self.commandFeedback = {m: None for m in self.machines}
        self.tokenInjector = TokenInjector(self.plcId, None, 1883, 60,
                                           {"SortingLine": [self.sortingLine], "ConveyorBelt": [self.conveyorBelt],
                                            "VacuumGripper": [self.vacuumGripper]})

    def read(self):
        self.tokenInjector.applyPendingRequests()
        for simulator in self.simulators:
            simulator.simulatedRead()

    def write(self):
        self.tokenInjector.simulateMovement()
        for simulator in self.simulators:
            simulator.simulatedWrite()

    def reset(self):
        if self.vacuumGripper.arm_reset_helper.must_reset():
            self.vacuumGripperSimulator.simulatedArmReset()
        if self.vacuumGripper.rot_reset_helper.must_reset():
            self.vacuumGripperSimulator.simulatedRotationReset()
        if self.vacuumGripper.vertical_reset_helper.must_reset():
            self.vacuumGripperSimulator.simulatedVerticalReset()


class TokenFlowIntegrationTestCase(unittest.TestCase):
    """Runs the real machine commands in the controller main loop (real time, 0.1 s cycles)."""

    def setUp(self):
        self.controller = TokenFlowController(os.path.join(os.path.dirname(__file__), "..", "example", "config.yml"))
        self.controller.mainLoopDelay = 0.1
        self.world = self.controller.tokenInjector.world
        ctHelper.clearPendingNotifications()

    def place(self, sensor, color=None):
        self.controller.tokenInjector.pendingRequests.put({"action": "place", "sensor": sensor, "color": color})

    def runUntilCommandDone(self, machineId, maxCycles):
        for _ in range(maxCycles):
            self.controller.mainLoopIteration()
            notification = ctHelper.readCommandFeedbackNotification(self.controller)
            if machineId in notification and " DONE" in notification:
                return
            self.assertNotIn("ABORTED", notification)
        self.fail(f"{machineId} command not done in {maxCycles} cycles")

    def test_ejectAutoSortsTheTokenIntoTheChuteOfItsColor(self):
        for color, chute in [("WHITE", "sl-white"), ("RED", "sl-red"), ("BLUE", "sl-blue")]:
            with self.subTest(color=color):
                self.world.clear()
                self.place("sl-input", color)
                ctHelper.sendMessage(self.controller, "SortingLine01",
                                     MachineCommand("COMMAND", "SORTING", 1, "EJECT", [Color.AUTO]))

                self.runUntilCommandDone("SortingLine01", maxCycles=60)

                self.assertEqual(Color[color], self.controller.sortingLine.colorToEject)
                self.assertEqual([chute], [token.spot.alias for token in self.world.tokens if token.spot])
                self.assertFalse(getattr(self.controller.sortingLine,
                                         f"sortingLineSens{color.capitalize()}LightBarrier"))
                self.assertTrue(self.controller.sortingLine.sortingLineSensInputLightBarrier)
                self.assertTrue(self.controller.sortingLine.sortingLineSensMiddleLightBarrier)

    def test_moveToSensorForwardCarriesTheTokenFromFeedToSwap(self):
        self.place("cb-feed")
        ctHelper.sendMessage(self.controller, "ConveyorBelt01",
                             MachineCommand("COMMAND", "CONVEYOR", 1, "MOVE_TO_SENSOR", [Direction.FORWARD]))

        self.runUntilCommandDone("ConveyorBelt01", maxCycles=60)

        self.assertEqual(["cb-swap"], [token.spot.alias for token in self.world.tokens])
        self.assertTrue(self.controller.conveyorBelt.conveyorSensFeed)
        self.assertFalse(self.controller.conveyorBelt.conveyorSensSwap)

    def test_moveOutForwardDropsTheTokenOffTheSwapEnd(self):
        self.place("cb-feed")
        ctHelper.sendMessage(self.controller, "ConveyorBelt01",
                             MachineCommand("COMMAND", "CONVEYOR", 1, "MOVE_OUT", [Direction.FORWARD]))

        self.runUntilCommandDone("ConveyorBelt01", maxCycles=80)

        self.assertEqual([], self.world.tokens)


    def test_vgrPicksTheTokenFromTheBlueChuteAndPlacesItOnTheConveyorFeed(self):
        # positions sent by the SCADA mission (Position(meaning, vertical, rot, horizontal))
        slOutputBlue = Position("OTHER", 1400, 2135, 1500)
        cbFeed = Position("OTHER", 1200, 1870, 1460)  # the gripper's calibrated "CB" named position
        self.place("sl-blue", "BLUE")

        ctHelper.sendMessage(self.controller, "VacuumGripper01", MachineCommand("COMMAND", "VACUUM", 1, "setup", []))
        self.runUntilCommandDone("VacuumGripper01", maxCycles=200)

        ctHelper.sendMessage(self.controller, "VacuumGripper01", MachineCommand("COMMAND", "VACUUM", 2, "pick", [slOutputBlue]))
        self.runUntilCommandDone("VacuumGripper01", maxCycles=400)

        token = self.world.tokens[0]
        self.assertEqual("vgr1-gripper", token.track.name)
        self.assertTrue(self.controller.sortingLine.sortingLineSensBlueLightBarrier, "blue chute is free again")

        ctHelper.sendMessage(self.controller, "VacuumGripper01", MachineCommand("COMMAND", "VACUUM", 3, "place", [cbFeed]))
        self.runUntilCommandDone("VacuumGripper01", maxCycles=400)

        self.assertEqual("cb-feed", token.spot.alias)
        self.assertFalse(self.controller.conveyorBelt.conveyorSensFeed)


    def test_vgrPicksAndPlacesAtNamedPositions(self):
        self.place("sl-blue", "BLUE")

        ctHelper.sendMessage(self.controller, "VacuumGripper01", MachineCommand("COMMAND", "VACUUM", 1, "setup", []))
        self.runUntilCommandDone("VacuumGripper01", maxCycles=200)

        ctHelper.sendMessage(self.controller, "VacuumGripper01",
                             MachineCommand("COMMAND", "VACUUM", 2, "pick", [NamedPosition("SL_OUTPUT_BLUE")]))
        self.runUntilCommandDone("VacuumGripper01", maxCycles=400)
        token = self.world.tokens[0]
        self.assertEqual("vgr1-gripper", token.track.name)

        ctHelper.sendMessage(self.controller, "VacuumGripper01",
                             MachineCommand("COMMAND", "VACUUM", 3, "place", [NamedPosition("CB")]))
        self.runUntilCommandDone("VacuumGripper01", maxCycles=400)
        self.assertEqual("cb-feed", token.spot.alias)


if __name__ == "__main__":
    unittest.main()
