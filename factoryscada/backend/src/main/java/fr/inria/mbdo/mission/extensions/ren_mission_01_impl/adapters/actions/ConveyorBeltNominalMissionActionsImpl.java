package fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions;

import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltmissions.conveyorbeltnominalmission.ConveyorBeltNominalMissionActions;
import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltsystem.conveyorbelt.ConveyorBeltMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltsystem.conveyorbeltmessages.FeedFreeEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltsystem.conveyorbeltmessages.SwapBusyEventMessage;
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
        // the token left the feed and waits at the swap (MOVE_TO_SENSOR done)
        log.info("ConveyorBelt notifies FeedFree to VGR1 and SwapBusy to VGR2 (triggered by {})",
                event.getClass().getSimpleName());
        vacuumGripper1.publish(new FeedFreeEventMessage());
        vacuumGripper2.publish(new SwapBusyEventMessage());
    }
}
