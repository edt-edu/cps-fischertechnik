package fr.inria.mbdo.mission.extensions.ren_mission_01_impl;

import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltmissions.conveyorbeltnominalmission.ConveyorBeltNominalMission;
import fr.inria.mbdo.mission.extensions.ren_mission_01.multiprocessingstationmissions.multiprocessingstationnominalmission.MultiProcessingStationNominalMission;
import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinesystem.sortinglinemessages.RedTokenAvailableEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippermissions.vacuumgripper1nominalmission.VacuumGripper1NominalMission;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippermissions.vacuumgripper2nominalmission.VacuumGripper2NominalMission;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.zonemissioncbnominal.ZoneMissionCBNominal;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.zonemissionmpsnominal.ZoneMissionMPSNominal;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.zonemissionmpsnominal.ZoneMissionMPSNominalActions;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.ConveyorBeltMissionTest.RecordingConveyorBelt;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.MultiProcessingStationMissionTest.RecordingMultiProcessingStation;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.VacuumGripper1MissionTest.StubSortingLine;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.VacuumGripper2MissionTest.RecordingVacuumGripper;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.ZoneAdapterImpl;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions.ConveyorBeltNominalMissionActionsImpl;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions.MultiProcessingStationNominalMissionActionsImpl;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions.VacuumGripper1NominalMissionActionsImpl;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions.VacuumGripper2NominalMissionActionsImpl;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions.ZoneMissionCBNominalActionsImpl;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The whole nominal production with all the nominal missions, three tokens and a slow MPS (its process lasts many
 * steps: the tokens wait on the swap for VGR2, the next one on the feed). The test plays the plant: it completes the
 * commands in a random order (one run per seed), applying their effect on the light barriers. Whatever the order, VGR1
 * never places a token on an occupied feed, the belt never brings a token to an occupied swap, VGR2 never places on an
 * occupied MPS input, and VGR1 and VGR2 are never over the belt at the same time; all the tokens reach the MPS output.
 */
class NominalProductionFlowTest {

    private static final int TOKENS = 3;
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
        final StubSortingLine sortingLine = new StubSortingLine();

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
            for (int step = 0; step < 5000; step++) {
                assertFalse(vgr1OverTheBelt() && vgr2OverTheBelt(), "VGR1 and VGR2 over the belt: " + trace);
                if (delivered == TOKENS && !vgr1Commands.hasPendingCommand() && !vgr2Commands.hasPendingCommand()
                        && !cbCommands.hasPendingCommand()) {
                    return;
                }
                if (mpsCommands.hasPendingCommand() && processStartedAt < 0) {
                    processStartedAt = step;
                }

                List<Runnable> possible = new ArrayList<>();
                if (tokensToSort > 0 && "Idle".equals(vgr1Mission.getActiveStateName())) {
                    possible.add(() -> {
                        tokensToSort--;
                        trace.add("SL red token");
                        sortingLine.publish(new RedTokenAvailableEventMessage());
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
                    fail("deadlock with " + delivered + " tokens delivered: VGR1 " + vgr1Mission.getActiveStateName()
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
    void threeTokensWithASlowMpsAreNeverStackedOnTheFeedNorTheMpsInput() {
        int refusals = 0;
        for (long seed = 0; seed < 200; seed++) {
            Plant plant = new Plant();

            plant.run(new Random(seed));

            assertEquals(TOKENS, plant.delivered, "seed " + seed);
            assertFalse(plant.feed || plant.swap, "seed " + seed + ": tokens left on the belt");
            assertEquals("Idle", plant.vgr1Mission.getActiveStateName(), "seed " + seed);
            refusals += plant.vgr1Refusals;
        }
        assertTrue(refusals > 0, "VGR1 was refused the feed (a token still on it) in some runs, and went to standby");
    }
}
