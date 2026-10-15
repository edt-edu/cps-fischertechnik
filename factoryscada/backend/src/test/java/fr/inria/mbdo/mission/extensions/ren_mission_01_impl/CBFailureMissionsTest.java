package fr.inria.mbdo.mission.extensions.ren_mission_01_impl;

import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinemissions.sortinglinenominalmission.SortingLineNominalMission;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippercbfailuremissions.vacuumgripper1cbfailuremission.VacuumGripper1CBFailureMission;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippercbfailuremissions.vacuumgripper2cbfailuremission.VacuumGripper2CBFailureMission;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.zonemissiondeadcb.ZoneMissionDeadCB;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.zonemissiondeadcb.ZoneMissionDeadCBActions;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.zonemissionmpsnominal.ZoneMissionMPSNominal;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.zonemissionmpsnominal.ZoneMissionMPSNominalActions;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.VacuumGripper2MissionTest.RecordingVacuumGripper;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.SortingLineAdapterImpl;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.ZoneAdapterImpl;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions.SortingLineNominalMissionActionsImpl;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions.VacuumGripper1CBFailureMissionActionsImpl;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions.VacuumGripper2CBFailureMissionActionsImpl;
import io.github.mbdo.factoryscada.core.MqttMessageRouter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

/**
 * With the conveyor belt failed, VGR1 asks the sorting line which colors wait at its outputs, then asks the dead CB zone
 * where to place each token; VGR2 asks the zone where to pick one, oldest first, and takes it to the MPS. The zone lets
 * one gripper at a time over the belt.
 */
class CBFailureMissionsTest {

    private static final String PICK_BLUE = "pickNamed SL_OUTPUT_BLUE";
    private static final String PLACE_FEED = "placeNamed CB";
    private static final String PLACE_SWAP = "placeNamed ALT_CB";
    private static final String PICK_FEED = "pickNamed CB";
    private static final String PICK_SWAP = "pickNamed ALT_CB";
    private static final String PLACE_MPS = "placeNamed MPS_INPUT";

    private RecordingVacuumGripper vgr1;
    private RecordingVacuumGripper vgr2;
    private SortingLineAdapterImpl sl;
    private SortingLineNominalMission slMission;
    private ZoneMissionDeadCB zoneMission;
    private VacuumGripper1CBFailureMission vgr1Mission;
    private VacuumGripper2CBFailureMission vgr2Mission;

    @BeforeEach
    void setUp() {
        vgr1 = new RecordingVacuumGripper();
        vgr2 = new RecordingVacuumGripper();
        sl = new SortingLineAdapterImpl("REN_MISSION_01/SL01", mock(MqttMessageRouter.class), "PLC/+/SortingLine/x/#");
        ZoneAdapterImpl zoneCB = new ZoneAdapterImpl("ZoneCB");
        ZoneAdapterImpl zoneMPS = new ZoneAdapterImpl("ZoneMPS");

        slMission = new SortingLineNominalMission(sl, vgr1, new SortingLineNominalMissionActionsImpl());
        zoneMission = new ZoneMissionDeadCB(zoneCB, new ZoneMissionDeadCBActions() { });
        ZoneMissionMPSNominal zoneMPSMission = new ZoneMissionMPSNominal(zoneMPS, new ZoneMissionMPSNominalActions() { });
        vgr1Mission = new VacuumGripper1CBFailureMission(vgr1, sl, zoneCB, new VacuumGripper1CBFailureMissionActionsImpl());
        vgr2Mission = new VacuumGripper2CBFailureMission(vgr2, zoneCB, zoneMPS,
            new VacuumGripper2CBFailureMissionActionsImpl());
        slMission.start();
        zoneMission.start();
        zoneMPSMission.start();
    }

    /**
     * A blue token waits at the sorting line output; VGR1 gets free (starts, or is back in standby), asks the sorting
     * line, and picks it.
     */
    private void vgr1PicksABlueToken() {
        sl.setSensor_SL_blue(false);
        if (vgr1Mission.isRunning()) {
            assertEquals("ReturnToStandbyCMD", vgr1Mission.getActiveStateName());
            vgr1.commandSucceeds(); // back in standby: asks the sorting line
        } else {
            vgr1Mission.startAt("AskSortingLine");
        }
        assertEquals(PICK_BLUE, vgr1.lastCommand());
        sl.setSensor_SL_blue(true);
        vgr1.commandSucceeds(); // picked: asks the zone where to place it
    }

    /** VGR1 places its token on the belt, retracts (the zone tells VGR2) and goes back to standby. */
    private void vgr1PlacesAndRetracts() {
        vgr1.commandSucceeds(); // placed: retract
        vgr1.commandSucceeds(); // retracted: tells the zone, goes to standby
        assertEquals("ReturnToStandbyCMD", vgr1Mission.getActiveStateName());
    }

    /** VGR2 picks its token on the belt and retracts (the zone tells VGR1), then waits for the MPS zone. */
    private void vgr2PicksAndRetracts() {
        vgr2.commandSucceeds(); // picked: retract
        vgr2.commandSucceeds(); // retracted: tells the zone, requests the MPS zone and places
    }

    @Test
    void vgr1AsksTheSortingLineWhichColorsWaitAndPicksTheFirst() {
        sl.setSensor_SL_white(false);
        sl.setSensor_SL_red(false);
        vgr1Mission.startAt("AskSortingLine");

        assertEquals(List.of("pickNamed SL_OUTPUT_WHITE"), vgr1.commands, "one pick, the other color is ignored");
        assertEquals("PickColorCMD", vgr1Mission.getActiveStateName());
    }

    @Test
    void vgr1WaitsWhenNoTokenWaitsAtTheSortingLine() {
        vgr1Mission.startAt("AskSortingLine");

        assertEquals("Idle", vgr1Mission.getActiveStateName());
        assertEquals(List.of(), vgr1.commands);
    }

    @Test
    void theSortingLineOnlyAnswersWhenIdleAndAnnouncesTheColorOfItsSort() {
        slMission.stop();
        slMission.startAt("SortCMD");
        vgr1Mission.startAt("AskSortingLine"); // the request is dropped: the sorting line sorts

        assertEquals("Idle", vgr1Mission.getActiveStateName());
        sl.setSensor_SL_blue(false); // the sort ends: the sorting line announces the color

        assertEquals(PICK_BLUE, vgr1.lastCommand());
        assertEquals("Idle", slMission.getActiveStateName());
    }

    @Test
    void vgr1PlacesOnTheSwapFirstThenTheFeedThenWaitsForAFreeSlot() {
        vgr1PicksABlueToken();
        assertEquals(PLACE_SWAP, vgr1.lastCommand());
        vgr1PlacesAndRetracts();
        assertEquals("TokenOnSwap", zoneMission.getActiveStateName());

        vgr1PicksABlueToken();
        assertEquals(PLACE_FEED, vgr1.lastCommand());
        vgr1PlacesAndRetracts();
        assertEquals("SwapThenFeed", zoneMission.getActiveStateName());

        vgr1PicksABlueToken();
        assertEquals("StandbyWithTokenCMD", vgr1Mission.getActiveStateName(), "both slots are taken");
        assertEquals("moveToSafePosition", vgr1.lastCommand(), "VGR1 waits in standby, holding its token");
        vgr1.commandSucceeds(); // in standby: asks again
        assertEquals("WaitForFreeCBSlot", vgr1Mission.getActiveStateName());

        vgr2Mission.startAt("CheckCB");
        assertEquals(PICK_SWAP, vgr2.lastCommand(), "the oldest token");
        vgr2PicksAndRetracts();

        assertEquals(PLACE_SWAP, vgr1.lastCommand(), "VGR1 is told once VGR2 is out of the belt");
        assertEquals("VGR1PlacingOnSwapAfterFeed", zoneMission.getActiveStateName());
    }

    @Test
    void vgr2IsToldWhenVgr1PlacedAToken() {
        vgr2Mission.startAt("CheckCB");
        assertEquals("Idle", vgr2Mission.getActiveStateName(), "nothing on the belt");

        vgr1PicksABlueToken();
        vgr1.commandSucceeds(); // placed: retract
        assertEquals(List.of(), vgr2.commands, "VGR1 is over the belt");

        vgr1.commandSucceeds(); // retracted: the zone tells VGR2

        assertEquals(List.of(PICK_SWAP), vgr2.commands);
        assertEquals("VGR2PickingLastToken", zoneMission.getActiveStateName());
    }

    @Test
    void oneGripperAtATimeOverTheBelt() {
        vgr1PicksABlueToken();
        vgr1PlacesAndRetracts(); // a token on the swap

        vgr2Mission.startAt("CheckCB");
        assertEquals(PICK_SWAP, vgr2.lastCommand());

        vgr1PicksABlueToken(); // VGR2 is over the belt
        assertEquals("StandbyWithTokenCMD", vgr1Mission.getActiveStateName());
        assertEquals("moveToSafePosition", vgr1.lastCommand(), "VGR1 waits in standby with its token");

        vgr2PicksAndRetracts(); // the zone tells VGR1, still moving to standby: dropped
        assertEquals("StandbyWithTokenCMD", vgr1Mission.getActiveStateName());

        vgr1.commandSucceeds(); // in standby: asks again
        assertEquals(PLACE_SWAP, vgr1.lastCommand());
    }

    @Test
    void refusedVgr1GoesToStandbyAndPlacesOnceToldASlotIsFree() {
        zoneMission.stop();
        zoneMission.startAt("SwapThenFeed"); // both slots hold a token
        vgr2Mission.startAt("CheckCB");      // VGR2 goes over the belt for the swap one
        assertEquals(PICK_SWAP, vgr2.lastCommand());

        vgr1PicksABlueToken();
        assertEquals(List.of(PICK_BLUE, "moveToSafePosition"), vgr1.commands, "no place, standby holding the token");
        vgr1.commandSucceeds(); // in standby: VGR2 still over the belt
        assertEquals("WaitForFreeCBSlot", vgr1Mission.getActiveStateName());

        vgr2PicksAndRetracts(); // out of the belt: the zone tells VGR1

        assertEquals(List.of(PICK_BLUE, "moveToSafePosition", PLACE_SWAP), vgr1.commands);
        assertEquals("VGR1PlacingOnSwapAfterFeed", zoneMission.getActiveStateName());
    }

    @Test
    void vgr2TakesTheTokensOldestFirst() {
        vgr1PicksABlueToken();
        vgr1PlacesAndRetracts(); // swap
        vgr2Mission.startAt("CheckCB");
        vgr2PicksAndRetracts(); // VGR2 takes the swap one: the belt is empty again
        assertEquals("Empty", zoneMission.getActiveStateName());

        vgr1PicksABlueToken();
        vgr1PlacesAndRetracts(); // swap again
        vgr1PicksABlueToken();
        assertEquals(PLACE_FEED, vgr1.lastCommand());
        vgr1PlacesAndRetracts();
        assertEquals("SwapThenFeed", zoneMission.getActiveStateName());

        vgr2BringsItsTokenToTheMpsAndComesBack();
        assertEquals(PICK_SWAP, vgr2.lastCommand(), "the swap one, placed first");
        vgr2PicksAndRetracts();
        vgr2BringsItsTokenToTheMpsAndComesBack();
        assertEquals(PICK_FEED, vgr2.lastCommand());
    }

    @Test
    void vgr2TakesTheTokenStuckOnTheFeedWhenTheBeltFailed() {
        zoneMission.stop();
        zoneMission.startAt("TokenOnFeed"); // migrated from the nominal CB zone with a token on the feed

        vgr2Mission.startAt("CheckCB");

        assertEquals(PICK_FEED, vgr2.lastCommand());
    }

    @Test
    void vgr2GoesToTheMpsRetractedWithoutTheSafePosition() {
        zoneMission.stop();
        zoneMission.startAt("TokenOnSwap");
        vgr2Mission.startAt("CheckCB");

        vgr2PicksAndRetracts();

        assertEquals(List.of(PICK_SWAP, "retractArm", PLACE_MPS), vgr2.commands);
        assertFalse(vgr2.commands.contains("moveToSafePosition"));
    }

    /** VGR2, after its pick on the belt, places the token at the MPS and comes back asking for the next one. */
    private void vgr2BringsItsTokenToTheMpsAndComesBack() {
        assertEquals(PLACE_MPS, vgr2.lastCommand());
        vgr2.commandSucceeds(); // placed: retract
        vgr2.commandSucceeds(); // retracted: release the MPS zone, standby
        vgr2.commandSucceeds(); // in standby: asks the zone for the next token
    }
}
