package fr.inria.mbdo.mission.extensions.ren_mission_01_impl;

import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltsystem.conveyorbeltmessages.SwapBusyEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippermissions.vacuumgripper2nominalmission.VacuumGripper2NominalMission;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgripper.AbstractVacuumGripperMachineAdapter;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgrippercommands.Position3D;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgrippermessages.VGRCommandSuccessEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.zonemissioncbnominal.ZoneMissionCBNominal;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.zonemissionmpsnominal.ZoneMissionMPSNominal;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.zonemissionmpsnominal.ZoneMissionMPSNominalActions;
import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltsystem.conveyorbeltmessages.FeedFreeEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.AcquireRequestEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.CBSlotRequestEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.ReleaseRequestEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.TokenPlacedOnCBEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.ZoneAdapterImpl;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions.VacuumGripper2NominalMissionActionsImpl;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions.ZoneMissionCBNominalActionsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * VGR2 picks the token at the CB swap once the conveyor belt reports it, but only when the CB zone lets it over the
 * belt (VGR1 is not over it), then brings it to the MPS input under the MPS zone, shared with the MPS.
 */
class VacuumGripper2MissionTest {

    /** Records the commands; each command's success is simulated by the test. */
    static final class RecordingVacuumGripper extends AbstractVacuumGripperMachineAdapter {
        final List<String> commands = new ArrayList<>();

        RecordingVacuumGripper() {
            super("REN_MISSION_01/VGR02");
        }

        void commandSucceeds() {
            publish(new VGRCommandSuccessEventMessage());
        }

        @Override public void goToPosition(Position3D targetPosition) { commands.add("goToPosition " + targetPosition); }
        @Override public void move(Position3D startPosition, Position3D endPosition) { commands.add("move"); }
        @Override public void pick(Position3D targetPosition) { commands.add("pick " + targetPosition); }
        @Override public void place(Position3D targetPosition) { commands.add("place " + targetPosition); }
        @Override public void setup() { commands.add("setup"); }
        @Override public void statusRequest() { commands.add("statusRequest"); }
        @Override public void grip() { commands.add("grip"); }
        @Override public void release() { commands.add("release"); }
        @Override public void stop() { commands.add("stop"); }
        @Override public void moveToSafePosition() { commands.add("moveToSafePosition"); }
        @Override public void retractArm() { commands.add("retractArm"); }
        @Override public void goToNamedPosition(String positionName) { commands.add("goToNamedPosition " + positionName); }
        @Override public void pickNamed(String positionName) { commands.add("pickNamed " + positionName); }
        @Override public void placeNamed(String positionName) { commands.add("placeNamed " + positionName); }

        String lastCommand() {
            return commands.get(commands.size() - 1);
        }
    }

    // named positions of VGR2 on the PLC
    private static final String PICK_CB_SWAP = "pickNamed ALT_CB";
    private static final String PLACE_MPS_INPUT = "placeNamed MPS_INPUT";

    private RecordingVacuumGripper vgr2;
    private ConveyorBeltMissionTest.RecordingConveyorBelt cb;
    private ZoneAdapterImpl zoneCB;
    private ZoneAdapterImpl zoneMPS;
    private ZoneMissionCBNominal zoneCBMission;
    private ZoneMissionMPSNominal zoneMPSMission;
    private VacuumGripper2NominalMission vgr2Mission;

    @BeforeEach
    void setUp() {
        vgr2 = new RecordingVacuumGripper();
        cb = new ConveyorBeltMissionTest.RecordingConveyorBelt();
        zoneCB = new ZoneAdapterImpl("ZoneCB");
        zoneMPS = new ZoneAdapterImpl("ZoneMPS");
        zoneCBMission = new ZoneMissionCBNominal(zoneCB, cb, new ZoneMissionCBNominalActionsImpl());
        zoneMPSMission = new ZoneMissionMPSNominal(zoneMPS, new ZoneMissionMPSNominalActions() { });
        vgr2Mission = new VacuumGripper2NominalMission(vgr2, cb, zoneCB, zoneMPS, new VacuumGripper2NominalMissionActionsImpl());
        cb.tokens(false, false);
        zoneCBMission.start();
        zoneMPSMission.start();
        vgr2Mission.start();

        assertEquals("setup", vgr2.lastCommand());
        vgr2.commandSucceeds();
        assertEquals("moveToSafePosition", vgr2.lastCommand());
        vgr2.commandSucceeds();
        assertEquals("Idle", vgr2Mission.getActiveStateName());
    }

    /** VGR1 places a token on the free feed (it asks the CB zone, then tells it once retracted). */
    private void vgr1PlacesATokenOnTheFeed() {
        zoneCB.publish(new CBSlotRequestEventMessage());
        zoneCB.publish(new TokenPlacedOnCBEventMessage());
    }

    /** The belt moved the token on the feed to the swap: it tells the CB zone, then VGR2 (SwapBusy). */
    private void theBeltMovesTheTokenToTheSwap() {
        cb.tokens(false, true);
        zoneCB.publish(new FeedFreeEventMessage());
        vgr2.publish(new SwapBusyEventMessage());
    }

    private void aTokenArrivesAtTheSwap() {
        vgr1PlacesATokenOnTheFeed();
        theBeltMovesTheTokenToTheSwap();
    }

    @Test
    void vgr2WaitsForVgr1ToLeaveTheBeltBeforePickingAtTheSwap() {
        vgr1PlacesATokenOnTheFeed();
        cb.tokens(false, true);
        zoneCB.publish(new FeedFreeEventMessage()); // the belt moved it to the swap, VGR2 busy elsewhere
        zoneCB.publish(new CBSlotRequestEventMessage()); // VGR1 places the next token on the feed
        assertEquals("VGR1PlacingOnFeedAfterSwap", zoneCBMission.getActiveStateName());

        vgr2.publish(new SwapBusyEventMessage()); // VGR2 learns of the token at the swap

        assertEquals("Idle", vgr2Mission.getActiveStateName(), "refused: VGR1 is over the belt");
        assertEquals("moveToSafePosition", vgr2.lastCommand(), "no pick while VGR1 is over the belt");

        zoneCB.publish(new TokenPlacedOnCBEventMessage()); // VGR1 out of the belt: the zone tells VGR2

        assertEquals(PICK_CB_SWAP, vgr2.lastCommand());
        assertEquals("VGR2PickingSwapBeforeFeed", zoneCBMission.getActiveStateName(), "VGR2 is now over the belt");
    }

    @Test
    void vgr2BringsTheTokenFromTheSwapToTheMpsInputAndTellsTheZones() {
        aTokenArrivesAtTheSwap();
        assertEquals(PICK_CB_SWAP, vgr2.lastCommand(), "VGR1 was not over the belt");

        vgr2.commandSucceeds(); // picked
        assertEquals("retractArm", vgr2.lastCommand(), "the arm leaves the belt first");
        assertEquals("VGR2PickingLastToken", zoneCBMission.getActiveStateName(), "VGR2 is still over the belt");

        vgr2.commandSucceeds(); // arm retracted: tell the CB zone, then go to the safe position
        assertEquals("Empty", zoneCBMission.getActiveStateName());
        assertEquals("moveToSafePosition", vgr2.lastCommand());

        vgr2.commandSucceeds(); // in standby: request the MPS zone
        assertEquals("IdleBusy", zoneMPSMission.getActiveStateName(), "the MPS zone is now held by VGR2");
        assertEquals(PLACE_MPS_INPUT, vgr2.lastCommand());

        vgr2.commandSucceeds(); // placed
        assertEquals("retractArm", vgr2.lastCommand(), "the arm leaves the MPS first");
        assertEquals("IdleBusy", zoneMPSMission.getActiveStateName());

        vgr2.commandSucceeds(); // arm retracted: release the MPS zone, then go to the safe position
        assertEquals("IdleFree", zoneMPSMission.getActiveStateName());
        assertEquals("moveToSafePosition", vgr2.lastCommand());

        cb.tokens(false, false);
        vgr2.commandSucceeds(); // in standby
        assertEquals("Idle", vgr2Mission.getActiveStateName(), "ready for the next token");
        assertEquals("Empty", zoneCBMission.getActiveStateName());
    }

    @Test
    void vgr2WaitsForTheMpsToReleaseTheMpsZoneBeforePlacing() {
        zoneMPS.publish(new AcquireRequestEventMessage()); // the MPS holds its zone (processing)
        aTokenArrivesAtTheSwap();
        vgr2.commandSucceeds(); // picked
        vgr2.commandSucceeds(); // arm retracted: tell the CB zone, go to the safe position
        vgr2.commandSucceeds(); // in standby: request the MPS zone

        assertEquals("WaitForMPSZoneAcquisition", vgr2Mission.getActiveStateName());
        assertEquals("IdleBusyRequested", zoneMPSMission.getActiveStateName());
        assertEquals("moveToSafePosition", vgr2.lastCommand(), "no place while the MPS holds its zone");

        zoneMPS.publish(new ReleaseRequestEventMessage()); // the MPS is done

        assertEquals(PLACE_MPS_INPUT, vgr2.lastCommand());
        assertEquals("IdleBusy", zoneMPSMission.getActiveStateName(), "the zone is now held by VGR2");
    }

    // The conveyor belt sends SwapBusy only once, when it brings a token to the swap: VGR2 must not miss it while busy.

    @Test
    void aTokenBroughtToTheSwapWhileVgr2IsAtTheMpsIsPickedOnceVgr2IsBackInIdle() {
        aTokenArrivesAtTheSwap();
        vgr2.commandSucceeds(); // picked
        cb.tokens(false, false);
        vgr2.commandSucceeds(); // arm retracted: tell the CB zone, go to the safe position
        vgr2.commandSucceeds(); // in standby: request the MPS zone, granted: place
        assertEquals(PLACE_MPS_INPUT, vgr2.lastCommand());

        aTokenArrivesAtTheSwap(); // the belt tells VGR2, busy at the MPS: the message is dropped
        assertEquals("PlaceMPSinCMD", vgr2Mission.getActiveStateName());

        vgr2.commandSucceeds(); // placed: retract
        vgr2.commandSucceeds(); // retracted: release the MPS zone, go to the safe position
        vgr2.commandSucceeds(); // back in standby: a token waits at the swap

        assertEquals(PICK_CB_SWAP, vgr2.lastCommand());
        assertEquals("PickCBswapCMD", vgr2Mission.getActiveStateName());
    }

    @Test
    void backInIdleWithNoTokenAtTheSwapVgr2Waits() {
        aTokenArrivesAtTheSwap();
        cb.tokens(false, false);
        for (int i = 0; i < 6; i++) {
            vgr2.commandSucceeds(); // picked, retracted, standby, placed, retracted, standby
        }

        assertEquals("Idle", vgr2Mission.getActiveStateName());
        assertEquals("moveToSafePosition", vgr2.lastCommand());
    }

    @Test
    void vgr2TakesTheTokenAtTheSwapWhileTheMpsIsFullAndHoldsItUntilTheMpsZoneIsFree() {
        zoneMPS.publish(new AcquireRequestEventMessage()); // the MPS holds its zone: a payload waits at its input

        aTokenArrivesAtTheSwap();
        assertEquals(PICK_CB_SWAP, vgr2.lastCommand(), "VGR2 picks although the MPS is full");
        vgr2.commandSucceeds(); // picked
        cb.tokens(false, false);
        vgr2.commandSucceeds(); // retracted: tell the CB zone (the belt may bring the next token)
        assertEquals("Empty", zoneCBMission.getActiveStateName());
        vgr2.commandSucceeds(); // in standby: request the MPS zone

        assertEquals("WaitForMPSZoneAcquisition", vgr2Mission.getActiveStateName(), "VGR2 holds the token");
        assertEquals("moveToSafePosition", vgr2.lastCommand());

        aTokenArrivesAtTheSwap(); // announced to VGR2 while it holds its token: dropped
        assertEquals("WaitForMPSZoneAcquisition", vgr2Mission.getActiveStateName());

        zoneMPS.publish(new ReleaseRequestEventMessage()); // the MPS is done
        assertEquals(PLACE_MPS_INPUT, vgr2.lastCommand());
        vgr2.commandSucceeds(); // placed
        vgr2.commandSucceeds(); // retracted
        vgr2.commandSucceeds(); // back in standby: takes the next token

        assertEquals(PICK_CB_SWAP, vgr2.lastCommand());
    }

    // Both VGR2 and the MPS use the MPS zone's AcquireResponseEventMessage: VGR2 only takes it "via zoneMPS", and the
    // CB zone answers VGR2 with its own messages.

    @Test
    void vgr2OnlyPlacesOnTheMpsZoneGrant() {
        zoneMPS.publish(new AcquireRequestEventMessage()); // the MPS holds its zone (processing)
        aTokenArrivesAtTheSwap();                          // VGR2 goes over the belt and picks
        zoneCB.publish(new CBSlotRequestEventMessage());   // VGR1 is refused meanwhile
        vgr2.commandSucceeds(); // picked
        vgr2.commandSucceeds(); // arm retracted: tell the CB zone, go to the safe position
        vgr2.commandSucceeds(); // in standby: request the MPS zone

        assertEquals("Empty", zoneCBMission.getActiveStateName());
        assertEquals("WaitForMPSZoneAcquisition", vgr2Mission.getActiveStateName());
        assertEquals("moveToSafePosition", vgr2.lastCommand(), "no place while the MPS holds its zone");

        zoneMPS.publish(new ReleaseRequestEventMessage()); // the MPS is done

        assertEquals(PLACE_MPS_INPUT, vgr2.lastCommand());
    }

    @Test
    void anMpsZoneGrantToTheMpsIsNotTakenAsTheCbZoneGrant() {
        vgr1PlacesATokenOnTheFeed();
        cb.tokens(false, true);
        zoneCB.publish(new FeedFreeEventMessage());
        zoneCB.publish(new CBSlotRequestEventMessage()); // VGR1 is over the belt (placing on the feed)
        vgr2.publish(new SwapBusyEventMessage());        // VGR2 is refused

        zoneMPS.publish(new AcquireRequestEventMessage()); // the MPS takes its zone, granted at once

        assertEquals("IdleBusy", zoneMPSMission.getActiveStateName(), "the MPS zone is held by the MPS");
        assertEquals("moveToSafePosition", vgr2.lastCommand(), "no pick while VGR1 is over the belt");

        zoneCB.publish(new TokenPlacedOnCBEventMessage()); // VGR1 out of the belt

        assertEquals(PICK_CB_SWAP, vgr2.lastCommand());
    }

    @Test
    void aRequestSentBeforeTheCbZoneRunsIsSentAgainWhenTheZoneStarts() {
        zoneCBMission.stop();
        cb.tokens(false, true);
        vgr2.publish(new SwapBusyEventMessage()); // the request is lost
        assertEquals("WaitForCBZoneAcquisition", vgr2Mission.getActiveStateName());

        zoneCBMission.start(); // a token at the swap: the zone tells it

        assertEquals(PICK_CB_SWAP, vgr2.lastCommand());
        assertEquals("VGR2PickingLastToken", zoneCBMission.getActiveStateName());
    }
}
