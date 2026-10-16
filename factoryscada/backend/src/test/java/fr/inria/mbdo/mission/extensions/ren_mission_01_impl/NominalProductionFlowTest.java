package fr.inria.mbdo.mission.extensions.ren_mission_01_impl;

import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltmissions.conveyorbeltnominalmission.ConveyorBeltNominalMission;
import fr.inria.mbdo.mission.extensions.ren_mission_01.multiprocessingstationmissions.multiprocessingstationnominalmission.MultiProcessingStationNominalMission;
import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinemissions.sortinglinenominalmission.SortingLineNominalMission;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippermissions.vacuumgripper1nominalmission.VacuumGripper1NominalMission;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippermissions.vacuumgripper2nominalmission.VacuumGripper2NominalMission;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.zonemissioncbnominal.ZoneMissionCBNominal;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.zonemissionmpsnominal.ZoneMissionMPSNominal;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.zonemissionmpsnominal.ZoneMissionMPSNominalActions;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.ConveyorBeltMissionTest.RecordingConveyorBelt;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.MultiProcessingStationMissionTest.RecordingMultiProcessingStation;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.VacuumGripper1SortingLinePullTest.SimulatedSortingLine;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.VacuumGripper2MissionTest.RecordingVacuumGripper;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.ZoneAdapterImpl;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions.ConveyorBeltNominalMissionActionsImpl;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions.MultiProcessingStationNominalMissionActionsImpl;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions.SortingLineNominalMissionActionsImpl;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions.VacuumGripper1NominalMissionActionsImpl;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions.VacuumGripper2NominalMissionActionsImpl;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions.ZoneMissionCBNominalActionsImpl;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The whole nominal production with all the nominal missions, five tokens and a slow MPS (its process lasts many
 * steps: the tokens wait on the swap for VGR2, the next one on the feed). The test plays the plant: it completes the
 * commands in a random order (one run per seed), applying their effect on the light barriers. The sorting line sorts
 * the tokens whenever it is free, whatever VGR1 does: tokens often wait at its outputs while VGR1 is busy (their push
 * dropped), VGR1 asks the sorting line for them once free. Whatever the order, VGR1 only picks where a token waits
 * (never twice the same token), never places a token on an occupied feed, the belt never brings a token to an occupied
 * swap, VGR2 never places on an occupied MPS input, and VGR1 and VGR2 are never over the belt at the same time; all the
 * tokens reach the MPS output.
 */
class NominalProductionFlowTest {

    private static final int TOKENS = 5;
    private static final List<String> COLORS = List.of("BLUE", "WHITE", "RED");
    /** The MPS process (6 s oven, 5 s saw) completes at the earliest this many steps after it started. */
    private static final int SLOW_MPS_STEPS = 25;

    /** A machine whose commands are completed by the plant, one at a time, in the order they were sent. */
    private abstract static class Simulated {
        int completed;

        abstract List<String> commands();

        boolean hasPendingCommand() {
            return completed < commands().size();
        }

        String pendingCommand() {
            return commands().get(completed);
        }

        String previousCommand() {
            return completed == 0 ? "" : commands().get(completed - 1);
        }
    }

    private final class Plant {
        final RecordingVacuumGripper vgr1 = new RecordingVacuumGripper();
        final RecordingVacuumGripper vgr2 = new RecordingVacuumGripper();
        final RecordingConveyorBelt cb = new RecordingConveyorBelt();
        final RecordingMultiProcessingStation mps = new RecordingMultiProcessingStation();
        final SimulatedSortingLine sortingLine = new SimulatedSortingLine();
        final SortingLineNominalMission slMission;

        final Simulated vgr1Commands = simulated(vgr1.commands);
        final Simulated vgr2Commands = simulated(vgr2.commands);
        final Simulated cbCommands = simulated(cb.commands);
        final Simulated mpsCommands = simulated(mps.commands);

        VacuumGripper1NominalMission vgr1Mission;
        VacuumGripper2NominalMission vgr2Mission;
        ZoneMissionCBNominal zoneCBMission;

        boolean feed;
        boolean swap;
        boolean mpsIn;
        boolean mpsOut;
        int tokensToSort = TOKENS;
        /** The token being sorted: 0 none, 1 at the input light barrier, 2 past it, on its way to its output. */
        int sortStage;
        String sortedColor;
        final Map<String, Boolean> tokenAtOutput = new HashMap<>(Map.of("BLUE", false, "WHITE", false,
                "RED", false));
        int picked;
        int sortedWhileVgr1Busy;
        int delivered;
        int processStartedAt = -1;
        int vgr1Refusals;
        final List<String> trace = new ArrayList<>();

        Plant() {
            ZoneAdapterImpl zoneCB = new ZoneAdapterImpl("ZoneCB");
            ZoneAdapterImpl zoneMPS = new ZoneAdapterImpl("ZoneMPS");
            cb.tokens(false, false);
            mps.setSensor_MPS_in(false);
            mps.setSensor_MPS_out(false);

            zoneCBMission = new ZoneMissionCBNominal(zoneCB, cb, new ZoneMissionCBNominalActionsImpl());
            ZoneMissionMPSNominal zoneMPSMission = new ZoneMissionMPSNominal(zoneMPS,
                    new ZoneMissionMPSNominalActions() { });
            ConveyorBeltNominalMission cbMission = new ConveyorBeltNominalMission(cb, vgr1, vgr2, zoneCB,
                    new ConveyorBeltNominalMissionActionsImpl());
            MultiProcessingStationNominalMission mpsMission = new MultiProcessingStationNominalMission(mps, zoneMPS,
                    new MultiProcessingStationNominalMissionActionsImpl());
            vgr1Mission = new VacuumGripper1NominalMission(vgr1, sortingLine, zoneCB,
                    new VacuumGripper1NominalMissionActionsImpl());
            vgr2Mission = new VacuumGripper2NominalMission(vgr2, cb, zoneCB, zoneMPS,
                    new VacuumGripper2NominalMissionActionsImpl());

            slMission = new SortingLineNominalMission(sortingLine, vgr1, new SortingLineNominalMissionActionsImpl());

            slMission.start();
            zoneCBMission.start();
            zoneMPSMission.start();
            cbMission.start();
            mpsMission.start();
            vgr1Mission.start();
            vgr2Mission.start();
            mpsCommands.completed = 1; // the MPS mission does not wait for its setup
        }

        boolean vgr1OverTheBelt() {
            return vgr1Commands.hasPendingCommand()
                    && (vgr1Commands.pendingCommand().equals("placeNamed CB")
                        || vgr1Commands.pendingCommand().equals("retractArm")
                           && vgr1Commands.previousCommand().equals("placeNamed CB"));
        }

        boolean vgr2OverTheBelt() {
            return vgr2Commands.hasPendingCommand()
                    && (vgr2Commands.pendingCommand().equals("pickNamed ALT_CB")
                        || vgr2Commands.pendingCommand().equals("retractArm")
                           && vgr2Commands.previousCommand().equals("pickNamed ALT_CB"));
        }

        void completeVgr1Command() {
            String command = vgr1Commands.pendingCommand();
            if (command.equals("moveToSafePosition") && vgr1Commands.previousCommand().startsWith("pickNamed")) {
                vgr1Refusals++; // refused the feed: VGR1 waits in standby, holding its token
            }
            vgr1Commands.completed++;
            trace.add("VGR1 " + command + " done");
            if (command.startsWith("pickNamed SL_OUTPUT_")) {
                String color = command.substring("pickNamed SL_OUTPUT_".length());
                assertTrue(tokenAtOutput.get(color), "VGR1 picked at an empty " + color + " output: " + trace);
                tokenAtOutput.put(color, false);
                setOutputBarrier(color, true);
                picked++;
            }
            if (command.equals("placeNamed CB")) {
                assertFalse(feed, "VGR1 stacked a token on the feed: " + trace);
                feed = true;
                cb.setConveyorSensFeed(false); // active-low: the belt may start moving it
            }
            vgr1.commandSucceeds();
        }

        void completeVgr2Command() {
            String command = vgr2Commands.pendingCommand();
            vgr2Commands.completed++;
            trace.add("VGR2 " + command + " done");
            if (command.equals("pickNamed ALT_CB")) {
                assertTrue(swap, "VGR2 picked at an empty swap: " + trace);
                swap = false;
                cb.setConveyorSensSwap(true); // the belt may move the token waiting on the feed
            }
            if (command.equals("placeNamed MPS_INPUT")) {
                assertFalse(mpsIn, "VGR2 stacked a token on the MPS input: " + trace);
                mpsIn = true;
                mps.setSensor_MPS_in(true);
            }
            vgr2.commandSucceeds();
        }

        void setOutputBarrier(String color, boolean value) {
            switch (color) {
                case "BLUE" -> sortingLine.setSensor_SL_blue(value);
                case "WHITE" -> sortingLine.setSensor_SL_white(value);
                default -> sortingLine.setSensor_SL_red(value);
            }
        }

        void completeCbCommand() {
            String command = cbCommands.pendingCommand();
            cbCommands.completed++;
            trace.add("CB " + command + " done");
            assertEquals("moveToSensor", command);
            assertTrue(feed, "the belt moved nothing: " + trace);
            assertFalse(swap, "the belt brought a token onto the one at the swap: " + trace);
            swap = true;
            cb.setConveyorSensSwap(false);
            feed = false;
            cb.setConveyorSensFeed(true);
            cb.commandSucceeds();
        }

        void completeMpsCommand() {
            String command = mpsCommands.pendingCommand();
            mpsCommands.completed++;
            trace.add("MPS " + command + " done");
            assertTrue(command.startsWith("process"), command);
            assertTrue(mpsIn, "the MPS processed nothing: " + trace);
            assertFalse(mpsOut, "the MPS delivered onto the previous payload: " + trace);
            mpsIn = false;
            mps.setSensor_MPS_in(false);
            mpsOut = true;
            mps.setSensor_MPS_out(true);
            delivered++;
            processStartedAt = -1;
            mps.commandSucceeds();
        }

        void run(Random random) {
            for (int step = 0; step < 20000; step++) {
                assertFalse(vgr1OverTheBelt() && vgr2OverTheBelt(), "VGR1 and VGR2 over the belt: " + trace);
                if (delivered == TOKENS && !vgr1Commands.hasPendingCommand() && !vgr2Commands.hasPendingCommand()
                        && !cbCommands.hasPendingCommand()) {
                    return;
                }
                if (mpsCommands.hasPendingCommand() && processStartedAt < 0) {
                    processStartedAt = step;
                }

                List<Runnable> possible = new ArrayList<>();
                // the sorting line sorts a token whenever it is free, whatever VGR1 does
                if (tokensToSort > 0 && sortStage == 0) {
                    possible.add(() -> {
                        tokensToSort--;
                        sortStage = 1;
                        sortedColor = COLORS.get(random.nextInt(COLORS.size()));
                        trace.add("SL " + sortedColor + " token at the input");
                        sortingLine.setSensor_SL_in(false);
                    });
                }
                if (sortStage == 1) {
                    possible.add(() -> {
                        sortStage = 2;
                        trace.add("SL token past the input");
                        sortingLine.setSensor_SL_in(true);
                    });
                }
                if (sortStage == 2 && !tokenAtOutput.get(sortedColor)) { // one token per output light barrier
                    possible.add(() -> {
                        sortStage = 0;
                        tokenAtOutput.put(sortedColor, true);
                        if (!"Idle".equals(vgr1Mission.getActiveStateName())) {
                            sortedWhileVgr1Busy++; // its push is dropped: VGR1 asks for it once free
                        }
                        trace.add("SL " + sortedColor + " token at its output");
                        setOutputBarrier(sortedColor, false);
                    });
                }
                if (vgr1Commands.hasPendingCommand()) {
                    possible.add(this::completeVgr1Command);
                }
                if (vgr2Commands.hasPendingCommand()) {
                    possible.add(this::completeVgr2Command);
                }
                if (cbCommands.hasPendingCommand()) {
                    possible.add(this::completeCbCommand);
                }
                if (mpsCommands.hasPendingCommand() && step - processStartedAt >= SLOW_MPS_STEPS) {
                    possible.add(this::completeMpsCommand);
                }
                if (mpsOut && delivered < TOKENS) {
                    possible.add(() -> { // the operator takes the payload at the MPS output
                        trace.add("MPS output cleared");
                        mpsOut = false;
                        mps.setSensor_MPS_out(false);
                    });
                }
                if (possible.isEmpty()) {
                    if (mpsCommands.hasPendingCommand()) {
                        continue; // the slow MPS is processing
                    }
                    fail("deadlock with " + delivered + " tokens delivered: SL " + slMission.getActiveStateName()
                            + ", outputs " + tokenAtOutput + ", VGR1 " + vgr1Mission.getActiveStateName()
                            + ", VGR2 " + vgr2Mission.getActiveStateName() + ", zone "
                            + zoneCBMission.getActiveStateName() + ": " + trace);
                }
                possible.get(random.nextInt(possible.size())).run();
            }
            fail("not finished: " + trace);
        }
    }

    private static Simulated simulated(List<String> commands) {
        return new Simulated() {
            @Override
            List<String> commands() {
                return commands;
            }
        };
    }

    @Test
    void fiveTokensWithASlowMpsAreNeverStackedOnTheFeedNorTheMpsInput() {
        int refusals = 0;
        int sortedWhileVgr1Busy = 0;
        for (long seed = 0; seed < 200; seed++) {
            Plant plant = new Plant();

            plant.run(new Random(seed));

            assertEquals(TOKENS, plant.delivered, "seed " + seed);
            assertFalse(plant.feed || plant.swap, "seed " + seed + ": tokens left on the belt");
            assertEquals("Idle", plant.vgr1Mission.getActiveStateName(), "seed " + seed);
            assertEquals(TOKENS, plant.picked, "seed " + seed + ": each token picked once");
            assertFalse(plant.tokenAtOutput.containsValue(true), "seed " + seed + ": tokens left at the sorting line");
            assertEquals("Idle", plant.slMission.getActiveStateName(), "seed " + seed);
            refusals += plant.vgr1Refusals;
            sortedWhileVgr1Busy += plant.sortedWhileVgr1Busy;
        }
        assertTrue(sortedWhileVgr1Busy > 0, "tokens were sorted faster than VGR1 cycles in some runs");
        assertTrue(refusals > 0, "VGR1 was refused the feed (a token still on it) in some runs, and went to standby");
    }
}
