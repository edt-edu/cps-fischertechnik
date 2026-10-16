package fr.inria.mbdo.mission.extensions.ren_mission_01_impl;

import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinemissions.sortinglinenominalmission.SortingLineNominalMission;
import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinesystem.sortingline.AbstractSortingLineMachineAdapter;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippermissions.vacuumgripper1nominalmission.VacuumGripper1NominalMission;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.PlaceOnFeedEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.VacuumGripper2MissionTest.RecordingVacuumGripper;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.ZoneAdapterImpl;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions.SortingLineNominalMissionActionsImpl;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions.VacuumGripper1NominalMissionActionsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * VGR1 pulls the sorted tokens: whenever it gets free (in standby after its setup, or back from the belt) it asks the
 * sorting line which colors wait at its outputs. The sorting line pushes the color at the end of each sort, but only a
 * VGR1 waiting in Idle gets it: the tokens sorted while VGR1 is busy are picked once it is back, one per ask, until the
 * sorting line answers that no token waits.
 */
class VacuumGripper1SortingLinePullTest {

    /** The sorting line light barriers (active-low), set by the test playing the plant. */
    static final class SimulatedSortingLine extends AbstractSortingLineMachineAdapter {
        final List<String> commands = new ArrayList<>();

        SimulatedSortingLine() {
            super("REN_MISSION_01/SL01");
            sensor_SL_in = true;
            sensor_SL_blue = true;
            sensor_SL_white = true;
            sensor_SL_red = true;
        }

        @Override public void eject() { commands.add("eject"); }
        @Override public void stop() { commands.add("stop"); }

        /** A token reaches the input light barrier, then passes it. */
        void tokenEntersAndPassesTheInput() {
            setSensor_SL_in(false);
            setSensor_SL_in(true);
        }
    }

    private static final String PICK_BLUE = "pickNamed SL_OUTPUT_BLUE";
    private static final String PICK_WHITE = "pickNamed SL_OUTPUT_WHITE";
    private static final String PICK_RED = "pickNamed SL_OUTPUT_RED";
    private static final String STANDBY = "moveToSafePosition";

    private RecordingVacuumGripper vgr1;
    private SimulatedSortingLine sl;
    private ZoneAdapterImpl zoneCB;
    private SortingLineNominalMission slMission;
    private VacuumGripper1NominalMission vgr1Mission;

    @BeforeEach
    void setUp() {
        vgr1 = new RecordingVacuumGripper();
        sl = new SimulatedSortingLine();
        zoneCB = new ZoneAdapterImpl("ZoneCB"); // the test plays the CB zone: it grants the feed
        slMission = new SortingLineNominalMission(sl, vgr1, new SortingLineNominalMissionActionsImpl());
        vgr1Mission = new VacuumGripper1NominalMission(vgr1, sl, zoneCB, new VacuumGripper1NominalMissionActionsImpl());
        slMission.start();
    }

    private void vgr1StartsAndReachesStandby() {
        vgr1Mission.start();
        assertEquals("setup", vgr1.lastCommand());
        vgr1.commandSucceeds();
        assertEquals(STANDBY, vgr1.lastCommand());
        vgr1.commandSucceeds(); // in standby: asks the sorting line
    }

    /** VGR1 lifts the token it picks (the light barrier sees no token any more): it then holds it. */
    private void vgr1CompletesItsPick(Runnable liftToken) {
        liftToken.run();
        vgr1.commandSucceeds(); // picked: asks the CB zone for the feed
        assertEquals("IdlePicked", vgr1Mission.getActiveStateName());
    }

    /** The CB zone grants the feed; VGR1 places, retracts, and goes back to standby, where it asks again. */
    private void vgr1PlacesAndGoesBackToStandby() {
        zoneCB.publish(new PlaceOnFeedEventMessage());
        vgr1.commandSucceeds(); // placed: retract
        vgr1.commandSucceeds(); // retracted: goes to standby
        assertEquals("ReturnToStandbyCMD", vgr1Mission.getActiveStateName());
    }

    private long picks(String pick) {
        return vgr1.commands.stream().filter(pick::equals).count();
    }

    @Test
    void tokensWaitingWhenVgr1StartsArePickedOnceInStandby() {
        sl.setSensor_SL_red(false); // a token waits at the red output, its push long gone

        vgr1StartsAndReachesStandby();

        assertEquals(PICK_RED, vgr1.lastCommand());
        assertEquals("PickColorCMD", vgr1Mission.getActiveStateName());
    }

    @Test
    void withNoTokenWaitingVgr1WaitsInIdleForTheNextSort() {
        vgr1StartsAndReachesStandby();
        assertEquals("Idle", vgr1Mission.getActiveStateName(), "NoTokenAvailable is ignored");
        assertEquals(List.of("setup", STANDBY), vgr1.commands);

        sl.tokenEntersAndPassesTheInput();
        assertEquals("SortCMD", slMission.getActiveStateName());
        sl.setSensor_SL_blue(false); // the sort ends: the sorting line pushes the color to VGR1, waiting in Idle

        assertEquals(PICK_BLUE, vgr1.lastCommand());
    }

    @Test
    void tokensSortedWhileVgr1IsBusyArePickedOneAfterAnotherOnceItIsBack() {
        vgr1StartsAndReachesStandby();
        sl.tokenEntersAndPassesTheInput();
        sl.setSensor_SL_blue(false);
        assertEquals(PICK_BLUE, vgr1.lastCommand());
        vgr1CompletesItsPick(() -> sl.setSensor_SL_blue(true));

        // VGR1 is busy at the belt: the pushes of these two sorts are dropped
        sl.tokenEntersAndPassesTheInput();
        sl.setSensor_SL_white(false);
        sl.tokenEntersAndPassesTheInput();
        sl.setSensor_SL_red(false);
        assertEquals("Idle", slMission.getActiveStateName());
        assertEquals(PICK_BLUE, vgr1.lastCommand(), "VGR1 is still busy with its blue token");

        vgr1PlacesAndGoesBackToStandby();
        vgr1.commandSucceeds(); // in standby: asks; the sorting line answers White and Red
        assertEquals(PICK_WHITE, vgr1.lastCommand(), "VGR1 takes the first color");
        assertEquals("PickColorCMD", vgr1Mission.getActiveStateName(), "Red was dropped while VGR1 picks");
        vgr1CompletesItsPick(() -> sl.setSensor_SL_white(true));

        vgr1PlacesAndGoesBackToStandby();
        vgr1.commandSucceeds(); // asks again: Red
        assertEquals(PICK_RED, vgr1.lastCommand());
        vgr1CompletesItsPick(() -> sl.setSensor_SL_red(true));

        vgr1PlacesAndGoesBackToStandby();
        vgr1.commandSucceeds(); // asks again: NoTokenAvailable
        assertEquals("Idle", vgr1Mission.getActiveStateName());
        assertEquals(1, picks(PICK_BLUE));
        assertEquals(1, picks(PICK_WHITE));
        assertEquals(1, picks(PICK_RED));
    }

    @Test
    void aTokenReachingItsOutputAfterTheSortEndedOnAWaitingTokenIsAnnouncedToo() {
        vgr1StartsAndReachesStandby();
        sl.tokenEntersAndPassesTheInput();
        sl.setSensor_SL_red(false);
        assertEquals(PICK_RED, vgr1.lastCommand()); // VGR1 picks the red token...

        sl.tokenEntersAndPassesTheInput(); // ...when the next sort starts: it ends on the red token still there
        assertEquals("Idle", slMission.getActiveStateName(), "the sorted token has not reached its output yet");

        vgr1CompletesItsPick(() -> sl.setSensor_SL_red(true));
        vgr1PlacesAndGoesBackToStandby();
        vgr1.commandSucceeds(); // asks: no token waits yet
        assertEquals("Idle", vgr1Mission.getActiveStateName());

        sl.setSensor_SL_blue(false); // the token reaches its output, the sorting line idle: it announces it

        assertEquals(PICK_BLUE, vgr1.lastCommand());
        assertEquals(1, picks(PICK_RED));
    }

    @Test
    void vgr1AskingWhileTheSortingLineSortsGetsTheSortedToken() {
        vgr1StartsAndReachesStandby();
        sl.tokenEntersAndPassesTheInput();
        sl.setSensor_SL_white(false);
        vgr1CompletesItsPick(() -> sl.setSensor_SL_white(true));
        vgr1PlacesAndGoesBackToStandby();

        sl.tokenEntersAndPassesTheInput(); // the sorting line sorts the next token...
        assertEquals("SortCMD", slMission.getActiveStateName());
        vgr1.commandSucceeds(); // ...when VGR1 is back in standby: its request is dropped
        assertEquals("Idle", vgr1Mission.getActiveStateName());
        assertEquals("SortCMD", slMission.getActiveStateName());

        sl.setSensor_SL_red(false); // the sort ends: the push reaches VGR1, waiting in Idle

        assertEquals(PICK_RED, vgr1.lastCommand());
        assertEquals(List.of("setup", STANDBY, PICK_WHITE, "placeNamed CB", "retractArm", STANDBY, PICK_RED),
                vgr1.commands);
    }
}
