package fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.zonemissionmpsnominal;

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
 * From ZoneMissions::ZoneMissionMPSNominal
 * Mission for managing zone acquirement on the Multi Processing Station between VGR2 and the MPS for nominal mission
 */
public class ZoneMissionMPSNominal extends AbstractMissionStrategy {
  private static final Logger logger = LoggerFactory.getLogger("ZoneMissionMPSNominal");

  public static final String DOT_SCHEMA = "digraph ZoneMissionMPSNominal {\n"
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

  private final ZoneMissionMPSNominalActions actions;

  private final Zone zone;

  public ZoneMissionMPSNominal(Zone zone, ZoneMissionMPSNominalActions actions) {
    this.zone = zone;
    this.actions = actions;

    // States are built from the collected transitions.
    RuntimeState idleFree = new RuntimeState("IdleFree");
    RuntimeState idleBusy = new RuntimeState("IdleBusy");
    RuntimeState idleBusyRequested = new RuntimeState("IdleBusyRequested");

    // Transitions connect triggers, runtime actions, and next-state targets.
    this.runtime.setEntryTransition(new RuntimeTransition(CompletionEvent.class, event -> true, event -> { }, idleFree));
    idleFree.addTransition(new RuntimeTransition(AcquireRequestEventMessage.class, event -> true, event -> this.zone.publish(new AcquireResponseEventMessage()), idleBusy));
    idleBusy.addTransition(new RuntimeTransition(ReleaseRequestEventMessage.class, event -> true, event -> { }, idleFree));
    idleBusy.addTransition(new RuntimeTransition(AcquireRequestEventMessage.class, event -> true, event -> { }, idleBusyRequested));
    idleBusyRequested.addTransition(new RuntimeTransition(ReleaseRequestEventMessage.class, event -> true, event -> this.zone.publish(new AcquireResponseEventMessage()), idleBusy));

    // Subscribe each mission machine to trigger event types used by this mission.
    zone.subscribe(AcquireRequestEventMessage.class, this::onEvent);
    zone.subscribe(ReleaseRequestEventMessage.class, this::onEvent);
  }

  @Override
  public String getName() {
    return "ZoneMissionMPSNominal";
  }

  @Override
  public List<MachineAdapter> getMachines() {
    return List.<MachineAdapter>of(this.zone);
  }

  @Override
  public String getDescription() {
    return "Mission for managing zone acquirement on the Multi Processing Station between VGR2 and the MPS for nominal mission";
  }

  public static String toDot() {
    return DOT_SCHEMA;
  }

  @Override
  public String getDotGraph() {
    return DOT_SCHEMA;
  }
}
