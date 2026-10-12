package fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippercbfailuremissions.vacuumgripper1cbfailuremission;

import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinesystem.sortingline.SortingLineMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinesystem.sortinglinemessages.BlueTokenAvailableEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinesystem.sortinglinemessages.RedTokenAvailableEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinesystem.sortinglinemessages.WhiteTokenAvailableEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgripper.VacuumGripperMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgrippermessages.VGRCommandSuccessEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.Zone;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.CBSlotFreeEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.NoCBSlotFreeEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.PlaceOnFeedEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.PlaceOnSwapEventMessage;
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
 * From VacuumGripperCBFailureMissions::VacuumGripper1CBFailureMission
 * VGR1 when the conveyor belt fails: takes the tokens at the sorting line outputs to the slot of the belt the dead CB zone gives
 */
public class VacuumGripper1CBFailureMission extends AbstractMissionStrategy {
  private static final Logger logger = LoggerFactory.getLogger("VacuumGripper1CBFailureMission");

  public static final String DOT_SCHEMA = "digraph VacuumGripper1CBFailureMission {\n"
      + "    fontname=\"Helvetica,Arial,sans-serif\"\n"
      + "    node [fontname=\"Helvetica,Arial,sans-serif\"]\n"
      + "    edge [fontname=\"Helvetica,Arial,sans-serif\"]\n"
      + "    rankdir=LR;\n"
      + "    node [shape=point, label=\"\"]; __init__;\n"
      + "    node [shape=circle, style=\"\", fillcolor=\"\"];\n"
      + "    \"SetupCMD\";\n"
      + "    \"StandbyCMD\";\n"
      + "    \"AskSortingLine\";\n"
      + "    \"Idle\";\n"
      + "    \"PickColorCMD\";\n"
      + "    \"AskCBSlot\";\n"
      + "    \"SelectCBSlot\";\n"
      + "    \"WaitForFreeCBSlot\";\n"
      + "    \"PlaceConveyorBeltCMD\";\n"
      + "    \"RetractFromCBCMD\";\n"
      + "    \"CBZoneReleased\";\n"
      + "    \"ReturnToStandbyCMD\";\n"
      + "\n"
      + "    __init__ -> \"SetupCMD\" [label=\"ε / vacuumGripper.setup()\"];\n"
      + "    \"SetupCMD\" -> \"StandbyCMD\" [label=\"VGRCommandSuccessEventMessage / gotoStandby\"];\n"
      + "    \"StandbyCMD\" -> \"AskSortingLine\" [label=\"VGRCommandSuccessEventMessage\"];\n"
      + "    \"AskSortingLine\" -> \"Idle\" [label=\"ε / askSortingLine\"];\n"
      + "    \"Idle\" -> \"PickColorCMD\" [label=\"BlueTokenAvailableEventMessage / pickBlue\"];\n"
      + "    \"Idle\" -> \"PickColorCMD\" [label=\"WhiteTokenAvailableEventMessage / pickWhite\"];\n"
      + "    \"Idle\" -> \"PickColorCMD\" [label=\"RedTokenAvailableEventMessage / pickRed\"];\n"
      + "    \"PickColorCMD\" -> \"AskCBSlot\" [label=\"VGRCommandSuccessEventMessage\"];\n"
      + "    \"AskCBSlot\" -> \"SelectCBSlot\" [label=\"ε / askCBSlot\"];\n"
      + "    \"SelectCBSlot\" -> \"PlaceConveyorBeltCMD\" [label=\"PlaceOnFeedEventMessage / placeConveyorBeltFeed\"];\n"
      + "    \"SelectCBSlot\" -> \"PlaceConveyorBeltCMD\" [label=\"PlaceOnSwapEventMessage / placeConveyorBeltSwap\"];\n"
      + "    \"SelectCBSlot\" -> \"WaitForFreeCBSlot\" [label=\"NoCBSlotFreeEventMessage\"];\n"
      + "    \"WaitForFreeCBSlot\" -> \"AskCBSlot\" [label=\"CBSlotFreeEventMessage\"];\n"
      + "    \"PlaceConveyorBeltCMD\" -> \"RetractFromCBCMD\" [label=\"VGRCommandSuccessEventMessage / vacuumGripper.retractArm()\"];\n"
      + "    \"RetractFromCBCMD\" -> \"CBZoneReleased\" [label=\"VGRCommandSuccessEventMessage / send ZonesSystem::ZonesMessages::TokenPlacedOnCBEventMessage -> zoneCB\"];\n"
      + "    \"CBZoneReleased\" -> \"ReturnToStandbyCMD\" [label=\"ε / gotoStandby\"];\n"
      + "    \"ReturnToStandbyCMD\" -> \"AskSortingLine\" [label=\"VGRCommandSuccessEventMessage\"];\n"
      + "}";

  private final VacuumGripper1CBFailureMissionActions actions;

  private final VacuumGripperMachine vacuumGripper;

  private final SortingLineMachine sortingLine;

  private final Zone zoneCB;

  public VacuumGripper1CBFailureMission(VacuumGripperMachine vacuumGripper,
      SortingLineMachine sortingLine, Zone zoneCB, VacuumGripper1CBFailureMissionActions actions) {
    this.vacuumGripper = vacuumGripper;
    this.sortingLine = sortingLine;
    this.zoneCB = zoneCB;
    this.actions = actions;

    // States are built from the collected transitions.
    RuntimeState setupCMD = new RuntimeState("SetupCMD");
    RuntimeState standbyCMD = new RuntimeState("StandbyCMD");
    RuntimeState askSortingLine = new RuntimeState("AskSortingLine");
    RuntimeState idle = new RuntimeState("Idle");
    RuntimeState pickColorCMD = new RuntimeState("PickColorCMD");
    RuntimeState askCBSlot = new RuntimeState("AskCBSlot");
    RuntimeState selectCBSlot = new RuntimeState("SelectCBSlot");
    RuntimeState waitForFreeCBSlot = new RuntimeState("WaitForFreeCBSlot");
    RuntimeState placeConveyorBeltCMD = new RuntimeState("PlaceConveyorBeltCMD");
    RuntimeState retractFromCBCMD = new RuntimeState("RetractFromCBCMD");
    RuntimeState cBZoneReleased = new RuntimeState("CBZoneReleased");
    RuntimeState returnToStandbyCMD = new RuntimeState("ReturnToStandbyCMD");

    // Transitions connect triggers, runtime actions, and next-state targets.
    this.runtime.setEntryTransition(new RuntimeTransition(CompletionEvent.class, event -> true, event -> this.vacuumGripper.setup(), setupCMD));
    setupCMD.addTransition(new RuntimeTransition(VGRCommandSuccessEventMessage.class, event -> true, event -> this.actions.gotoStandby(event, this.vacuumGripper, this.sortingLine, this.zoneCB), standbyCMD, this.vacuumGripper));
    standbyCMD.addTransition(new RuntimeTransition(VGRCommandSuccessEventMessage.class, event -> true, event -> { }, askSortingLine, this.vacuumGripper));
    askSortingLine.addTransition(new RuntimeTransition(CompletionEvent.class, event -> true, event -> this.actions.askSortingLine(event, this.vacuumGripper, this.sortingLine, this.zoneCB), idle));
    idle.addTransition(new RuntimeTransition(BlueTokenAvailableEventMessage.class, event -> true, event -> this.actions.pickBlue(event, this.vacuumGripper, this.sortingLine, this.zoneCB), pickColorCMD));
    idle.addTransition(new RuntimeTransition(WhiteTokenAvailableEventMessage.class, event -> true, event -> this.actions.pickWhite(event, this.vacuumGripper, this.sortingLine, this.zoneCB), pickColorCMD));
    idle.addTransition(new RuntimeTransition(RedTokenAvailableEventMessage.class, event -> true, event -> this.actions.pickRed(event, this.vacuumGripper, this.sortingLine, this.zoneCB), pickColorCMD));
    pickColorCMD.addTransition(new RuntimeTransition(VGRCommandSuccessEventMessage.class, event -> true, event -> { }, askCBSlot, this.vacuumGripper));
    askCBSlot.addTransition(new RuntimeTransition(CompletionEvent.class, event -> true, event -> this.actions.askCBSlot(event, this.vacuumGripper, this.sortingLine, this.zoneCB), selectCBSlot));
    selectCBSlot.addTransition(new RuntimeTransition(PlaceOnFeedEventMessage.class, event -> true, event -> this.actions.placeConveyorBeltFeed(event, this.vacuumGripper, this.sortingLine, this.zoneCB), placeConveyorBeltCMD, this.zoneCB));
    selectCBSlot.addTransition(new RuntimeTransition(PlaceOnSwapEventMessage.class, event -> true, event -> this.actions.placeConveyorBeltSwap(event, this.vacuumGripper, this.sortingLine, this.zoneCB), placeConveyorBeltCMD, this.zoneCB));
    selectCBSlot.addTransition(new RuntimeTransition(NoCBSlotFreeEventMessage.class, event -> true, event -> { }, waitForFreeCBSlot, this.zoneCB));
    waitForFreeCBSlot.addTransition(new RuntimeTransition(CBSlotFreeEventMessage.class, event -> true, event -> { }, askCBSlot, this.zoneCB));
    placeConveyorBeltCMD.addTransition(new RuntimeTransition(VGRCommandSuccessEventMessage.class, event -> true, event -> this.vacuumGripper.retractArm(), retractFromCBCMD, this.vacuumGripper));
    retractFromCBCMD.addTransition(new RuntimeTransition(VGRCommandSuccessEventMessage.class, event -> true, event -> this.zoneCB.publish(new TokenPlacedOnCBEventMessage()), cBZoneReleased, this.vacuumGripper));
    cBZoneReleased.addTransition(new RuntimeTransition(CompletionEvent.class, event -> true, event -> this.actions.gotoStandby(event, this.vacuumGripper, this.sortingLine, this.zoneCB), returnToStandbyCMD));
    returnToStandbyCMD.addTransition(new RuntimeTransition(VGRCommandSuccessEventMessage.class, event -> true, event -> { }, askSortingLine, this.vacuumGripper));

    // Subscribe each mission machine to trigger event types used by this mission.
    vacuumGripper.subscribe(VGRCommandSuccessEventMessage.class, event -> onEvent(vacuumGripper, event));
    sortingLine.subscribe(BlueTokenAvailableEventMessage.class, event -> onEvent(sortingLine, event));
    sortingLine.subscribe(WhiteTokenAvailableEventMessage.class, event -> onEvent(sortingLine, event));
    sortingLine.subscribe(RedTokenAvailableEventMessage.class, event -> onEvent(sortingLine, event));
    zoneCB.subscribe(PlaceOnFeedEventMessage.class, event -> onEvent(zoneCB, event));
    zoneCB.subscribe(PlaceOnSwapEventMessage.class, event -> onEvent(zoneCB, event));
    zoneCB.subscribe(NoCBSlotFreeEventMessage.class, event -> onEvent(zoneCB, event));
    zoneCB.subscribe(CBSlotFreeEventMessage.class, event -> onEvent(zoneCB, event));
  }

  @Override
  public String getName() {
    return "VacuumGripper1CBFailureMission";
  }

  @Override
  public List<MachineAdapter> getMachines() {
    return List.<MachineAdapter>of(this.vacuumGripper, this.sortingLine, this.zoneCB);
  }

  @Override
  public String getDescription() {
    return "VGR1 when the conveyor belt fails: takes the tokens at the sorting line outputs to the slot of the belt the dead CB zone gives";
  }

  public static String toDot() {
    return DOT_SCHEMA;
  }

  @Override
  public String getDotGraph() {
    return DOT_SCHEMA;
  }
}
