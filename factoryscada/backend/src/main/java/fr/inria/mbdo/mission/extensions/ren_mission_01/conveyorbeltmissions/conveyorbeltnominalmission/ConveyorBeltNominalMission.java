package fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltmissions.conveyorbeltnominalmission;

import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltmissions.conveyorbeltnominalmission.customevents.AcceptWhenConveyorBeltConveyorSensFeedEqualsfalseAndConveyorBeltConveyorSensSwapEqualstrueEvent;
import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltsystem.conveyorbelt.ConveyorBeltMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltsystem.conveyorbeltmessages.CBCommandSuccessEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltsystem.conveyorbeltmessages.TokenAtFeedEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgripper.VacuumGripperMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.Zone;
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
 * From ConveyorBeltMissions::ConveyorBeltNominalMission
 * Nominal mission for the conveyor belt
 */
public class ConveyorBeltNominalMission extends AbstractMissionStrategy {
  private static final Logger logger = LoggerFactory.getLogger("ConveyorBeltNominalMission");

  public static final String DOT_SCHEMA = "digraph ConveyorBeltNominalMission {\n"
      + "    fontname=\"Helvetica,Arial,sans-serif\"\n"
      + "    node [fontname=\"Helvetica,Arial,sans-serif\"]\n"
      + "    edge [fontname=\"Helvetica,Arial,sans-serif\"]\n"
      + "    rankdir=LR;\n"
      + "    node [shape=point, label=\"\"]; __init__;\n"
      + "    node [shape=circle, style=\"\", fillcolor=\"\"];\n"
      + "    \"CheckBelt\";\n"
      + "    \"Idle\";\n"
      + "    \"MovingToSensor\";\n"
      + "\n"
      + "    __init__ -> \"CheckBelt\" [label=\"ε\"];\n"
      + "    \"CheckBelt\" -> \"Idle\" [label=\"ε / checkBelt\"];\n"
      + "    \"Idle\" -> \"MovingToSensor\" [label=\"when(conveyorBelt.conveyorSensFeed == false and conveyorBelt.conveyorSensSwap == true) / conveyorBelt.moveToSensor()\"];\n"
      + "    \"Idle\" -> \"MovingToSensor\" [label=\"TokenAtFeedEventMessage / conveyorBelt.moveToSensor()\"];\n"
      + "    \"MovingToSensor\" -> \"Idle\" [label=\"CBCommandSuccessEventMessage / notifyTokenAtSwap\"];\n"
      + "}";

  private final ConveyorBeltNominalMissionActions actions;

  private final ConveyorBeltMachine conveyorBelt;

  private final VacuumGripperMachine vacuumGripper1;

  private final VacuumGripperMachine vacuumGripper2;

  private final Zone zoneCB;

  public ConveyorBeltNominalMission(ConveyorBeltMachine conveyorBelt,
      VacuumGripperMachine vacuumGripper1, VacuumGripperMachine vacuumGripper2, Zone zoneCB,
      ConveyorBeltNominalMissionActions actions) {
    this.conveyorBelt = conveyorBelt;
    this.vacuumGripper1 = vacuumGripper1;
    this.vacuumGripper2 = vacuumGripper2;
    this.zoneCB = zoneCB;
    this.actions = actions;

    // States are built from the collected transitions.
    RuntimeState checkBelt = new RuntimeState("CheckBelt");
    RuntimeState idle = new RuntimeState("Idle");
    RuntimeState movingToSensor = new RuntimeState("MovingToSensor");

    // Transitions connect triggers, runtime actions, and next-state targets.
    this.runtime.setEntryTransition(new RuntimeTransition(CompletionEvent.class, event -> true, event -> { }, checkBelt));
    checkBelt.addTransition(new RuntimeTransition(CompletionEvent.class, event -> true, event -> this.actions.checkBelt(event, this.conveyorBelt, this.vacuumGripper1, this.vacuumGripper2, this.zoneCB), idle));
    idle.addTransition(new RuntimeTransition(AcceptWhenConveyorBeltConveyorSensFeedEqualsfalseAndConveyorBeltConveyorSensSwapEqualstrueEvent.class, event -> true, event -> this.conveyorBelt.moveToSensor(), movingToSensor));
    idle.addTransition(new RuntimeTransition(TokenAtFeedEventMessage.class, event -> true, event -> this.conveyorBelt.moveToSensor(), movingToSensor, this.conveyorBelt));
    movingToSensor.addTransition(new RuntimeTransition(CBCommandSuccessEventMessage.class, event -> true, event -> this.actions.notifyTokenAtSwap(event, this.conveyorBelt, this.vacuumGripper1, this.vacuumGripper2, this.zoneCB), idle));

    // Subscribe each mission machine to trigger event types used by this mission.
    conveyorBelt.subscribe(AcceptWhenConveyorBeltConveyorSensFeedEqualsfalseAndConveyorBeltConveyorSensSwapEqualstrueEvent.class, event -> onEvent(conveyorBelt, event));
    conveyorBelt.subscribe(TokenAtFeedEventMessage.class, event -> onEvent(conveyorBelt, event));
    conveyorBelt.subscribe(CBCommandSuccessEventMessage.class, event -> onEvent(conveyorBelt, event));
  }

  @Override
  public String getName() {
    return "ConveyorBeltNominalMission";
  }

  @Override
  public List<MachineAdapter> getMachines() {
    return List.<MachineAdapter>of(this.conveyorBelt, this.vacuumGripper1, this.vacuumGripper2, this.zoneCB);
  }

  @Override
  public String getDescription() {
    return "Nominal mission for the conveyor belt";
  }

  public static String toDot() {
    return DOT_SCHEMA;
  }

  @Override
  public String getDotGraph() {
    return DOT_SCHEMA;
  }
}
