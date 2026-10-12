package fr.inria.mbdo.mission.extensions.ren_mission_01_impl;

import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippermissions.vacuumgripper2nominalmission.VacuumGripper2NominalMission;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.zonemissioncbnominal.ZoneMissionCBNominal;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.zonemissionmpsnominal.ZoneMissionMPSNominal;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.zonemissionmpsnominal.ZoneMissionMPSNominalActions;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.VacuumGripper2MissionTest.RecordingVacuumGripper;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.ZoneAdapterImpl;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions.VacuumGripper2NominalMissionActionsImpl;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions.ZoneMissionCBNominalActionsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A stopped mission ignores events, and a mission can be started directly in one of its states (to migrate a machine
 * to it), without running its entry action.
 */
class MissionMigrationRuntimeTest {

    private RecordingVacuumGripper vgr2;
    private ZoneMissionCBNominal zoneCBMission;
    private VacuumGripper2NominalMission vgr2Mission;

    @BeforeEach
    void setUp() {
        vgr2 = new RecordingVacuumGripper();
        ZoneAdapterImpl zoneCB = new ZoneAdapterImpl("ZoneCB");
        ZoneAdapterImpl zoneMPS = new ZoneAdapterImpl("ZoneMPS");
        zoneCBMission = new ZoneMissionCBNominal(zoneCB, new ZoneMissionCBNominalActionsImpl());
        ZoneMissionMPSNominal zoneMPSMission = new ZoneMissionMPSNominal(zoneMPS, new ZoneMissionMPSNominalActions() { });
        vgr2Mission = new VacuumGripper2NominalMission(vgr2, zoneCB, zoneMPS, new VacuumGripper2NominalMissionActionsImpl());
        zoneCBMission.start();
        zoneMPSMission.start();
    }

    @Test
    void aMissionNotStartedIsNotRunning() {
        assertFalse(vgr2Mission.isRunning());
        assertNull(vgr2Mission.getActiveStateName());
    }

    @Test
    void aStoppedMissionIgnoresEvents() {
        vgr2Mission.start();
        vgr2.commandSucceeds(); // setup done: go to standby
        assertEquals("StandbyCMD", vgr2Mission.getActiveStateName());

        vgr2Mission.stop();
        vgr2.commandSucceeds();

        assertFalse(vgr2Mission.isRunning());
        assertNull(vgr2Mission.getActiveStateName());
        assertEquals("moveToSafePosition", vgr2.lastCommand(), "no command sent after the stop");
    }

    @Test
    void startAtEntersTheStateWithoutRunningTheEntryAction() {
        vgr2Mission.startAt("PickCBswapCMD");

        assertTrue(vgr2Mission.isRunning());
        assertEquals("PickCBswapCMD", vgr2Mission.getActiveStateName());
        assertTrue(vgr2.commands.isEmpty(), "no setup sent");

        vgr2.commandSucceeds(); // the pick in progress when migrating ends
        assertEquals("retractArm", vgr2.lastCommand());
    }

    @Test
    void startAtRunsTheCompletionTransitionsOfTheState() {
        zoneCBMission.startAt("IdleBusy"); // VGR2 held the CB zone
        vgr2Mission.startAt("CBZoneReleased");

        assertEquals("StandbyBeforeMPSCMD", vgr2Mission.getActiveStateName());
        assertEquals("moveToSafePosition", vgr2.lastCommand());
    }

    @Test
    void startAtAnUnknownStateFails() {
        assertThrows(IllegalArgumentException.class, () -> vgr2Mission.startAt("NoSuchState"));
        assertFalse(vgr2Mission.isRunning());
    }

    @Test
    void theStatesAreListedFromTheInitialOne() {
        assertEquals("SetupCMD", vgr2Mission.getStateNames().get(0));
        assertTrue(vgr2Mission.getStateNames().containsAll(
            java.util.List.of("Idle", "WaitForCBZoneAcquisition", "PickCBswapCMD", "ReturnToStandbyCMD")));
    }
}
