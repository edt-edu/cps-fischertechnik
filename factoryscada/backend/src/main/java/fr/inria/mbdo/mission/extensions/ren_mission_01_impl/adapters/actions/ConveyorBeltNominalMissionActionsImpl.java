package fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions;

import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltmissions.conveyorbeltnominalmission.ConveyorBeltNominalMissionActions;
import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltsystem.conveyorbelt.ConveyorBeltMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltsystem.conveyorbeltmessages.FeedFreeEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltsystem.conveyorbeltmessages.SwapBusyEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltsystem.conveyorbeltmessages.TokenAtFeedEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.ConveyorBeltAdapterImpl;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgripper.VacuumGripperMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.Zone;
import fr.inria.mbdo.mission.runtime.rtc.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default (logging) implementation of ConveyorBeltNominalMission actions.
 * Replace with real logic when connecting to hardware.
 */
public class ConveyorBeltNominalMissionActionsImpl implements ConveyorBeltNominalMissionActions {

    private static final Logger log = LoggerFactory.getLogger(ConveyorBeltNominalMissionActionsImpl.class);

    /**
     * The token left the feed and waits at the swap (MOVE_TO_SENSOR done). The CB zone is told first: the feed is free
     * for VGR1 again, and the zone knows the token at the swap when VGR2, told next, asks it to pick it.
     */
    @Override
    public void notifyTokenAtSwap(Event event, ConveyorBeltMachine conveyorBelt, VacuumGripperMachine vacuumGripper1,
                                  VacuumGripperMachine vacuumGripper2, Zone zoneCB) {
        log.info("ConveyorBelt notifies FeedFree to the CB zone and VGR1, SwapBusy to VGR2 (triggered by {})",
                event.getClass().getSimpleName());
        zoneCB.publish(new FeedFreeEventMessage());
        vacuumGripper1.publish(new FeedFreeEventMessage());
        vacuumGripper2.publish(new SwapBusyEventMessage());
    }

    /**
     * On (re)start, tokens may already be on the belt: a token at the swap is announced to VGR2 (which takes it once
     * free); else a token at the feed is announced to the belt itself, which then moves it to the swap.
     */
    @Override
    public void checkBelt(Event event, ConveyorBeltMachine conveyorBelt, VacuumGripperMachine vacuumGripper1,
                          VacuumGripperMachine vacuumGripper2, Zone zoneCB) {
        if (ConveyorBeltAdapterImpl.tokenAtSwap(conveyorBelt)) {
            log.info("ConveyorBelt starts with a token at the swap: SwapBusy to VGR2");
            vacuumGripper2.publish(new SwapBusyEventMessage());
        } else if (ConveyorBeltAdapterImpl.tokenAtFeed(conveyorBelt)) {
            log.info("ConveyorBelt starts with a token at the feed: moves it to the swap");
            conveyorBelt.publish(new TokenAtFeedEventMessage());
        }
    }
}
