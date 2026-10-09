package fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions;

import fr.inria.mbdo.mission.extensions.ren_mission_01.multiprocessingstationmissions.multiprocessingstationnominalmission.MultiProcessingStationNominalMissionActions;
import fr.inria.mbdo.mission.extensions.ren_mission_01.multiprocessingstationsystem.multiprocessingstation.MultiProcessingStationMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.multiprocessingstationsystem.multiprocessingstationcommands.MPSOutput;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.Zone;
import fr.inria.mbdo.mission.runtime.rtc.event.Event;

/**
 * Default (logging) implementation of MultiProcessingStationNominalMission
 * actions.
 * Replace with real logic when connecting to hardware.
 */
public class MultiProcessingStationNominalMissionActionsImpl implements MultiProcessingStationNominalMissionActions {

    // the process run on each payload: 3 s in the oven, 2 s under the saw, delivered at the end of the conveyor
    private static final int OVEN_TIME_S = 3;
    private static final int SAW_TIME_S = 2;
    private static final MPSOutput OUTPUT = MPSOutput.CONVEYOR;

    @Override
    public void processPayload(Event event, MultiProcessingStationMachine multiProcessingStation, Zone zoneMPS) {
        multiProcessingStation.process(OVEN_TIME_S, SAW_TIME_S, OUTPUT);
    }
}
