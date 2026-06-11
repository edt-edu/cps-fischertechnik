package fr.inria.mbdo.mission.extensions.ren_mission_01_impl;

import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippermissions.vacuumgripper2nominalmission.VacuumGripper2NominalMissionActions;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgripper.VacuumGripperMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.Zone;
import fr.inria.mbdo.mission.runtime.rtc.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default (logging) implementation of VacuumGripper2NominalMission actions.
 * Replace with real logic when connecting to hardware.
 */
public class VacuumGripper2NominalMissionActionsImpl implements VacuumGripper2NominalMissionActions {

    private static final Logger log = LoggerFactory.getLogger(VacuumGripper2NominalMissionActionsImpl.class);

    @Override
    public void placeMPSin(Event event, VacuumGripperMachine vacuumGripper, Zone zoneCB, Zone zoneMPS) {
        log.info("VacuumGripper2 placeMPSin triggered by {}", event.getClass().getSimpleName());
    }

    @Override
    public void pickCBswap(Event event, VacuumGripperMachine vacuumGripper, Zone zoneCB, Zone zoneMPS) {
        log.info("VacuumGripper2 pickCBswap triggered by {}", event.getClass().getSimpleName());
    }

    @Override
    public void releaseCBZoneAndAcquireMPSZone(Event event, VacuumGripperMachine vacuumGripper, Zone zoneCB, Zone zoneMPS) {
        log.info("VacuumGripper2 releaseCBZoneAndAcquireMPSZone triggered by {}", event.getClass().getSimpleName());
    }

    @Override
    public void goToStandby(Event event, VacuumGripperMachine vacuumGripper, Zone zoneCB, Zone zoneMPS) {
        log.info("VacuumGripper2 goToStandby triggered by {}", event.getClass().getSimpleName());
    }
}
