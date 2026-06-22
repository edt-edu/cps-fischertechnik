package fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions;

import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltmissions.conveyorbeltnominalmission.ConveyorBeltNominalMissionActions;
import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltsystem.conveyorbelt.ConveyorBeltMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgripper.VacuumGripperMachine;
import fr.inria.mbdo.mission.runtime.rtc.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default (logging) implementation of ConveyorBeltNominalMission actions.
 * Replace with real logic when connecting to hardware.
 */
public class ConveyorBeltNominalMissionActionsImpl implements ConveyorBeltNominalMissionActions {

    private static final Logger log = LoggerFactory.getLogger(ConveyorBeltNominalMissionActionsImpl.class);

    @Override
    public void notifyVgr1AndVgr2(Event event, ConveyorBeltMachine conveyorBelt, VacuumGripperMachine vacuumGripper1, VacuumGripperMachine vacuumGripper2) {
        log.info("ConveyorBelt performActionUsage triggered by {}", event.getClass().getSimpleName());
    }
}
