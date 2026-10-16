package fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.zonemissioncbnominal;

import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltsystem.conveyorbelt.ConveyorBeltMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltsystem.conveyorbeltmessages.FeedFreeEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltsystem.conveyorbeltmessages.SwapBusyEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltsystem.conveyorbeltmessages.TokenAtFeedEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.Zone;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.CBSlotFreeEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.CBSlotRequestEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.CBTokenRequestEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.NoCBSlotFreeEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.NoTokenOnCBEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.PickFromSwapEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.PlaceOnFeedEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.TokenOnCBEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.TokenPickedFromCBEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.TokenPlacedOnCBEventMessage;
import fr.inria.mbdo.mission.runtime.api.AbstractMissionStrategy;
import fr.inria.mbdo.mission.runtime.api.MachineAdapter;
import fr.inria.mbdo.mission.runtime.rtc.def.RuntimeState;
import fr.inria.mbdo.mission.runtime.rtc.def.RuntimeTransition;
import fr.inria.mbdo.mission.runtime.rtc.event.CompletionEvent;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * From ZoneMissions::ZoneMissionCBNominal
 * Zone of the conveyor belt in the nominal production: knows whether a token is on the feed and on the swap, and
 *            lets one gripper at a time over the belt. VGR1 only places on a free feed, VGR2 only picks at the swap.
 */
public class ZoneMissionCBNominal extends AbstractMissionStrategy {
  private static final Logger logger = LoggerFactory.getLogger("ZoneMissionCBNominal");

  public static final String DOT_SCHEMA = "digraph ZoneMissionCBNominal {\n"
      + "    fontname=\"Helvetica,Arial,sans-serif\"\n"
      + "    node [fontname=\"Helvetica,Arial,sans-serif\"]\n"
      + "    edge [fontname=\"Helvetica,Arial,sans-serif\"]\n"
      + "    rankdir=LR;\n"
      + "    node [shape=point, label=\"\"]; __init__;\n"
      + "    node [shape=circle, style=\"\", fillcolor=\"\"];\n"
      + "    \"CheckBelt\";\n"
      + "    \"Empty\";\n"
      + "    \"TokenOnFeed\";\n"
      + "    \"TokenOnSwap\";\n"
      + "    \"SwapThenFeed\";\n"
      + "    \"VGR1PlacingOnFeed\";\n"
      + "    \"VGR1PlacingOnFeedTokenMovedToSwap\";\n"
      + "    \"VGR1PlacingOnFeedAfterSwap\";\n"
      + "    \"VGR2PickingLastToken\";\n"
      + "    \"VGR2PickingSwapBeforeFeed\";\n"
      + "    \"VGR2PickingSwapFeedMovedToSwap\";\n"
      + "\n"
      + "    __init__ -> \"CheckBelt\" [label=\"ε\"];\n"
      + "    \"CheckBelt\" -> \"Empty\" [label=\"ε / checkBelt\"];\n"
      + "    \"Empty\" -> \"VGR1PlacingOnFeed\" [label=\"CBSlotRequestEventMessage / send ZonesSystem::ZonesMessages::PlaceOnFeedEventMessage -> zone\"];\n"
      + "    \"Empty\" -> \"Empty\" [label=\"CBTokenRequestEventMessage / send ZonesSystem::ZonesMessages::NoTokenOnCBEventMessage -> zone\"];\n"
      + "    \"Empty\" -> \"TokenOnSwap\" [label=\"SwapBusyEventMessage\"];\n"
      + "    \"Empty\" -> \"TokenOnFeed\" [label=\"TokenAtFeedEventMessage\"];\n"
      + "    \"TokenOnFeed\" -> \"TokenOnFeed\" [label=\"CBSlotRequestEventMessage / send ZonesSystem::ZonesMessages::NoCBSlotFreeEventMessage -> zone\"];\n"
      + "    \"TokenOnFeed\" -> \"TokenOnFeed\" [label=\"CBTokenRequestEventMessage / send ZonesSystem::ZonesMessages::NoTokenOnCBEventMessage -> zone\"];\n"
      + "    \"TokenOnFeed\" -> \"TokenOnSwap\" [label=\"FeedFreeEventMessage / send ZonesSystem::ZonesMessages::CBSlotFreeEventMessage -> zone\"];\n"
      + "    \"TokenOnSwap\" -> \"VGR1PlacingOnFeedAfterSwap\" [label=\"CBSlotRequestEventMessage / send ZonesSystem::ZonesMessages::PlaceOnFeedEventMessage -> zone\"];\n"
      + "    \"TokenOnSwap\" -> \"VGR2PickingLastToken\" [label=\"CBTokenRequestEventMessage / send ZonesSystem::ZonesMessages::PickFromSwapEventMessage -> zone\"];\n"
      + "    \"TokenOnSwap\" -> \"SwapThenFeed\" [label=\"TokenAtFeedEventMessage\"];\n"
      + "    \"SwapThenFeed\" -> \"SwapThenFeed\" [label=\"CBSlotRequestEventMessage / send ZonesSystem::ZonesMessages::NoCBSlotFreeEventMessage -> zone\"];\n"
      + "    \"SwapThenFeed\" -> \"VGR2PickingSwapBeforeFeed\" [label=\"CBTokenRequestEventMessage / send ZonesSystem::ZonesMessages::PickFromSwapEventMessage -> zone\"];\n"
      + "    \"VGR1PlacingOnFeed\" -> \"TokenOnFeed\" [label=\"TokenPlacedOnCBEventMessage\"];\n"
      + "    \"VGR1PlacingOnFeed\" -> \"VGR1PlacingOnFeed\" [label=\"CBTokenRequestEventMessage / send ZonesSystem::ZonesMessages::NoTokenOnCBEventMessage -> zone\"];\n"
      + "    \"VGR1PlacingOnFeed\" -> \"VGR1PlacingOnFeedTokenMovedToSwap\" [label=\"FeedFreeEventMessage\"];\n"
      + "    \"VGR1PlacingOnFeedTokenMovedToSwap\" -> \"TokenOnSwap\" [label=\"TokenPlacedOnCBEventMessage / send ZonesSystem::ZonesMessages::TokenOnCBEventMessage -> zone\"];\n"
      + "    \"VGR1PlacingOnFeedTokenMovedToSwap\" -> \"VGR1PlacingOnFeedTokenMovedToSwap\" [label=\"CBTokenRequestEventMessage / send ZonesSystem::ZonesMessages::NoTokenOnCBEventMessage -> zone\"];\n"
      + "    \"VGR1PlacingOnFeedAfterSwap\" -> \"SwapThenFeed\" [label=\"TokenPlacedOnCBEventMessage / send ZonesSystem::ZonesMessages::TokenOnCBEventMessage -> zone\"];\n"
      + "    \"VGR1PlacingOnFeedAfterSwap\" -> \"VGR1PlacingOnFeedAfterSwap\" [label=\"CBTokenRequestEventMessage / send ZonesSystem::ZonesMessages::NoTokenOnCBEventMessage -> zone\"];\n"
      + "    \"VGR2PickingLastToken\" -> \"Empty\" [label=\"TokenPickedFromCBEventMessage / send ZonesSystem::ZonesMessages::CBSlotFreeEventMessage -> zone\"];\n"
      + "    \"VGR2PickingLastToken\" -> \"VGR2PickingLastToken\" [label=\"CBSlotRequestEventMessage / send ZonesSystem::ZonesMessages::NoCBSlotFreeEventMessage -> zone\"];\n"
      + "    \"VGR2PickingSwapBeforeFeed\" -> \"TokenOnFeed\" [label=\"TokenPickedFromCBEventMessage\"];\n"
      + "    \"VGR2PickingSwapBeforeFeed\" -> \"VGR2PickingSwapBeforeFeed\" [label=\"CBSlotRequestEventMessage / send ZonesSystem::ZonesMessages::NoCBSlotFreeEventMessage -> zone\"];\n"
      + "    \"VGR2PickingSwapBeforeFeed\" -> \"VGR2PickingSwapFeedMovedToSwap\" [label=\"FeedFreeEventMessage\"];\n"
      + "    \"VGR2PickingSwapFeedMovedToSwap\" -> \"TokenOnSwap\" [label=\"TokenPickedFromCBEventMessage / send ZonesSystem::ZonesMessages::CBSlotFreeEventMessage -> zone\"];\n"
      + "    \"VGR2PickingSwapFeedMovedToSwap\" -> \"VGR2PickingSwapFeedMovedToSwap\" [label=\"CBSlotRequestEventMessage / send ZonesSystem::ZonesMessages::NoCBSlotFreeEventMessage -> zone\"];\n"
      + "}";

  private final ZoneMissionCBNominalActions actions;

  private final Zone zone;

  private final ConveyorBeltMachine conveyorBelt;

  public ZoneMissionCBNominal(Zone zone, ConveyorBeltMachine conveyorBelt,
      ZoneMissionCBNominalActions actions) {
    this.zone = zone;
    this.conveyorBelt = conveyorBelt;
    this.actions = actions;

    // States are built from the collected transitions.
    RuntimeState checkBelt = new RuntimeState("CheckBelt");
    RuntimeState empty = new RuntimeState("Empty");
    RuntimeState tokenOnFeed = new RuntimeState("TokenOnFeed");
    RuntimeState tokenOnSwap = new RuntimeState("TokenOnSwap");
    RuntimeState swapThenFeed = new RuntimeState("SwapThenFeed");
    RuntimeState vGR1PlacingOnFeed = new RuntimeState("VGR1PlacingOnFeed");
    RuntimeState vGR1PlacingOnFeedTokenMovedToSwap = new RuntimeState("VGR1PlacingOnFeedTokenMovedToSwap");
    RuntimeState vGR1PlacingOnFeedAfterSwap = new RuntimeState("VGR1PlacingOnFeedAfterSwap");
    RuntimeState vGR2PickingLastToken = new RuntimeState("VGR2PickingLastToken");
    RuntimeState vGR2PickingSwapBeforeFeed = new RuntimeState("VGR2PickingSwapBeforeFeed");
    RuntimeState vGR2PickingSwapFeedMovedToSwap = new RuntimeState("VGR2PickingSwapFeedMovedToSwap");

    // Transitions connect triggers, runtime actions, and next-state targets.
    this.runtime.setEntryTransition(new RuntimeTransition(CompletionEvent.class, event -> true, event -> { }, checkBelt));
    checkBelt.addTransition(new RuntimeTransition(CompletionEvent.class, event -> true, event -> this.actions.checkBelt(event, this.zone, this.conveyorBelt), empty));
    empty.addTransition(new RuntimeTransition(CBSlotRequestEventMessage.class, event -> true, event -> this.zone.publish(new PlaceOnFeedEventMessage()), vGR1PlacingOnFeed, this.zone));
    empty.addTransition(new RuntimeTransition(CBTokenRequestEventMessage.class, event -> true, event -> this.zone.publish(new NoTokenOnCBEventMessage()), empty, this.zone));
    empty.addTransition(new RuntimeTransition(SwapBusyEventMessage.class, event -> true, event -> { }, tokenOnSwap, this.zone));
    empty.addTransition(new RuntimeTransition(TokenAtFeedEventMessage.class, event -> true, event -> { }, tokenOnFeed, this.zone));
    tokenOnFeed.addTransition(new RuntimeTransition(CBSlotRequestEventMessage.class, event -> true, event -> this.zone.publish(new NoCBSlotFreeEventMessage()), tokenOnFeed, this.zone));
    tokenOnFeed.addTransition(new RuntimeTransition(CBTokenRequestEventMessage.class, event -> true, event -> this.zone.publish(new NoTokenOnCBEventMessage()), tokenOnFeed, this.zone));
    tokenOnFeed.addTransition(new RuntimeTransition(FeedFreeEventMessage.class, event -> true, event -> this.zone.publish(new CBSlotFreeEventMessage()), tokenOnSwap, this.zone));
    tokenOnSwap.addTransition(new RuntimeTransition(CBSlotRequestEventMessage.class, event -> true, event -> this.zone.publish(new PlaceOnFeedEventMessage()), vGR1PlacingOnFeedAfterSwap, this.zone));
    tokenOnSwap.addTransition(new RuntimeTransition(CBTokenRequestEventMessage.class, event -> true, event -> this.zone.publish(new PickFromSwapEventMessage()), vGR2PickingLastToken, this.zone));
    tokenOnSwap.addTransition(new RuntimeTransition(TokenAtFeedEventMessage.class, event -> true, event -> { }, swapThenFeed, this.zone));
    swapThenFeed.addTransition(new RuntimeTransition(CBSlotRequestEventMessage.class, event -> true, event -> this.zone.publish(new NoCBSlotFreeEventMessage()), swapThenFeed, this.zone));
    swapThenFeed.addTransition(new RuntimeTransition(CBTokenRequestEventMessage.class, event -> true, event -> this.zone.publish(new PickFromSwapEventMessage()), vGR2PickingSwapBeforeFeed, this.zone));
    vGR1PlacingOnFeed.addTransition(new RuntimeTransition(TokenPlacedOnCBEventMessage.class, event -> true, event -> { }, tokenOnFeed, this.zone));
    vGR1PlacingOnFeed.addTransition(new RuntimeTransition(CBTokenRequestEventMessage.class, event -> true, event -> this.zone.publish(new NoTokenOnCBEventMessage()), vGR1PlacingOnFeed, this.zone));
    vGR1PlacingOnFeed.addTransition(new RuntimeTransition(FeedFreeEventMessage.class, event -> true, event -> { }, vGR1PlacingOnFeedTokenMovedToSwap, this.zone));
    vGR1PlacingOnFeedTokenMovedToSwap.addTransition(new RuntimeTransition(TokenPlacedOnCBEventMessage.class, event -> true, event -> this.zone.publish(new TokenOnCBEventMessage()), tokenOnSwap, this.zone));
    vGR1PlacingOnFeedTokenMovedToSwap.addTransition(new RuntimeTransition(CBTokenRequestEventMessage.class, event -> true, event -> this.zone.publish(new NoTokenOnCBEventMessage()), vGR1PlacingOnFeedTokenMovedToSwap, this.zone));
    vGR1PlacingOnFeedAfterSwap.addTransition(new RuntimeTransition(TokenPlacedOnCBEventMessage.class, event -> true, event -> this.zone.publish(new TokenOnCBEventMessage()), swapThenFeed, this.zone));
    vGR1PlacingOnFeedAfterSwap.addTransition(new RuntimeTransition(CBTokenRequestEventMessage.class, event -> true, event -> this.zone.publish(new NoTokenOnCBEventMessage()), vGR1PlacingOnFeedAfterSwap, this.zone));
    vGR2PickingLastToken.addTransition(new RuntimeTransition(TokenPickedFromCBEventMessage.class, event -> true, event -> this.zone.publish(new CBSlotFreeEventMessage()), empty, this.zone));
    vGR2PickingLastToken.addTransition(new RuntimeTransition(CBSlotRequestEventMessage.class, event -> true, event -> this.zone.publish(new NoCBSlotFreeEventMessage()), vGR2PickingLastToken, this.zone));
    vGR2PickingSwapBeforeFeed.addTransition(new RuntimeTransition(TokenPickedFromCBEventMessage.class, event -> true, event -> { }, tokenOnFeed, this.zone));
    vGR2PickingSwapBeforeFeed.addTransition(new RuntimeTransition(CBSlotRequestEventMessage.class, event -> true, event -> this.zone.publish(new NoCBSlotFreeEventMessage()), vGR2PickingSwapBeforeFeed, this.zone));
    vGR2PickingSwapBeforeFeed.addTransition(new RuntimeTransition(FeedFreeEventMessage.class, event -> true, event -> { }, vGR2PickingSwapFeedMovedToSwap, this.zone));
    vGR2PickingSwapFeedMovedToSwap.addTransition(new RuntimeTransition(TokenPickedFromCBEventMessage.class, event -> true, event -> this.zone.publish(new CBSlotFreeEventMessage()), tokenOnSwap, this.zone));
    vGR2PickingSwapFeedMovedToSwap.addTransition(new RuntimeTransition(CBSlotRequestEventMessage.class, event -> true, event -> this.zone.publish(new NoCBSlotFreeEventMessage()), vGR2PickingSwapFeedMovedToSwap, this.zone));

    // Subscribe each mission machine to trigger event types used by this mission.
    zone.subscribe(CBSlotRequestEventMessage.class, event -> onEvent(zone, event));
    zone.subscribe(CBTokenRequestEventMessage.class, event -> onEvent(zone, event));
    zone.subscribe(SwapBusyEventMessage.class, event -> onEvent(zone, event));
    zone.subscribe(TokenAtFeedEventMessage.class, event -> onEvent(zone, event));
    zone.subscribe(FeedFreeEventMessage.class, event -> onEvent(zone, event));
    zone.subscribe(TokenPlacedOnCBEventMessage.class, event -> onEvent(zone, event));
    zone.subscribe(TokenPickedFromCBEventMessage.class, event -> onEvent(zone, event));
  }

  @Override
  public String getName() {
    return "ZoneMissionCBNominal";
  }

  @Override
  public List<MachineAdapter> getMachines() {
    return List.<MachineAdapter>of(this.zone, this.conveyorBelt);
  }

  @Override
  public String getDescription() {
    return "Zone of the conveyor belt in the nominal production: knows whether a token is on the feed and on the swap, and\n"
            + "           lets one gripper at a time over the belt. VGR1 only places on a free feed, VGR2 only picks at the swap.";
  }

  public static String toDot() {
    return DOT_SCHEMA;
  }

  @Override
  public String getDotGraph() {
    return DOT_SCHEMA;
  }
}
