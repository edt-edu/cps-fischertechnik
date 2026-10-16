package fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.zonemissiondeadcb;

import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.Zone;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.CBSlotFreeEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.CBSlotRequestEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.CBTokenRequestEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.NoCBSlotFreeEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.NoTokenOnCBEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.PickFromFeedEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.PickFromSwapEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.PlaceOnFeedEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.PlaceOnSwapEventMessage;
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
 * From ZoneMissions::ZoneMissionDeadCB
 * Zone of the dead conveyor belt: knows which of its two slots (feed, swap) hold a token, and in which order, and
 *            lets one gripper at a time over the belt
 */
public class ZoneMissionDeadCB extends AbstractMissionStrategy {
  private static final Logger logger = LoggerFactory.getLogger("ZoneMissionDeadCB");

  public static final String DOT_SCHEMA = "digraph ZoneMissionDeadCB {\n"
      + "    fontname=\"Helvetica,Arial,sans-serif\"\n"
      + "    node [fontname=\"Helvetica,Arial,sans-serif\"]\n"
      + "    edge [fontname=\"Helvetica,Arial,sans-serif\"]\n"
      + "    rankdir=LR;\n"
      + "    node [shape=point, label=\"\"]; __init__;\n"
      + "    node [shape=circle, style=\"\", fillcolor=\"\"];\n"
      + "    \"Empty\";\n"
      + "    \"TokenOnFeed\";\n"
      + "    \"TokenOnSwap\";\n"
      + "    \"FeedThenSwap\";\n"
      + "    \"SwapThenFeed\";\n"
      + "    \"VGR1PlacingOnSwap\";\n"
      + "    \"VGR1PlacingOnSwapAfterFeed\";\n"
      + "    \"VGR1PlacingOnFeedAfterSwap\";\n"
      + "    \"VGR2PickingLastToken\";\n"
      + "    \"VGR2PickingFeedBeforeSwap\";\n"
      + "    \"VGR2PickingSwapBeforeFeed\";\n"
      + "\n"
      + "    __init__ -> \"Empty\" [label=\"ε\"];\n"
      + "    \"Empty\" -> \"VGR1PlacingOnSwap\" [label=\"CBSlotRequestEventMessage / send ZonesSystem::ZonesMessages::PlaceOnSwapEventMessage -> zone\"];\n"
      + "    \"Empty\" -> \"Empty\" [label=\"CBTokenRequestEventMessage / send ZonesSystem::ZonesMessages::NoTokenOnCBEventMessage -> zone\"];\n"
      + "    \"TokenOnFeed\" -> \"VGR1PlacingOnSwapAfterFeed\" [label=\"CBSlotRequestEventMessage / send ZonesSystem::ZonesMessages::PlaceOnSwapEventMessage -> zone\"];\n"
      + "    \"TokenOnFeed\" -> \"VGR2PickingLastToken\" [label=\"CBTokenRequestEventMessage / send ZonesSystem::ZonesMessages::PickFromFeedEventMessage -> zone\"];\n"
      + "    \"TokenOnSwap\" -> \"VGR1PlacingOnFeedAfterSwap\" [label=\"CBSlotRequestEventMessage / send ZonesSystem::ZonesMessages::PlaceOnFeedEventMessage -> zone\"];\n"
      + "    \"TokenOnSwap\" -> \"VGR2PickingLastToken\" [label=\"CBTokenRequestEventMessage / send ZonesSystem::ZonesMessages::PickFromSwapEventMessage -> zone\"];\n"
      + "    \"FeedThenSwap\" -> \"FeedThenSwap\" [label=\"CBSlotRequestEventMessage / send ZonesSystem::ZonesMessages::NoCBSlotFreeEventMessage -> zone\"];\n"
      + "    \"FeedThenSwap\" -> \"VGR2PickingFeedBeforeSwap\" [label=\"CBTokenRequestEventMessage / send ZonesSystem::ZonesMessages::PickFromFeedEventMessage -> zone\"];\n"
      + "    \"SwapThenFeed\" -> \"SwapThenFeed\" [label=\"CBSlotRequestEventMessage / send ZonesSystem::ZonesMessages::NoCBSlotFreeEventMessage -> zone\"];\n"
      + "    \"SwapThenFeed\" -> \"VGR2PickingSwapBeforeFeed\" [label=\"CBTokenRequestEventMessage / send ZonesSystem::ZonesMessages::PickFromSwapEventMessage -> zone\"];\n"
      + "    \"VGR1PlacingOnSwap\" -> \"TokenOnSwap\" [label=\"TokenPlacedOnCBEventMessage / send ZonesSystem::ZonesMessages::TokenOnCBEventMessage -> zone\"];\n"
      + "    \"VGR1PlacingOnSwap\" -> \"VGR1PlacingOnSwap\" [label=\"CBTokenRequestEventMessage / send ZonesSystem::ZonesMessages::NoTokenOnCBEventMessage -> zone\"];\n"
      + "    \"VGR1PlacingOnSwapAfterFeed\" -> \"FeedThenSwap\" [label=\"TokenPlacedOnCBEventMessage / send ZonesSystem::ZonesMessages::TokenOnCBEventMessage -> zone\"];\n"
      + "    \"VGR1PlacingOnSwapAfterFeed\" -> \"VGR1PlacingOnSwapAfterFeed\" [label=\"CBTokenRequestEventMessage / send ZonesSystem::ZonesMessages::NoTokenOnCBEventMessage -> zone\"];\n"
      + "    \"VGR1PlacingOnFeedAfterSwap\" -> \"SwapThenFeed\" [label=\"TokenPlacedOnCBEventMessage / send ZonesSystem::ZonesMessages::TokenOnCBEventMessage -> zone\"];\n"
      + "    \"VGR1PlacingOnFeedAfterSwap\" -> \"VGR1PlacingOnFeedAfterSwap\" [label=\"CBTokenRequestEventMessage / send ZonesSystem::ZonesMessages::NoTokenOnCBEventMessage -> zone\"];\n"
      + "    \"VGR2PickingLastToken\" -> \"Empty\" [label=\"TokenPickedFromCBEventMessage / send ZonesSystem::ZonesMessages::CBSlotFreeEventMessage -> zone\"];\n"
      + "    \"VGR2PickingLastToken\" -> \"VGR2PickingLastToken\" [label=\"CBSlotRequestEventMessage / send ZonesSystem::ZonesMessages::NoCBSlotFreeEventMessage -> zone\"];\n"
      + "    \"VGR2PickingFeedBeforeSwap\" -> \"TokenOnSwap\" [label=\"TokenPickedFromCBEventMessage / send ZonesSystem::ZonesMessages::CBSlotFreeEventMessage -> zone\"];\n"
      + "    \"VGR2PickingFeedBeforeSwap\" -> \"VGR2PickingFeedBeforeSwap\" [label=\"CBSlotRequestEventMessage / send ZonesSystem::ZonesMessages::NoCBSlotFreeEventMessage -> zone\"];\n"
      + "    \"VGR2PickingSwapBeforeFeed\" -> \"TokenOnFeed\" [label=\"TokenPickedFromCBEventMessage / send ZonesSystem::ZonesMessages::CBSlotFreeEventMessage -> zone\"];\n"
      + "    \"VGR2PickingSwapBeforeFeed\" -> \"VGR2PickingSwapBeforeFeed\" [label=\"CBSlotRequestEventMessage / send ZonesSystem::ZonesMessages::NoCBSlotFreeEventMessage -> zone\"];\n"
      + "}";

  private final ZoneMissionDeadCBActions actions;

  private final Zone zone;

  public ZoneMissionDeadCB(Zone zone, ZoneMissionDeadCBActions actions) {
    this.zone = zone;
    this.actions = actions;

    // States are built from the collected transitions.
    RuntimeState empty = new RuntimeState("Empty");
    RuntimeState tokenOnFeed = new RuntimeState("TokenOnFeed");
    RuntimeState tokenOnSwap = new RuntimeState("TokenOnSwap");
    RuntimeState feedThenSwap = new RuntimeState("FeedThenSwap");
    RuntimeState swapThenFeed = new RuntimeState("SwapThenFeed");
    RuntimeState vGR1PlacingOnSwap = new RuntimeState("VGR1PlacingOnSwap");
    RuntimeState vGR1PlacingOnSwapAfterFeed = new RuntimeState("VGR1PlacingOnSwapAfterFeed");
    RuntimeState vGR1PlacingOnFeedAfterSwap = new RuntimeState("VGR1PlacingOnFeedAfterSwap");
    RuntimeState vGR2PickingLastToken = new RuntimeState("VGR2PickingLastToken");
    RuntimeState vGR2PickingFeedBeforeSwap = new RuntimeState("VGR2PickingFeedBeforeSwap");
    RuntimeState vGR2PickingSwapBeforeFeed = new RuntimeState("VGR2PickingSwapBeforeFeed");

    // Transitions connect triggers, runtime actions, and next-state targets.
    this.runtime.setEntryTransition(new RuntimeTransition(CompletionEvent.class, event -> true, event -> { }, empty));
    empty.addTransition(new RuntimeTransition(CBSlotRequestEventMessage.class, event -> true, event -> this.zone.publish(new PlaceOnSwapEventMessage()), vGR1PlacingOnSwap, this.zone));
    empty.addTransition(new RuntimeTransition(CBTokenRequestEventMessage.class, event -> true, event -> this.zone.publish(new NoTokenOnCBEventMessage()), empty, this.zone));
    tokenOnFeed.addTransition(new RuntimeTransition(CBSlotRequestEventMessage.class, event -> true, event -> this.zone.publish(new PlaceOnSwapEventMessage()), vGR1PlacingOnSwapAfterFeed, this.zone));
    tokenOnFeed.addTransition(new RuntimeTransition(CBTokenRequestEventMessage.class, event -> true, event -> this.zone.publish(new PickFromFeedEventMessage()), vGR2PickingLastToken, this.zone));
    tokenOnSwap.addTransition(new RuntimeTransition(CBSlotRequestEventMessage.class, event -> true, event -> this.zone.publish(new PlaceOnFeedEventMessage()), vGR1PlacingOnFeedAfterSwap, this.zone));
    tokenOnSwap.addTransition(new RuntimeTransition(CBTokenRequestEventMessage.class, event -> true, event -> this.zone.publish(new PickFromSwapEventMessage()), vGR2PickingLastToken, this.zone));
    feedThenSwap.addTransition(new RuntimeTransition(CBSlotRequestEventMessage.class, event -> true, event -> this.zone.publish(new NoCBSlotFreeEventMessage()), feedThenSwap, this.zone));
    feedThenSwap.addTransition(new RuntimeTransition(CBTokenRequestEventMessage.class, event -> true, event -> this.zone.publish(new PickFromFeedEventMessage()), vGR2PickingFeedBeforeSwap, this.zone));
    swapThenFeed.addTransition(new RuntimeTransition(CBSlotRequestEventMessage.class, event -> true, event -> this.zone.publish(new NoCBSlotFreeEventMessage()), swapThenFeed, this.zone));
    swapThenFeed.addTransition(new RuntimeTransition(CBTokenRequestEventMessage.class, event -> true, event -> this.zone.publish(new PickFromSwapEventMessage()), vGR2PickingSwapBeforeFeed, this.zone));
    vGR1PlacingOnSwap.addTransition(new RuntimeTransition(TokenPlacedOnCBEventMessage.class, event -> true, event -> this.zone.publish(new TokenOnCBEventMessage()), tokenOnSwap, this.zone));
    vGR1PlacingOnSwap.addTransition(new RuntimeTransition(CBTokenRequestEventMessage.class, event -> true, event -> this.zone.publish(new NoTokenOnCBEventMessage()), vGR1PlacingOnSwap, this.zone));
    vGR1PlacingOnSwapAfterFeed.addTransition(new RuntimeTransition(TokenPlacedOnCBEventMessage.class, event -> true, event -> this.zone.publish(new TokenOnCBEventMessage()), feedThenSwap, this.zone));
    vGR1PlacingOnSwapAfterFeed.addTransition(new RuntimeTransition(CBTokenRequestEventMessage.class, event -> true, event -> this.zone.publish(new NoTokenOnCBEventMessage()), vGR1PlacingOnSwapAfterFeed, this.zone));
    vGR1PlacingOnFeedAfterSwap.addTransition(new RuntimeTransition(TokenPlacedOnCBEventMessage.class, event -> true, event -> this.zone.publish(new TokenOnCBEventMessage()), swapThenFeed, this.zone));
    vGR1PlacingOnFeedAfterSwap.addTransition(new RuntimeTransition(CBTokenRequestEventMessage.class, event -> true, event -> this.zone.publish(new NoTokenOnCBEventMessage()), vGR1PlacingOnFeedAfterSwap, this.zone));
    vGR2PickingLastToken.addTransition(new RuntimeTransition(TokenPickedFromCBEventMessage.class, event -> true, event -> this.zone.publish(new CBSlotFreeEventMessage()), empty, this.zone));
    vGR2PickingLastToken.addTransition(new RuntimeTransition(CBSlotRequestEventMessage.class, event -> true, event -> this.zone.publish(new NoCBSlotFreeEventMessage()), vGR2PickingLastToken, this.zone));
    vGR2PickingFeedBeforeSwap.addTransition(new RuntimeTransition(TokenPickedFromCBEventMessage.class, event -> true, event -> this.zone.publish(new CBSlotFreeEventMessage()), tokenOnSwap, this.zone));
    vGR2PickingFeedBeforeSwap.addTransition(new RuntimeTransition(CBSlotRequestEventMessage.class, event -> true, event -> this.zone.publish(new NoCBSlotFreeEventMessage()), vGR2PickingFeedBeforeSwap, this.zone));
    vGR2PickingSwapBeforeFeed.addTransition(new RuntimeTransition(TokenPickedFromCBEventMessage.class, event -> true, event -> this.zone.publish(new CBSlotFreeEventMessage()), tokenOnFeed, this.zone));
    vGR2PickingSwapBeforeFeed.addTransition(new RuntimeTransition(CBSlotRequestEventMessage.class, event -> true, event -> this.zone.publish(new NoCBSlotFreeEventMessage()), vGR2PickingSwapBeforeFeed, this.zone));

    // Subscribe each mission machine to trigger event types used by this mission.
    zone.subscribe(CBSlotRequestEventMessage.class, event -> onEvent(zone, event));
    zone.subscribe(CBTokenRequestEventMessage.class, event -> onEvent(zone, event));
    zone.subscribe(TokenPlacedOnCBEventMessage.class, event -> onEvent(zone, event));
    zone.subscribe(TokenPickedFromCBEventMessage.class, event -> onEvent(zone, event));
  }

  @Override
  public String getName() {
    return "ZoneMissionDeadCB";
  }

  @Override
  public List<MachineAdapter> getMachines() {
    return List.<MachineAdapter>of(this.zone);
  }

  @Override
  public String getDescription() {
    return "Zone of the dead conveyor belt: knows which of its two slots (feed, swap) hold a token, and in which order, and\n"
            + "           lets one gripper at a time over the belt";
  }

  public static String toDot() {
    return DOT_SCHEMA;
  }

  @Override
  public String getDotGraph() {
    return DOT_SCHEMA;
  }
}
