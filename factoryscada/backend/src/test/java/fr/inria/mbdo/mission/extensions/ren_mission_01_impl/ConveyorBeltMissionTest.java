package fr.inria.mbdo.mission.extensions.ren_mission_01_impl;

import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltmissions.conveyorbeltnominalmission.ConveyorBeltNominalMission;
import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltsystem.conveyorbeltmessages.CBCommandSuccessEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippermissions.vacuumgripper2nominalmission.VacuumGripper2NominalMission;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.zonemissioncbnominal.ZoneMissionCBNominal;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.zonemissionmpsnominal.ZoneMissionMPSNominal;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.zonemissionmpsnominal.ZoneMissionMPSNominalActions;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.VacuumGripper2MissionTest.RecordingVacuumGripper;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.ConveyorBeltAdapterImpl;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.ZoneAdapterImpl;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions.ConveyorBeltNominalMissionActionsImpl;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions.VacuumGripper2NominalMissionActionsImpl;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions.ZoneMissionCBNominalActionsImpl;
import io.github.mbdo.factoryscada.core.MqttMessageRouter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * The conveyor belt moves a token from its feed to its swap when the swap is free, and tells VGR2. When its mission is
 * (re)started with tokens already on the belt (e.g. after a repair), it checks its light barriers (active-low) once:
 * the light barrier edges Idle waits for already happened.
 */
class ConveyorBeltMissionTest {

    /** The SCADA conveyor belt adapter, recording its commands instead of sending them to the PLC. */
    static final class RecordingConveyorBelt extends ConveyorBeltAdapterImpl {
        final List<String> commands = new ArrayList<>();

        RecordingConveyorBelt() {
            super("REN_MISSION_01/CB01", mock(MqttMessageRouter.class), "PLC/+/ConveyorBelt/x/#");
        }

        void commandSucceeds() {
            publish(new CBCommandSuccessEventMessage());
        }

        /** Light barriers are active-low: false = token present. */
        void tokens(boolean atFeed, boolean atSwap) {
            setConveyorSensFeed(!atFeed);
            setConveyorSensSwap(!atSwap);
        }

        @Override public void moveToSensor() { commands.add("moveToSensor"); }
        @Override public void moveOut() { commands.add("moveOut"); }
        @Override public void moveNbSteps() { commands.add("moveNbSteps"); }
        @Override public void stop() { commands.add("stop"); }
        @Override public void statusRequest() { commands.add("statusRequest"); }
    }

    private static final String PICK_CB_SWAP = "pickNamed ALT_CB";

    private RecordingConveyorBelt cb;
    private RecordingVacuumGripper vgr1;
    private RecordingVacuumGripper vgr2;
    private ConveyorBeltNominalMission cbMission;
    private ZoneMissionCBNominal zoneCBMission;
    private VacuumGripper2NominalMission vgr2Mission;

    @BeforeEach
    void setUp() {
        cb = new RecordingConveyorBelt();
        vgr1 = new RecordingVacuumGripper();
        vgr2 = new RecordingVacuumGripper();
        ZoneAdapterImpl zoneCB = new ZoneAdapterImpl("ZoneCB");
        ZoneAdapterImpl zoneMPS = new ZoneAdapterImpl("ZoneMPS");
        zoneCBMission = new ZoneMissionCBNominal(zoneCB, cb, new ZoneMissionCBNominalActionsImpl());
        ZoneMissionMPSNominal zoneMPSMission = new ZoneMissionMPSNominal(zoneMPS, new ZoneMissionMPSNominalActions() { });
        cbMission = new ConveyorBeltNominalMission(cb, vgr1, vgr2, zoneCB, new ConveyorBeltNominalMissionActionsImpl());
        vgr2Mission = new VacuumGripper2NominalMission(vgr2, cb, zoneCB, zoneMPS,
                new VacuumGripper2NominalMissionActionsImpl());
        zoneMPSMission.start();
        // VGR2 is waiting in Idle before the light barriers are known (it then checks the swap no more)
        vgr2Mission.start();
        vgr2.commandSucceeds(); // setup
        vgr2.commandSucceeds(); // in standby
        assertEquals("Idle", vgr2Mission.getActiveStateName());
        vgr2.commands.clear();
    }

    /** The CB zone, then the belt, (re)start: both check the light barriers. */
    private void startZoneAndBelt() {
        zoneCBMission.start();
        cbMission.start();
    }

    @Test
    void startedWithATokenAtTheSwapTheBeltTellsVgr2() {
        cb.tokens(false, true);

        startZoneAndBelt();

        assertEquals(List.of(PICK_CB_SWAP), vgr2.commands, "VGR2 takes the token at the swap");
        assertEquals(List.of(), cb.commands, "the belt does not move");
        assertEquals("Idle", cbMission.getActiveStateName());
    }

    @Test
    void startedWithATokenOnlyAtTheFeedTheBeltMovesItToTheSwap() {
        cb.tokens(true, false);

        startZoneAndBelt();

        assertEquals(List.of("moveToSensor"), cb.commands);
        assertEquals("MovingToSensor", cbMission.getActiveStateName());

        cb.tokens(false, true);
        cb.commandSucceeds(); // the token is at the swap: VGR2 is told
        assertEquals(List.of(PICK_CB_SWAP), vgr2.commands);
        assertEquals("Idle", cbMission.getActiveStateName());
    }

    @Test
    void startedWithTokensAtTheFeedAndTheSwapTheBeltMovesTheFeedOneOnceVgr2TookTheSwapOne() {
        cb.tokens(true, true);

        startZoneAndBelt();

        assertEquals(List.of(PICK_CB_SWAP), vgr2.commands);
        assertEquals(List.of(), cb.commands, "the swap is busy");

        cb.tokens(true, false); // VGR2 took the swap one
        assertEquals(List.of("moveToSensor"), cb.commands);
    }

    @Test
    void startedWithAnEmptyBeltTheBeltWaits() {
        cb.tokens(false, false);

        startZoneAndBelt();

        assertEquals(List.of(), cb.commands);
        assertEquals(List.of(), vgr2.commands);
        assertEquals("Idle", cbMission.getActiveStateName());

        cb.tokens(true, false); // a token is placed at the feed
        assertEquals(List.of("moveToSensor"), cb.commands);
    }

    @Test
    void lightBarriersNotReceivedYetReadAsFree() {
        startZoneAndBelt();

        assertEquals(List.of(), cb.commands);
        assertEquals(List.of(), vgr2.commands);
        assertEquals("Idle", cbMission.getActiveStateName());
    }
}
