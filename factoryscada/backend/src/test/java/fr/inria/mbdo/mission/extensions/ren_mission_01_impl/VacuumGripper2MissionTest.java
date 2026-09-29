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
 * then brings it to the MPS input under the MPS zone.
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

        String lastCommand() {
            return commands.get(commands.size() - 1);
        }
    }

    private static final String PICK_CB_SWAP = "pick " + new Position3D(1050, 1810, 1155);
    private static final String PLACE_MPS_INPUT = "place " + new Position3D(1000, 1890, 2050);

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
        assertEquals("moveToSafePosition", vgr2.lastCommand());
        vgr2.commandSucceeds(); // in standby: release the CB zone, acquire the MPS zone

        assertEquals("IdleFree", zoneCBMission.getActiveStateName());
        assertEquals(PLACE_MPS_INPUT, vgr2.lastCommand());
        assertEquals("IdleBusy", zoneMPSMission.getActiveStateName());

        vgr2.commandSucceeds(); // placed
        vgr2.commandSucceeds(); // in standby: release the MPS zone
        assertEquals("IdleFree", zoneMPSMission.getActiveStateName());
        assertEquals("GotoStandbyCMD", vgr2Mission.getActiveStateName());

        vgr2.commandSucceeds();
        assertEquals("Idle", vgr2Mission.getActiveStateName(), "ready for the next token");
    }
}
