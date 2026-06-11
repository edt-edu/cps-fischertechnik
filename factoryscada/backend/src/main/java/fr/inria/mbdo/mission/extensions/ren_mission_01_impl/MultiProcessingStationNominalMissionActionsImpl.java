package fr.inria.mbdo.mission.extensions.ren_mission_01_impl;

import fr.inria.mbdo.mission.extensions.ren_mission_01.multiprocessingstationmissions.multiprocessingstationnominalmission.MultiProcessingStationNominalMissionActions;
import fr.inria.mbdo.mission.extensions.ren_mission_01.multiprocessingstationsystem.multiprocessingstation.MultiProcessingStationMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.Zone;
import fr.inria.mbdo.mission.runtime.rtc.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default (logging) implementation of MultiProcessingStationNominalMission
 * actions.
 * Replace with real logic when connecting to hardware.
 */
public class MultiProcessingStationNominalMissionActionsImpl implements MultiProcessingStationNominalMissionActions {

    private static final Logger log = LoggerFactory.getLogger(MultiProcessingStationNominalMissionActionsImpl.class);

    @Override
    public void broadcastCompletion(Event event, MultiProcessingStationMachine multiProcessingStation, Zone zoneMPS) {
        log.info("MultiProcessingStation broadcastCompletion triggered by {}", event.getClass().getSimpleName());
    }
}
