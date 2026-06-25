package fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippermissions.vacuumgripper1nominalmission;

import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinesystem.sortingline.SortingLineMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinesystem.sortinglinemessages.BlueTokenAvailableEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinesystem.sortinglinemessages.RedTokenAvailableEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinesystem.sortinglinemessages.WhiteTokenAvailableEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgripper.VacuumGripperMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgrippermessages.VGRCommandSuccessEventMessage;
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
 * From VacuumGripperMissions::VacuumGripper1NominalMission
 * Secondary mission scenario for VacuumGripper n°1
 */
public class VacuumGripper1NominalMission extends AbstractMissionStrategy {
  private static final Logger logger = LoggerFactory.getLogger("VacuumGripper1NominalMission");

  public static final String DOT_SCHEMA = "digraph VacuumGripper1NominalMission {\n"
      + "    fontname=\"Helvetica,Arial,sans-serif\"\n"
      + "    node [fontname=\"Helvetica,Arial,sans-serif\"]\n"
      + "    edge [fontname=\"Helvetica,Arial,sans-serif\"]\n"
      + "    rankdir=LR;\n"
      + "    node [shape=point, label=\"\"]; __init__;\n"
      + "    node [shape=circle, style=\"\", fillcolor=\"\"];\n"
      + "    \"SetupCMD\";\n"
      + "    \"StandbyCMD\";\n"
      + "    \"Idle\";\n"
      + "    \"PickColorCMD\";\n"
      + "    \"IdlePicked\";\n"
      + "    \"PlaceConveyorBeltFeed\";\n"
      + "\n"
      + "    __init__ -> \"SetupCMD\" [label=\"ε / vacuumGripper.setup()\"];\n"
      + "    \"SetupCMD\" -> \"StandbyCMD\" [label=\"VGRCommandSuccessEventMessage / gotoStandby\"];\n"
      + "    \"StandbyCMD\" -> \"Idle\" [label=\"VGRCommandSuccessEventMessage / send ZonesSystem::ZonesMessages::ReleaseRequestEventMessage -> zoneCB\"];\n"
      + "    \"Idle\" -> \"PickColorCMD\" [label=\"BlueTokenAvailableEventMessage / pickBlue\"];\n"
      + "    \"Idle\" -> \"PickColorCMD\" [label=\"WhiteTokenAvailableEventMessage / pickWhite\"];\n"
      + "    \"Idle\" -> \"PickColorCMD\" [label=\"RedTokenAvailableEventMessage / pickRed\"];\n"
      + "    \"PickColorCMD\" -> \"IdlePicked\" [label=\"VGRCommandSuccessEventMessage / send ZonesSystem::ZonesMessages::AcquireRequestEventMessage -> zoneCB\"];\n"
      + "    \"IdlePicked\" -> \"PlaceConveyorBeltFeed\" [label=\"AcquireResponseEventMessage / placeConveyoBeltFeed\"];\n"
      + "    \"PlaceConveyorBeltFeed\" -> \"StandbyCMD\" [label=\"VGRCommandSuccessEventMessage / gotoStandby\"];\n"
      + "}";

  private final VacuumGripper1NominalMissionActions actions;

  private final VacuumGripperMachine vacuumGripper;

  private final SortingLineMachine sortingLine;

  private final Zone zoneCB;

  public VacuumGripper1NominalMission(VacuumGripperMachine vacuumGripper,
      SortingLineMachine sortingLine, Zone zoneCB, VacuumGripper1NominalMissionActions actions) {
    this.vacuumGripper = vacuumGripper;
    this.sortingLine = sortingLine;
    this.zoneCB = zoneCB;
    this.actions = actions;

    // States are built from the collected transitions.
    RuntimeState setupCMD = new RuntimeState("SetupCMD");
    RuntimeState standbyCMD = new RuntimeState("StandbyCMD");
    RuntimeState idle = new RuntimeState("Idle");
    RuntimeState pickColorCMD = new RuntimeState("PickColorCMD");
    RuntimeState idlePicked = new RuntimeState("IdlePicked");
    RuntimeState placeConveyorBeltFeed = new RuntimeState("PlaceConveyorBeltFeed");

    // Transitions connect triggers, runtime actions, and next-state targets.
    this.runtime.setEntryTransition(new RuntimeTransition(CompletionEvent.class, event -> true, event -> this.vacuumGripper.setup(), setupCMD));
    setupCMD.addTransition(new RuntimeTransition(VGRCommandSuccessEventMessage.class, event -> true, event -> this.actions.gotoStandby(event, this.vacuumGripper, this.sortingLine, this.zoneCB), standbyCMD));
    standbyCMD.addTransition(new RuntimeTransition(VGRCommandSuccessEventMessage.class, event -> true, event -> this.zoneCB.publish(new ReleaseRequestEventMessage()), idle));
    idle.addTransition(new RuntimeTransition(BlueTokenAvailableEventMessage.class, event -> true, event -> this.actions.pickBlue(event, this.vacuumGripper, this.sortingLine, this.zoneCB), pickColorCMD));
    idle.addTransition(new RuntimeTransition(WhiteTokenAvailableEventMessage.class, event -> true, event -> this.actions.pickWhite(event, this.vacuumGripper, this.sortingLine, this.zoneCB), pickColorCMD));
    idle.addTransition(new RuntimeTransition(RedTokenAvailableEventMessage.class, event -> true, event -> this.actions.pickRed(event, this.vacuumGripper, this.sortingLine, this.zoneCB), pickColorCMD));
    pickColorCMD.addTransition(new RuntimeTransition(VGRCommandSuccessEventMessage.class, event -> true, event -> this.zoneCB.publish(new AcquireRequestEventMessage()), idlePicked));
    idlePicked.addTransition(new RuntimeTransition(AcquireResponseEventMessage.class, event -> true, event -> this.actions.placeConveyoBeltFeed(event, this.vacuumGripper, this.sortingLine, this.zoneCB), placeConveyorBeltFeed));
    placeConveyorBeltFeed.addTransition(new RuntimeTransition(VGRCommandSuccessEventMessage.class, event -> true, event -> this.actions.gotoStandby(event, this.vacuumGripper, this.sortingLine, this.zoneCB), standbyCMD));

    // Subscribe each mission machine to trigger event types used by this mission.
    vacuumGripper.subscribe(VGRCommandSuccessEventMessage.class, this::onEvent);
    sortingLine.subscribe(BlueTokenAvailableEventMessage.class, this::onEvent);
    sortingLine.subscribe(WhiteTokenAvailableEventMessage.class, this::onEvent);
    sortingLine.subscribe(RedTokenAvailableEventMessage.class, this::onEvent);
    zoneCB.subscribe(AcquireResponseEventMessage.class, this::onEvent);
  }

  @Override
  public String getName() {
    return "VacuumGripper1NominalMission";
  }

  @Override
  public List<MachineAdapter> getMachines() {
    return List.<MachineAdapter>of(this.vacuumGripper, this.sortingLine, this.zoneCB);
  }

  @Override
  public String getDescription() {
    return "Secondary mission scenario for VacuumGripper n°1";
  }

  public static String toDot() {
    return DOT_SCHEMA;
  }

  @Override
  public String getDotGraph() {
    return DOT_SCHEMA;
  }
}
