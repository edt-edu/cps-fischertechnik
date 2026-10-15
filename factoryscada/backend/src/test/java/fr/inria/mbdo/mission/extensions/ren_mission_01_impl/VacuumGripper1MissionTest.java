package fr.inria.mbdo.mission.extensions.ren_mission_01_impl;

import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltsystem.conveyorbeltmessages.FeedFreeEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinesystem.sortingline.AbstractSortingLineMachineAdapter;
import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinesystem.sortinglinemessages.RedTokenAvailableEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippermissions.vacuumgripper1nominalmission.VacuumGripper1NominalMission;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.zonemissioncbnominal.ZoneMissionCBNominal;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.CBTokenRequestEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.TokenPickedFromCBEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.ConveyorBeltMissionTest.RecordingConveyorBelt;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.ZoneAdapterImpl;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions.VacuumGripper1NominalMissionActionsImpl;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions.ZoneMissionCBNominalActionsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * VGR1 picks a sorted token at its chute and asks the CB zone for the conveyor belt feed. Granted (the feed is free and
 * VGR2 is not over the belt), it places the token there, retracts its arm, tells the zone, then goes to the safe
 * position. Refused, it waits in standby, holding its token, until the zone tells it the feed is usable: it never
 * stacks a token on the feed.
 */
class VacuumGripper1MissionTest {

    /** The sorting line only publishes the token messages here. */
    static final class StubSortingLine extends AbstractSortingLineMachineAdapter {
        StubSortingLine() {
            super("REN_MISSION_01/SL01");
        }

        @Override public void eject() { }
        @Override public void stop() { }
    }

    // named positions of VGR1 on the PLC
    private static final String PICK_RED = "pickNamed SL_OUTPUT_RED";
    private static final String PLACE_CB_FEED = "placeNamed CB";
    private static final String STANDBY = "moveToSafePosition";

    private VacuumGripper2MissionTest.RecordingVacuumGripper vgr1;
    private StubSortingLine sortingLine;
    private RecordingConveyorBelt cb;
    private ZoneAdapterImpl zoneCB;
    private ZoneMissionCBNominal zoneCBMission;
    private VacuumGripper1NominalMission vgr1Mission;

    @BeforeEach
    void setUp() {
        vgr1 = new VacuumGripper2MissionTest.RecordingVacuumGripper();
        sortingLine = new StubSortingLine();
        cb = new RecordingConveyorBelt();
        zoneCB = new ZoneAdapterImpl("ZoneCB");
        zoneCBMission = new ZoneMissionCBNominal(zoneCB, cb, new ZoneMissionCBNominalActionsImpl());
        vgr1Mission = new VacuumGripper1NominalMission(vgr1, sortingLine, zoneCB,
                new VacuumGripper1NominalMissionActionsImpl());
        vgr1Mission.start();

        assertEquals("setup", vgr1.lastCommand());
        vgr1.commandSucceeds();
        assertEquals(STANDBY, vgr1.lastCommand());
        vgr1.commandSucceeds();
        assertEquals("Idle", vgr1Mission.getActiveStateName());
    }

    /** VGR1 picks a red token; once picked, it asks the CB zone for the feed. */
    private void vgr1PicksARedToken() {
        sortingLine.publish(new RedTokenAvailableEventMessage());
        assertEquals(PICK_RED, vgr1.lastCommand());
        vgr1.commandSucceeds();
    }

    @Test
    void afterThePlaceTheArmRetractsThenTheZoneIsToldThenVgr1GoesToTheSafePosition() {
        cb.tokens(false, false);
        zoneCBMission.start();

        vgr1PicksARedToken(); // the feed is free: placed at once
        assertEquals(PLACE_CB_FEED, vgr1.lastCommand());
        assertEquals("VGR1PlacingOnFeed", zoneCBMission.getActiveStateName());

        vgr1.commandSucceeds(); // placed
        assertEquals("retractArm", vgr1.lastCommand(), "the arm leaves the belt first");
        assertEquals("VGR1PlacingOnFeed", zoneCBMission.getActiveStateName(), "VGR1 is still over the belt");

        vgr1.commandSucceeds(); // arm retracted: tell the zone, then go to the safe position
        assertEquals("TokenOnFeed", zoneCBMission.getActiveStateName());
        assertEquals(STANDBY, vgr1.lastCommand());

        vgr1.commandSucceeds(); // in standby
        assertEquals("Idle", vgr1Mission.getActiveStateName(), "ready for the next token");
    }

    @Test
    void withATokenStillOnTheFeedVgr1WaitsInStandbyHoldingItsTokenThenPlacesOnceTheBeltMovedIt() {
        cb.tokens(true, true); // a token waits on the swap for VGR2, the next one on the feed
        zoneCBMission.start();
        assertEquals("SwapThenFeed", zoneCBMission.getActiveStateName());

        vgr1PicksARedToken();
        assertEquals(STANDBY, vgr1.lastCommand(), "refused: VGR1 goes to the safe position with its token");
        assertEquals("StandbyWithTokenCMD", vgr1Mission.getActiveStateName());

        vgr1.commandSucceeds(); // in standby: asks again, still refused
        assertEquals("WaitForFreeCBSlot", vgr1Mission.getActiveStateName());
        assertFalse(vgr1.commands.contains(PLACE_CB_FEED));

        zoneCB.publish(new CBTokenRequestEventMessage());    // VGR2 picks the swap token...
        zoneCB.publish(new TokenPickedFromCBEventMessage()); // ...and leaves the belt: the feed token still there
        assertEquals("TokenOnFeed", zoneCBMission.getActiveStateName());
        assertEquals("WaitForFreeCBSlot", vgr1Mission.getActiveStateName(), "the feed is not free yet");

        cb.tokens(false, true);
        zoneCB.publish(new FeedFreeEventMessage()); // the belt moved it to the swap

        assertEquals(PLACE_CB_FEED, vgr1.lastCommand());
        assertEquals("VGR1PlacingOnFeedAfterSwap", zoneCBMission.getActiveStateName());
    }

    @Test
    void theFeedFreedWhileVgr1MovesToStandbyIsNotLost() {
        cb.tokens(true, false); // the belt moves a token from the feed
        zoneCBMission.start();
        assertEquals("TokenOnFeed", zoneCBMission.getActiveStateName());

        vgr1PicksARedToken();
        assertEquals("StandbyWithTokenCMD", vgr1Mission.getActiveStateName());

        zoneCB.publish(new FeedFreeEventMessage()); // the token reached the swap: CBSlotFree, dropped while moving
        assertEquals("StandbyWithTokenCMD", vgr1Mission.getActiveStateName());

        vgr1.commandSucceeds(); // in standby: asks again

        assertEquals(List.of("setup", STANDBY, PICK_RED, STANDBY, PLACE_CB_FEED), vgr1.commands);
        assertEquals("VGR1PlacingOnFeedAfterSwap", zoneCBMission.getActiveStateName());
    }

    @Test
    void aRequestSentBeforeTheZoneRunsIsSentAgainWhenTheZoneStarts() {
        vgr1PicksARedToken(); // the zone is not running: the request is lost
        assertEquals("IdlePicked", vgr1Mission.getActiveStateName());

        cb.tokens(false, false);
        zoneCBMission.start(); // the feed is free: the zone tells it

        assertEquals(PLACE_CB_FEED, vgr1.lastCommand());
    }
}
