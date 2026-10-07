package fr.inria.mbdo.mission.extensions.ren_mission_01_impl;

import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltsystem.conveyorbeltmessages.SwapBusyEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippermissions.vacuumgripper2nominalmission.VacuumGripper2NominalMission;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgripper.AbstractVacuumGripperMachineAdapter;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgrippercommands.Position3D;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgrippermessages.VGRCommandSuccessEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.zonemissioncbnominal.ZoneMissionCBNominal;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.zonemissionmpsnominal.ZoneMissionMPSNominal;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.zonemissionmpsnominal.ZoneMissionMPSNominalActions;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.AcquireRequestEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.ReleaseRequestEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.ZoneAdapterImpl;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions.VacuumGripper2NominalMissionActionsImpl;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions.ZoneMissionCBNominalActionsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * VGR2 picks the token at the CB swap once the conveyor belt reports it, but only when it holds the CB zone,
 * then brings it to the MPS input under the MPS zone, shared with the MPS.
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
    private ZoneAdapterImpl zoneCB;
    private ZoneAdapterImpl zoneMPS;
    private ZoneMissionCBNominal zoneCBMission;
    private ZoneMissionMPSNominal zoneMPSMission;
    private VacuumGripper2NominalMission vgr2Mission;

    @BeforeEach
    void setUp() {
        vgr2 = new RecordingVacuumGripper();
        zoneCB = new ZoneAdapterImpl("ZoneCB");
        zoneMPS = new ZoneAdapterImpl("ZoneMPS");
        zoneCBMission = new ZoneMissionCBNominal(zoneCB, new ZoneMissionCBNominalActionsImpl());
        zoneMPSMission = new ZoneMissionMPSNominal(zoneMPS, new ZoneMissionMPSNominalActions() { });
        vgr2Mission = new VacuumGripper2NominalMission(vgr2, zoneCB, zoneMPS, new VacuumGripper2NominalMissionActionsImpl());
        zoneCBMission.start();
        zoneMPSMission.start();
        vgr2Mission.start();

        assertEquals("setup", vgr2.lastCommand());
        vgr2.commandSucceeds();
        assertEquals("moveToSafePosition", vgr2.lastCommand());
        vgr2.commandSucceeds();
        assertEquals("Idle", vgr2Mission.getActiveStateName());
    }

    @Test
    void vgr2WaitsForVgr1ToReleaseTheCbZoneBeforePickingAtTheSwap() {
        zoneCB.publish(new AcquireRequestEventMessage()); // VGR1 holds the CB zone (placing on the feed)
        assertEquals("IdleBusy", zoneCBMission.getActiveStateName());

        vgr2.publish(new SwapBusyEventMessage()); // the conveyor belt brought the token to the swap

        assertEquals("WaitForCBZoneAcquisition", vgr2Mission.getActiveStateName());
        assertEquals("IdleBusyRequested", zoneCBMission.getActiveStateName());
        assertEquals("moveToSafePosition", vgr2.lastCommand(), "no pick while VGR1 is over the belt");

        zoneCB.publish(new ReleaseRequestEventMessage()); // VGR1 back in standby

        assertEquals(PICK_CB_SWAP, vgr2.lastCommand());
        assertEquals("IdleBusy", zoneCBMission.getActiveStateName(), "the zone is now held by VGR2");
    }

    @Test
    void vgr2BringsTheTokenFromTheSwapToTheMpsInputAndReleasesBothZones() {
        vgr2.publish(new SwapBusyEventMessage());
        assertEquals(PICK_CB_SWAP, vgr2.lastCommand(), "the CB zone was free");

        vgr2.commandSucceeds(); // picked
        assertEquals("retractArm", vgr2.lastCommand(), "the arm leaves the belt first");
        assertEquals("IdleBusy", zoneCBMission.getActiveStateName(), "the CB zone is kept while the arm is over the belt");

        vgr2.commandSucceeds(); // arm retracted: release the CB zone, then go to the safe position
        assertEquals("IdleFree", zoneCBMission.getActiveStateName());
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

        vgr2.commandSucceeds(); // in standby
        assertEquals("Idle", vgr2Mission.getActiveStateName(), "ready for the next token");
        assertEquals("IdleFree", zoneCBMission.getActiveStateName());
    }

    @Test
    void vgr2WaitsForTheMpsToReleaseTheMpsZoneBeforePlacing() {
        zoneMPS.publish(new AcquireRequestEventMessage()); // the MPS holds its zone (processing)
        vgr2.publish(new SwapBusyEventMessage());
        vgr2.commandSucceeds(); // picked
        vgr2.commandSucceeds(); // arm retracted: release the CB zone, go to the safe position
        vgr2.commandSucceeds(); // in standby: request the MPS zone

        assertEquals("WaitForMPSZoneAcquisition", vgr2Mission.getActiveStateName());
        assertEquals("IdleBusyRequested", zoneMPSMission.getActiveStateName());
        assertEquals("moveToSafePosition", vgr2.lastCommand(), "no place while the MPS holds its zone");

        zoneMPS.publish(new ReleaseRequestEventMessage()); // the MPS is done

        assertEquals(PLACE_MPS_INPUT, vgr2.lastCommand());
        assertEquals("IdleBusy", zoneMPSMission.getActiveStateName(), "the zone is now held by VGR2");
    }

    // Both zones publish the same AcquireResponseEventMessage, and VGR2 listens to both: a grant must only be taken
    // from the zone VGR2 is waiting for ("accept ... via zoneMPS" / "via zoneCB").

    @Test
    void aCbZoneGrantToVgr1IsNotTakenAsTheMpsZoneGrant() {
        zoneMPS.publish(new AcquireRequestEventMessage()); // the MPS holds its zone (processing)
        vgr2.publish(new SwapBusyEventMessage());          // VGR2 takes the CB zone and picks
        zoneCB.publish(new AcquireRequestEventMessage());  // VGR1 waits for the CB zone
        vgr2.commandSucceeds(); // picked
        vgr2.commandSucceeds(); // arm retracted: release the CB zone (granted to VGR1), go to the safe position
        vgr2.commandSucceeds(); // in standby: request the MPS zone

        assertEquals("IdleBusy", zoneCBMission.getActiveStateName(), "the CB zone is now held by VGR1");
        assertEquals("WaitForMPSZoneAcquisition", vgr2Mission.getActiveStateName());
        assertEquals("moveToSafePosition", vgr2.lastCommand(), "no place while the MPS holds its zone");

        zoneMPS.publish(new ReleaseRequestEventMessage()); // the MPS is done

        assertEquals(PLACE_MPS_INPUT, vgr2.lastCommand());
    }

    @Test
    void anMpsZoneGrantToTheMpsIsNotTakenAsTheCbZoneGrant() {
        zoneCB.publish(new AcquireRequestEventMessage()); // VGR1 holds the CB zone (placing on the feed)
        vgr2.publish(new SwapBusyEventMessage());         // VGR2 waits for the CB zone

        zoneMPS.publish(new AcquireRequestEventMessage()); // the MPS takes its zone, granted at once

        assertEquals("IdleBusy", zoneMPSMission.getActiveStateName(), "the MPS zone is held by the MPS");
        assertEquals("WaitForCBZoneAcquisition", vgr2Mission.getActiveStateName());
        assertEquals("moveToSafePosition", vgr2.lastCommand(), "no pick while VGR1 is over the belt");

        zoneCB.publish(new ReleaseRequestEventMessage()); // VGR1 back in standby

        assertEquals(PICK_CB_SWAP, vgr2.lastCommand());
    }
}
