package fr.inria.mbdo.mission.extensions.ren_mission_01_impl;

import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinesystem.sortingline.SortingLineMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippermissions.VacuumGripper1NominalMissionActions;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgripper.VacuumGripperMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.Zone;
import fr.inria.mbdo.mission.runtime.rtc.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default (logging) implementation of VacuumGripper1NominalMission actions.
 * Replace with real logic when connecting to hardware.
 */
public class VacuumGripper1NominalMissionActionsImpl implements VacuumGripper1NominalMissionActions {

    private static final Logger log = LoggerFactory.getLogger(VacuumGripper1NominalMissionActionsImpl.class);

    @Override
    public void pickWhite(Event event, VacuumGripperMachine vacuumGripper, SortingLineMachine sortingLine,
            Zone zoneCB) {
        log.info("VacuumGripper1 pickWhite triggered by {}", event.getClass().getSimpleName());
    }

    @Override
    public void pickRed(Event event, VacuumGripperMachine vacuumGripper, SortingLineMachine sortingLine, Zone zoneCB) {
        log.info("VacuumGripper1 pickRed triggered by {}", event.getClass().getSimpleName());
    }

    @Override
    public void goToStandby(Event event, VacuumGripperMachine vacuumGripper, SortingLineMachine sortingLine,
            Zone zoneCB) {
        log.info("VacuumGripper1 goToStandby triggered by {}", event.getClass().getSimpleName());
    }

    @Override
    public void placeConveyoBeltFeed(Event event, VacuumGripperMachine vacuumGripper, SortingLineMachine sortingLine,
            Zone zoneCB) {
        log.info("VacuumGripper1 placeConveyoBeltFeed triggered by {}", event.getClass().getSimpleName());
    }

    @Override
    public void pickBlue(Event event, VacuumGripperMachine vacuumGripper, SortingLineMachine sortingLine, Zone zoneCB) {
        log.info("VacuumGripper1 pickBlue triggered by {}", event.getClass().getSimpleName());
    }
}
