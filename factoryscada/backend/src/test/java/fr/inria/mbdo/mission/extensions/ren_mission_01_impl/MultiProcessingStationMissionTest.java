package fr.inria.mbdo.mission.extensions.ren_mission_01_impl;

import fr.inria.mbdo.mission.extensions.ren_mission_01.multiprocessingstationmissions.multiprocessingstationnominalmission.MultiProcessingStationNominalMission;
import fr.inria.mbdo.mission.extensions.ren_mission_01.multiprocessingstationsystem.multiprocessingstation.AbstractMultiProcessingStationMachineAdapter;
import fr.inria.mbdo.mission.extensions.ren_mission_01.multiprocessingstationsystem.multiprocessingstationcommands.MPSOutput;
import fr.inria.mbdo.mission.extensions.ren_mission_01.multiprocessingstationsystem.multiprocessingstationmessages.MPSCommandSuccessEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippercbfailuremissions.vacuumgripper2cbfailuremission.VacuumGripper2CBFailureMission;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.zonemissiondeadcb.ZoneMissionDeadCB;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.zonemissiondeadcb.ZoneMissionDeadCBActions;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions.VacuumGripper2CBFailureMissionActionsImpl;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippermissions.vacuumgripper2nominalmission.VacuumGripper2NominalMission;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.zonemissioncbnominal.ZoneMissionCBNominal;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.zonemissionmpsnominal.ZoneMissionMPSNominal;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.zonemissionmpsnominal.ZoneMissionMPSNominalActions;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.AcquireRequestEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.ReleaseRequestEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.ZoneAdapterImpl;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions.MultiProcessingStationNominalMissionActionsImpl;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions.VacuumGripper2NominalMissionActionsImpl;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions.ZoneMissionCBNominalActionsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The MPS takes the MPS zone, shared with VGR2, as soon as a payload is at its input (whatever its output) and holds it
 * until the payload is delivered, so that VGR2 never places a payload on an occupied input nor while the MPS processes.
 * Once it gets the zone, it only processes if the payload is still at the input (6 s oven, 5 s saw, conveyor output),
 * and waits, holding the zone, while its output still holds the previous payload.
 */
class MultiProcessingStationMissionTest {

    static final class RecordingMultiProcessingStation extends AbstractMultiProcessingStationMachineAdapter {
        final List<String> commands = new ArrayList<>();

        RecordingMultiProcessingStation() {
            super("REN_MISSION_01/MPS01");
        }

        String lastCommand() {
            return commands.isEmpty() ? null : commands.get(commands.size() - 1);
        }

        void commandSucceeds() {
            publish(new MPSCommandSuccessEventMessage());
        }

        @Override public void process(int ovenTime, int sawTime, MPSOutput output) {
            commands.add("process " + ovenTime + " " + sawTime + " " + output);
        }
        @Override public void setup() { commands.add("setup"); }
        @Override public void stop() { commands.add("stop"); }
        @Override public void process1() { commands.add("process1"); }
        @Override public void moveToSafePosition() { commands.add("moveToSafePosition"); }
        @Override public void ovenLoad() { }
        @Override public void ovenUnload() { }
        @Override public void ovenHeat() { }
        @Override public void ovenProcess() { }
        @Override public void armMove() { }
        @Override public void armPick() { }
        @Override public void armPlace() { }
        @Override public void turntableRotate() { }
        @Override public void turntableEject() { }
        @Override public void conveyorMoveToSensor() { }
        @Override public void conveyorMoveOut() { }
        @Override public void sawCut() { }
    }

    private RecordingMultiProcessingStation mps;
    private ZoneAdapterImpl zoneMPS;
    private ZoneMissionMPSNominal zoneMPSMission;
    private MultiProcessingStationNominalMission mpsMission;

    @BeforeEach
    void setUp() {
        mps = new RecordingMultiProcessingStation();
        zoneMPS = new ZoneAdapterImpl("ZoneMPS");
        zoneMPSMission = new ZoneMissionMPSNominal(zoneMPS, new ZoneMissionMPSNominalActions() { });
        mpsMission = new MultiProcessingStationNominalMission(mps, zoneMPS,
                new MultiProcessingStationNominalMissionActionsImpl());
        zoneMPSMission.start();
        mpsMission.start();
        assertEquals("setup", mps.lastCommand());
    }

    private void payloadAtTheInput() {
        mps.setSensor_MPS_out(false);
        mps.setSensor_MPS_in(true);
    }

    @Test
    void aPayloadAtTheInputIsProcessed6sInTheOven5sUnderTheSawAndDeliveredOnTheConveyor() {
        payloadAtTheInput();

        assertEquals("process 6 5 CONVEYOR", mps.lastCommand());
        assertEquals("IdleBusy", zoneMPSMission.getActiveStateName(), "the MPS holds its zone while processing");
    }

    @Test
    void theProcessWaitsForTheMpsZone() {
        zoneMPS.publish(new AcquireRequestEventMessage()); // VGR2 is placing at the input

        payloadAtTheInput();
        assertEquals("setup", mps.lastCommand(), "no process while VGR2 holds the zone");

        zoneMPS.publish(new ReleaseRequestEventMessage()); // VGR2's arm is out
        assertEquals("process 6 5 CONVEYOR", mps.lastCommand());
    }

    @Test
    void withNothingAtTheInputOnceVgr2FreesTheZoneTheMpsReleasesItAndGoesBackToIdle() {
        zoneMPS.publish(new AcquireRequestEventMessage()); // VGR2 is placing at the input
        payloadAtTheInput();                               // its arm crosses the input light barrier...
        mps.setSensor_MPS_in(false);                       // ...but it leaves nothing there

        zoneMPS.publish(new ReleaseRequestEventMessage()); // VGR2's arm is out: the zone goes to the MPS

        assertEquals("setup", mps.lastCommand(), "nothing to process");
        assertEquals("Idle", mpsMission.getActiveStateName());
        assertEquals("IdleFree", zoneMPSMission.getActiveStateName(), "the MPS released the zone");

        mps.setSensor_MPS_in(true); // the next payload
        assertEquals("process 6 5 CONVEYOR", mps.lastCommand());
    }

    @Test
    void theZoneIsReleasedOnceThePayloadIsDelivered() {
        payloadAtTheInput();
        assertEquals("IdleBusy", zoneMPSMission.getActiveStateName());

        mps.commandSucceeds(); // payload delivered at the output

        assertEquals("IdleFree", zoneMPSMission.getActiveStateName());
        assertEquals("Idle", mpsMission.getActiveStateName());
    }

    @Test
    void vgr2CannotPlaceTheNextPayloadWhileTheMpsProcesses() {
        payloadAtTheInput();

        zoneMPS.publish(new AcquireRequestEventMessage()); // VGR2 brings the next payload
        assertEquals("IdleBusyRequested", zoneMPSMission.getActiveStateName(), "VGR2 waits for the MPS zone");

        mps.commandSucceeds(); // payload delivered: the zone goes to VGR2
        assertEquals("IdleBusy", zoneMPSMission.getActiveStateName());
    }

    /** The first payload is processed and delivered at the output, where it stays; the input is free again. */
    private void aProcessedPayloadStaysAtTheOutput() {
        payloadAtTheInput();
        mps.commandSucceeds();          // the first payload is delivered at the output...
        mps.setSensor_MPS_in(false);
        mps.setSensor_MPS_out(true);    // ...and stays there
        assertEquals("IdleFree", zoneMPSMission.getActiveStateName());
        mps.commands.clear();
    }

    @Test
    void aPayloadAtTheInputWhileTheOutputIsFullHoldsTheZoneUntilTheOutputIsCleared() {
        aProcessedPayloadStaysAtTheOutput();

        mps.setSensor_MPS_in(true);     // the next payload is placed at the input

        assertEquals(List.of(), mps.commands, "no process while the output is full");
        assertEquals("WaitForOutputFree", mpsMission.getActiveStateName());
        assertEquals("IdleBusy", zoneMPSMission.getActiveStateName(), "the MPS holds its zone: no other payload on its input");

        zoneMPS.publish(new AcquireRequestEventMessage()); // VGR2 brings another payload
        assertEquals("IdleBusyRequested", zoneMPSMission.getActiveStateName(), "VGR2 waits for the MPS zone");

        mps.setSensor_MPS_out(false);   // the output is cleared

        assertEquals("process 6 5 CONVEYOR", mps.lastCommand());
        assertEquals("IdleBusyRequested", zoneMPSMission.getActiveStateName(), "the zone is kept while processing");

        mps.commandSucceeds();          // delivered: the zone goes to VGR2
        assertEquals("Idle", mpsMission.getActiveStateName());
        assertEquals("IdleBusy", zoneMPSMission.getActiveStateName());
    }

    @Test
    void aPayloadPlacedByVgr2WhileTheOutputIsFullKeepsTheZoneOnceVgr2ReleasesIt() {
        aProcessedPayloadStaysAtTheOutput();
        zoneMPS.publish(new AcquireRequestEventMessage()); // VGR2 is placing at the input
        mps.setSensor_MPS_in(true);                        // its payload is at the input: the MPS requests the zone

        zoneMPS.publish(new ReleaseRequestEventMessage()); // VGR2's arm is out: the zone goes to the MPS

        assertEquals(List.of(), mps.commands, "no process while the output is full");
        assertEquals("WaitForOutputFree", mpsMission.getActiveStateName());
        assertEquals("IdleBusy", zoneMPSMission.getActiveStateName(), "the MPS keeps the zone");

        mps.setSensor_MPS_out(false);
        assertEquals("process 6 5 CONVEYOR", mps.lastCommand());
    }

    @Test
    void aPayloadRemovedFromTheInputWhileWaitingForTheOutputReleasesTheZone() {
        aProcessedPayloadStaysAtTheOutput();
        mps.setSensor_MPS_in(true);
        assertEquals("WaitForOutputFree", mpsMission.getActiveStateName());

        mps.setSensor_MPS_in(false);    // the payload is taken away by hand...
        mps.setSensor_MPS_out(false);   // ...then the output is cleared

        assertEquals(List.of(), mps.commands, "nothing to process");
        assertEquals("Idle", mpsMission.getActiveStateName());
        assertEquals("IdleFree", zoneMPSMission.getActiveStateName());
    }

    /** With the real VGR2 mission: VGR2 waits, holding its token, until the MPS has processed the payload at its input. */
    @Test
    void vgr2DoesNotPlaceOnTheMpsInputWhileAPayloadWaitsThereForTheOutput() {
        VacuumGripper2MissionTest.RecordingVacuumGripper vgr2 = new VacuumGripper2MissionTest.RecordingVacuumGripper();
        ZoneAdapterImpl zoneCB = new ZoneAdapterImpl("ZoneCB");
        ConveyorBeltMissionTest.RecordingConveyorBelt cb = new ConveyorBeltMissionTest.RecordingConveyorBelt();
        ZoneMissionCBNominal zoneCBMission = new ZoneMissionCBNominal(zoneCB, cb, new ZoneMissionCBNominalActionsImpl());
        VacuumGripper2NominalMission vgr2Mission = new VacuumGripper2NominalMission(vgr2, cb, zoneCB, zoneMPS,
                new VacuumGripper2NominalMissionActionsImpl());
        vgr2Mission.start();
        vgr2.commandSucceeds(); // setup
        vgr2.commandSucceeds(); // in standby
        assertEquals("Idle", vgr2Mission.getActiveStateName());

        aProcessedPayloadStaysAtTheOutput();
        mps.setSensor_MPS_in(true); // a payload at the input, the output still full
        assertEquals("WaitForOutputFree", mpsMission.getActiveStateName());

        cb.tokens(false, true);
        zoneCBMission.start(); // the next token waits at the CB swap: the CB zone tells VGR2, which brings it
        vgr2.commandSucceeds(); // picked
        vgr2.commandSucceeds(); // retracted
        vgr2.commandSucceeds(); // in standby: requests the MPS zone

        assertEquals("WaitForMPSZoneAcquisition", vgr2Mission.getActiveStateName());
        assertEquals("moveToSafePosition", vgr2.lastCommand(), "VGR2 does not place on the occupied input");

        mps.setSensor_MPS_out(false); // the output is cleared: the MPS processes its payload
        assertEquals("process 6 5 CONVEYOR", mps.lastCommand());
        assertEquals("WaitForMPSZoneAcquisition", vgr2Mission.getActiveStateName(), "nor while the MPS processes");

        mps.setSensor_MPS_in(false); // the payload left the input
        mps.commandSucceeds();       // delivered at the output: the MPS releases its zone

        assertEquals("placeNamed MPS_INPUT", vgr2.lastCommand());
        assertEquals("PlaceMPSinCMD", vgr2Mission.getActiveStateName());
    }

    /**
     * With the conveyor belt failed: VGR2 (VacuumGripper2CBFailureMission) takes a token from the dead belt while a
     * payload waits at the MPS input for the output; it holds it, and only places once the MPS delivered its payload
     * and released the zone: no stacking on the MPS input.
     */
    @Test
    void withTheBeltFailedVgr2DoesNotStackATokenOnThePayloadWaitingAtTheMpsInput() {
        VacuumGripper2MissionTest.RecordingVacuumGripper vgr2 = new VacuumGripper2MissionTest.RecordingVacuumGripper();
        ZoneAdapterImpl zoneCB = new ZoneAdapterImpl("ZoneCB");
        ZoneMissionDeadCB deadCBMission = new ZoneMissionDeadCB(zoneCB, new ZoneMissionDeadCBActions() { });
        VacuumGripper2CBFailureMission vgr2Mission = new VacuumGripper2CBFailureMission(vgr2, zoneCB, zoneMPS,
                new VacuumGripper2CBFailureMissionActionsImpl());

        aProcessedPayloadStaysAtTheOutput();
        mps.setSensor_MPS_in(true); // a payload at the input, the output still full
        assertEquals("WaitForOutputFree", mpsMission.getActiveStateName());
        assertEquals("IdleBusy", zoneMPSMission.getActiveStateName());

        deadCBMission.startAt("TokenOnSwap"); // a token waits on the dead belt
        vgr2Mission.startAt("CheckCB");
        assertEquals("pickNamed ALT_CB", vgr2.lastCommand());
        vgr2.commandSucceeds(); // picked: retract
        vgr2.commandSucceeds(); // retracted: tells the dead CB zone, requests the MPS zone

        assertEquals("WaitForMPSZoneAcquisition", vgr2Mission.getActiveStateName());
        assertEquals("retractArm", vgr2.lastCommand(), "VGR2 holds its token: no place on the occupied input");
        assertEquals("IdleBusyRequested", zoneMPSMission.getActiveStateName());

        mps.setSensor_MPS_out(false); // the output is cleared: the MPS processes its payload
        assertEquals("process 6 5 CONVEYOR", mps.lastCommand());
        assertEquals("WaitForMPSZoneAcquisition", vgr2Mission.getActiveStateName(), "nor while the MPS processes");

        mps.setSensor_MPS_in(false); // the payload left the input
        mps.commandSucceeds();       // delivered: the MPS releases its zone, granted to VGR2

        assertEquals("placeNamed MPS_INPUT", vgr2.lastCommand());
        assertEquals("PlaceMPSinCMD", vgr2Mission.getActiveStateName());
        assertEquals("IdleBusy", zoneMPSMission.getActiveStateName(), "held by VGR2 while it places");

        mps.setSensor_MPS_in(true); // VGR2's token at the input: the MPS waits for the zone
        assertEquals("WaitForZone", mpsMission.getActiveStateName());
        vgr2.commandSucceeds(); // placed: retract
        vgr2.commandSucceeds(); // retracted: releases the MPS zone, granted to the MPS
        assertEquals("process 6 5 CONVEYOR", mps.lastCommand());
        assertEquals("ProcessCMD", mpsMission.getActiveStateName());
    }
}
