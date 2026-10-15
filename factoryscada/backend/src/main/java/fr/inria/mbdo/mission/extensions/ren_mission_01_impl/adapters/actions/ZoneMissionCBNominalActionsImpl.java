package fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions;

import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltsystem.conveyorbelt.ConveyorBeltMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltsystem.conveyorbeltmessages.SwapBusyEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltsystem.conveyorbeltmessages.TokenAtFeedEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.zonemissioncbnominal.ZoneMissionCBNominalActions;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.Zone;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.CBSlotFreeEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.TokenOnCBEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.ConveyorBeltAdapterImpl;
import fr.inria.mbdo.mission.runtime.rtc.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ZoneMissionCBNominal actions: the CB zone knows the tokens on the feed and the swap of the conveyor belt, and the
 * gripper over it. It answers the grippers and the belt on the zone adapter itself (see the mission).
 */
public class ZoneMissionCBNominalActionsImpl implements ZoneMissionCBNominalActions {

    private static final Logger log = LoggerFactory.getLogger(ZoneMissionCBNominalActionsImpl.class);

    /**
     * Started, or migrated (from the dead CB zone, after a repair): the light barriers (active-low, not received yet =
     * free) give the tokens on the belt. SwapBusy then TokenAtFeed, published on the zone, lead the mission from Empty
     * to TokenOnSwap, TokenOnFeed or SwapThenFeed (processed once the check completed, in this order). Then a gripper
     * already waiting (migrated) is told: VGR1 if the feed is free, VGR2 if a token is at the swap; both ask again.
     */
    @Override
    public void checkBelt(Event event, Zone zone, ConveyorBeltMachine conveyorBelt) {
        boolean atFeed = ConveyorBeltAdapterImpl.tokenAtFeed(conveyorBelt);
        boolean atSwap = ConveyorBeltAdapterImpl.tokenAtSwap(conveyorBelt);
        log.info("ZoneCB starts with a token at the feed: {}, at the swap: {}", atFeed, atSwap);
        if (atSwap) {
            zone.publish(new SwapBusyEventMessage());
        }
        if (atFeed) {
            zone.publish(new TokenAtFeedEventMessage());
        }
        if (!atFeed) {
            zone.publish(new CBSlotFreeEventMessage());
        }
        if (atSwap) {
            zone.publish(new TokenOnCBEventMessage());
        }
    }
}
