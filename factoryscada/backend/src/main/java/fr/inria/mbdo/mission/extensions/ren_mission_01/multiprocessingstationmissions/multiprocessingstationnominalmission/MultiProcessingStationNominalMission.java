package fr.inria.mbdo.mission.extensions.ren_mission_01.multiprocessingstationmissions.multiprocessingstationnominalmission;

import fr.inria.mbdo.mission.extensions.ren_mission_01.multiprocessingstationmissions.multiprocessingstationnominalmission.customevents.AcceptWhenMultiProcessingStationSensorMPSinEqualstrueAndMultiProcessingStationSensorMPSoutEqualsfalseEvent;
import fr.inria.mbdo.mission.extensions.ren_mission_01.multiprocessingstationsystem.multiprocessingstation.MultiProcessingStationMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.multiprocessingstationsystem.multiprocessingstationmessages.MPSCommandSuccessEventMessage;
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
 * From MultiProcessingStationMissions::MultiProcessingStationNominalMission
 * Nominal mission for the multi-processing station
 */
public class MultiProcessingStationNominalMission extends AbstractMissionStrategy {
  private static final Logger logger = LoggerFactory.getLogger("MultiProcessingStationNominalMission");

  public static final String DOT_SCHEMA = "digraph MultiProcessingStationNominalMission {\n"
      + "    fontname=\"Helvetica,Arial,sans-serif\"\n"
      + "    node [fontname=\"Helvetica,Arial,sans-serif\"]\n"
      + "    edge [fontname=\"Helvetica,Arial,sans-serif\"]\n"
      + "    rankdir=LR;\n"
      + "    node [shape=point, label=\"\"]; __init__;\n"
      + "    node [shape=circle, style=\"\", fillcolor=\"\"];\n"
      + "    \"Idle\";\n"
      + "    \"WaitForZone\";\n"
      + "    \"ProcessCMD\";\n"
      + "\n"
      + "    __init__ -> \"Idle\" [label=\"ε / multiProcessingStation.setup()\"];\n"
      + "    \"Idle\" -> \"WaitForZone\" [label=\"when(multiProcessingStation.sensor_MPS_in == true and multiProcessingStation.sensor_MPS_out == false) / send ZonesSystem::ZonesMessages::AcquireRequestEventMessage -> zoneMPS\"];\n"
      + "    \"WaitForZone\" -> \"ProcessCMD\" [label=\"AcquireResponseEventMessage / processPayload\"];\n"
      + "    \"ProcessCMD\" -> \"Idle\" [label=\"MPSCommandSuccessEventMessage / send ZonesSystem::ZonesMessages::ReleaseRequestEventMessage -> zoneMPS\"];\n"
      + "}";

  private final MultiProcessingStationNominalMissionActions actions;

  private final MultiProcessingStationMachine multiProcessingStation;

  private final Zone zoneMPS;

  public MultiProcessingStationNominalMission(MultiProcessingStationMachine multiProcessingStation,
      Zone zoneMPS, MultiProcessingStationNominalMissionActions actions) {
    this.multiProcessingStation = multiProcessingStation;
    this.zoneMPS = zoneMPS;
    this.actions = actions;

    // States are built from the collected transitions.
    RuntimeState idle = new RuntimeState("Idle");
    RuntimeState waitForZone = new RuntimeState("WaitForZone");
    RuntimeState processCMD = new RuntimeState("ProcessCMD");

    // Transitions connect triggers, runtime actions, and next-state targets.
    this.runtime.setEntryTransition(new RuntimeTransition(CompletionEvent.class, event -> true, event -> this.multiProcessingStation.setup(), idle));
    idle.addTransition(new RuntimeTransition(AcceptWhenMultiProcessingStationSensorMPSinEqualstrueAndMultiProcessingStationSensorMPSoutEqualsfalseEvent.class, event -> true, event -> this.zoneMPS.publish(new AcquireRequestEventMessage()), waitForZone));
    waitForZone.addTransition(new RuntimeTransition(AcquireResponseEventMessage.class, event -> true, event -> this.actions.processPayload(event, this.multiProcessingStation, this.zoneMPS), processCMD, this.zoneMPS));
    processCMD.addTransition(new RuntimeTransition(MPSCommandSuccessEventMessage.class, event -> true, event -> this.zoneMPS.publish(new ReleaseRequestEventMessage()), idle, this.multiProcessingStation));

    // Subscribe each mission machine to trigger event types used by this mission.
    multiProcessingStation.subscribe(AcceptWhenMultiProcessingStationSensorMPSinEqualstrueAndMultiProcessingStationSensorMPSoutEqualsfalseEvent.class, event -> onEvent(multiProcessingStation, event));
    multiProcessingStation.subscribe(MPSCommandSuccessEventMessage.class, event -> onEvent(multiProcessingStation, event));
    zoneMPS.subscribe(AcquireResponseEventMessage.class, event -> onEvent(zoneMPS, event));
  }

  @Override
  public String getName() {
    return "MultiProcessingStationNominalMission";
  }

  @Override
  public List<MachineAdapter> getMachines() {
    return List.<MachineAdapter>of(this.multiProcessingStation, this.zoneMPS);
  }

  @Override
  public String getDescription() {
    return "Nominal mission for the multi-processing station";
  }

  public static String toDot() {
    return DOT_SCHEMA;
  }

  @Override
  public String getDotGraph() {
    return DOT_SCHEMA;
  }
}
