package fr.inria.mbdo.mission.extensions.ren_mission_01_impl;

import fr.inria.mbdo.mission.extensions.ren_mission_01.multiprocessingstationmissions.multiprocessingstationnominalmission.MultiProcessingStationNominalMission;
import fr.inria.mbdo.mission.extensions.ren_mission_01.multiprocessingstationsystem.multiprocessingstation.AbstractMultiProcessingStationMachineAdapter;
import fr.inria.mbdo.mission.extensions.ren_mission_01.multiprocessingstationsystem.multiprocessingstationcommands.MPSOutput;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.zonemissionmpsnominal.ZoneMissionMPSNominal;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.zonemissionmpsnominal.ZoneMissionMPSNominalActions;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.AcquireRequestEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.ReleaseRequestEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.ZoneAdapterImpl;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions.MultiProcessingStationNominalMissionActionsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The MPS processes a payload placed at its input once it holds the MPS zone: 3 s oven, 2 s saw, conveyor output. */
class MultiProcessingStationMissionTest {

    static final class RecordingMultiProcessingStation extends AbstractMultiProcessingStationMachineAdapter {
        final List<String> commands = new ArrayList<>();

        RecordingMultiProcessingStation() {
            super("REN_MISSION_01/MPS01");
        }

        String lastCommand() {
            return commands.isEmpty() ? null : commands.get(commands.size() - 1);
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
    void aPayloadAtTheInputIsProcessed3sInTheOven2sUnderTheSawAndDeliveredOnTheConveyor() {
        payloadAtTheInput();

        assertEquals("process 3 2 CONVEYOR", mps.lastCommand());
        assertEquals("IdleBusy", zoneMPSMission.getActiveStateName(), "the MPS holds its zone while processing");
    }

    @Test
    void theProcessWaitsForTheMpsZone() {
        zoneMPS.publish(new AcquireRequestEventMessage()); // VGR2 is placing at the input

        payloadAtTheInput();
        assertEquals("setup", mps.lastCommand(), "no process while VGR2 holds the zone");

        zoneMPS.publish(new ReleaseRequestEventMessage()); // VGR2's arm is out
        assertEquals("process 3 2 CONVEYOR", mps.lastCommand());
    }
}
