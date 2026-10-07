package fr.inria.mbdo.mission.extensions.ren_mission_01_impl;

import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinesystem.sortingline.AbstractSortingLineMachineAdapter;
import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinesystem.sortinglinemessages.RedTokenAvailableEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippermissions.vacuumgripper1nominalmission.VacuumGripper1NominalMission;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.zonemissioncbnominal.ZoneMissionCBNominal;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.ZoneAdapterImpl;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions.VacuumGripper1NominalMissionActionsImpl;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions.ZoneMissionCBNominalActionsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * VGR1 picks a sorted token at its chute and places it on the conveyor belt feed under the CB zone; after the place it
 * retracts its arm, releases the CB zone, then goes to the safe position.
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

    private VacuumGripper2MissionTest.RecordingVacuumGripper vgr1;
    private StubSortingLine sortingLine;
    private ZoneAdapterImpl zoneCB;
    private ZoneMissionCBNominal zoneCBMission;
    private VacuumGripper1NominalMission vgr1Mission;

    @BeforeEach
    void setUp() {
        vgr1 = new VacuumGripper2MissionTest.RecordingVacuumGripper();
        sortingLine = new StubSortingLine();
        zoneCB = new ZoneAdapterImpl("ZoneCB");
        zoneCBMission = new ZoneMissionCBNominal(zoneCB, new ZoneMissionCBNominalActionsImpl());
        vgr1Mission = new VacuumGripper1NominalMission(vgr1, sortingLine, zoneCB,
                new VacuumGripper1NominalMissionActionsImpl());
        zoneCBMission.start();
        vgr1Mission.start();

        assertEquals("setup", vgr1.lastCommand());
        vgr1.commandSucceeds();
        assertEquals("moveToSafePosition", vgr1.lastCommand());
        vgr1.commandSucceeds();
        assertEquals("Idle", vgr1Mission.getActiveStateName());
    }

    @Test
    void afterThePlaceTheArmRetractsThenTheZoneIsReleasedThenVgr1GoesToTheSafePosition() {
        sortingLine.publish(new RedTokenAvailableEventMessage());
        assertEquals(PICK_RED, vgr1.lastCommand());

        vgr1.commandSucceeds(); // picked: request the CB zone, free, so placed at once
        assertEquals(PLACE_CB_FEED, vgr1.lastCommand());
        assertEquals("IdleBusy", zoneCBMission.getActiveStateName());

        vgr1.commandSucceeds(); // placed
        assertEquals("retractArm", vgr1.lastCommand(), "the arm leaves the belt first");
        assertEquals("IdleBusy", zoneCBMission.getActiveStateName(), "the CB zone is kept while the arm is over the belt");

        vgr1.commandSucceeds(); // arm retracted: release the CB zone, then go to the safe position
        assertEquals("IdleFree", zoneCBMission.getActiveStateName());
        assertEquals("moveToSafePosition", vgr1.lastCommand());

        vgr1.commandSucceeds(); // in standby
        assertEquals("Idle", vgr1Mission.getActiveStateName(), "ready for the next token");
    }
}
