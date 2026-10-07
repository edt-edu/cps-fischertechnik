package fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.zonemissioncbnominal;

import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.Zone;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.AcquireRequestEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.AcquireResponseEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.ReleaseRequestEventMessage;
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
 * Mission for managing zone acquirement on Conveyor Belt between VGR1 and VGR2 for nominal mission
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
      + "    \"IdleFree\";\n"
      + "    \"IdleBusy\";\n"
      + "    \"IdleBusyRequested\";\n"
      + "\n"
      + "    __init__ -> \"IdleFree\" [label=\"ε\"];\n"
      + "    \"IdleFree\" -> \"IdleBusy\" [label=\"AcquireRequestEventMessage / send ZonesSystem::ZonesMessages::AcquireResponseEventMessage -> zone\"];\n"
      + "    \"IdleBusy\" -> \"IdleFree\" [label=\"ReleaseRequestEventMessage\"];\n"
      + "    \"IdleBusy\" -> \"IdleBusyRequested\" [label=\"AcquireRequestEventMessage\"];\n"
      + "    \"IdleBusyRequested\" -> \"IdleBusy\" [label=\"ReleaseRequestEventMessage / send ZonesSystem::ZonesMessages::AcquireResponseEventMessage -> zone\"];\n"
      + "}";

  private final ZoneMissionCBNominalActions actions;

  private final Zone zone;

  public ZoneMissionCBNominal(Zone zone, ZoneMissionCBNominalActions actions) {
    this.zone = zone;
    this.actions = actions;

    // States are built from the collected transitions.
    RuntimeState idleFree = new RuntimeState("IdleFree");
    RuntimeState idleBusy = new RuntimeState("IdleBusy");
    RuntimeState idleBusyRequested = new RuntimeState("IdleBusyRequested");

    // Transitions connect triggers, runtime actions, and next-state targets.
    this.runtime.setEntryTransition(new RuntimeTransition(CompletionEvent.class, event -> true, event -> { }, idleFree));
    idleFree.addTransition(new RuntimeTransition(AcquireRequestEventMessage.class, event -> true, event -> this.zone.publish(new AcquireResponseEventMessage()), idleBusy, this.zone));
    idleBusy.addTransition(new RuntimeTransition(ReleaseRequestEventMessage.class, event -> true, event -> { }, idleFree, this.zone));
    idleBusy.addTransition(new RuntimeTransition(AcquireRequestEventMessage.class, event -> true, event -> { }, idleBusyRequested, this.zone));
    idleBusyRequested.addTransition(new RuntimeTransition(ReleaseRequestEventMessage.class, event -> true, event -> this.zone.publish(new AcquireResponseEventMessage()), idleBusy, this.zone));

    // Subscribe each mission machine to trigger event types used by this mission.
    zone.subscribe(AcquireRequestEventMessage.class, event -> onEvent(zone, event));
    zone.subscribe(ReleaseRequestEventMessage.class, event -> onEvent(zone, event));
  }

  @Override
  public String getName() {
    return "ZoneMissionCBNominal";
  }

  @Override
  public List<MachineAdapter> getMachines() {
    return List.<MachineAdapter>of(this.zone);
  }

  @Override
  public String getDescription() {
    return "Mission for managing zone acquirement on Conveyor Belt between VGR1 and VGR2 for nominal mission";
  }

  public static String toDot() {
    return DOT_SCHEMA;
  }

  @Override
  public String getDotGraph() {
    return DOT_SCHEMA;
  }
}
