package fr.inria.mbdo.mission.extensions.ren_mission_01_impl;

import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltmissions.conveyorbeltnominalmission.ConveyorBeltNominalMission;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.zonemissioncbnominal.ZoneMissionCBNominal;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.ConveyorBeltMissionTest.RecordingConveyorBelt;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.VacuumGripper2MissionTest.RecordingVacuumGripper;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.ZoneAdapterImpl;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions.ConveyorBeltNominalMissionActionsImpl;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions.ZoneMissionCBNominalActionsImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The SCADA mission page shows a mission executing a command only while its active state waits for a command result
 * (from the mission model, whatever the state's name): never a zone, which only exchanges messages.
 */
class AwaitingCommandResultTest {

    private final RecordingConveyorBelt cb = new RecordingConveyorBelt();
    private final ZoneAdapterImpl zoneCB = new ZoneAdapterImpl("ZoneCB");

    @Test
    void aZoneNeverWaitsForACommandResult() {
        cb.tokens(true, false);
        ZoneMissionCBNominal zone = new ZoneMissionCBNominal(zoneCB, cb, new ZoneMissionCBNominalActionsImpl());

        zone.start();

        assertEquals("TokenOnFeed", zone.getActiveStateName());
        assertFalse(zone.isAwaitingCommandResult());
    }

    @Test
    void theBeltWaitsForTheResultOfItsMoveOnly() {
        cb.tokens(true, false);
        ConveyorBeltNominalMission belt = new ConveyorBeltNominalMission(cb, new RecordingVacuumGripper(),
                new RecordingVacuumGripper(), zoneCB, new ConveyorBeltNominalMissionActionsImpl());

        belt.start();
        assertEquals("MovingToSensor", belt.getActiveStateName());
        assertTrue(belt.isAwaitingCommandResult());

        cb.commandSucceeds();
        assertEquals("Idle", belt.getActiveStateName());
        assertFalse(belt.isAwaitingCommandResult());
    }
}
