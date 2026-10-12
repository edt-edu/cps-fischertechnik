package fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippercbfailuremissions.vacuumgripper2cbfailuremission;

import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgripper.VacuumGripperMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgrippermessages.VGRCommandSuccessEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.Zone;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.AcquireResponseEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.NoTokenOnCBEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.PickFromFeedEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.PickFromSwapEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.ReleaseRequestEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.TokenOnCBEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.TokenPickedFromCBEventMessage;
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
 * From VacuumGripperCBFailureMissions::VacuumGripper2CBFailureMission
 * VGR2 when the conveyor belt fails: takes the tokens from the slot of the belt the dead CB zone gives, oldest first, to the MPS
 */
public class VacuumGripper2CBFailureMission extends AbstractMissionStrategy {
  private static final Logger logger = LoggerFactory.getLogger("VacuumGripper2CBFailureMission");

  public static final String DOT_SCHEMA = "digraph VacuumGripper2CBFailureMission {\n"
      + "    fontname=\"Helvetica,Arial,sans-serif\"\n"
      + "    node [fontname=\"Helvetica,Arial,sans-serif\"]\n"
      + "    edge [fontname=\"Helvetica,Arial,sans-serif\"]\n"
      + "    rankdir=LR;\n"
      + "    node [shape=point, label=\"\"]; __init__;\n"
      + "    node [shape=circle, style=\"\", fillcolor=\"\"];\n"
      + "    \"SetupCMD\";\n"
      + "    \"StandbyCMD\";\n"
      + "    \"CheckCB\";\n"
      + "    \"SelectCBSlot\";\n"
      + "    \"Idle\";\n"
      + "    \"PickCBCMD\";\n"
      + "    \"RetractFromCBCMD\";\n"
      + "    \"CBZoneReleased\";\n"
      + "    \"WaitForMPSZoneAcquisition\";\n"
      + "    \"PlaceMPSinCMD\";\n"
      + "    \"RetractFromMPSCMD\";\n"
      + "    \"MPSZoneReleased\";\n"
      + "    \"ReturnToStandbyCMD\";\n"
      + "\n"
      + "    __init__ -> \"SetupCMD\" [label=\"ε / vacuumGripper.setup()\"];\n"
      + "    \"SetupCMD\" -> \"StandbyCMD\" [label=\"VGRCommandSuccessEventMessage / gotoStandby\"];\n"
      + "    \"StandbyCMD\" -> \"CheckCB\" [label=\"VGRCommandSuccessEventMessage\"];\n"
      + "    \"CheckCB\" -> \"SelectCBSlot\" [label=\"ε / askCBForToken\"];\n"
      + "    \"SelectCBSlot\" -> \"PickCBCMD\" [label=\"PickFromFeedEventMessage / pickCBfeed\"];\n"
      + "    \"SelectCBSlot\" -> \"PickCBCMD\" [label=\"PickFromSwapEventMessage / pickCBswap\"];\n"
      + "    \"SelectCBSlot\" -> \"Idle\" [label=\"NoTokenOnCBEventMessage\"];\n"
      + "    \"Idle\" -> \"CheckCB\" [label=\"TokenOnCBEventMessage\"];\n"
      + "    \"PickCBCMD\" -> \"RetractFromCBCMD\" [label=\"VGRCommandSuccessEventMessage / vacuumGripper.retractArm()\"];\n"
      + "    \"RetractFromCBCMD\" -> \"CBZoneReleased\" [label=\"VGRCommandSuccessEventMessage / send ZonesSystem::ZonesMessages::TokenPickedFromCBEventMessage -> zoneCB\"];\n"
      + "    \"CBZoneReleased\" -> \"WaitForMPSZoneAcquisition\" [label=\"ε / requestMPSZone\"];\n"
      + "    \"WaitForMPSZoneAcquisition\" -> \"PlaceMPSinCMD\" [label=\"AcquireResponseEventMessage / placeMPSin\"];\n"
      + "    \"PlaceMPSinCMD\" -> \"RetractFromMPSCMD\" [label=\"VGRCommandSuccessEventMessage / vacuumGripper.retractArm()\"];\n"
      + "    \"RetractFromMPSCMD\" -> \"MPSZoneReleased\" [label=\"VGRCommandSuccessEventMessage / send ZonesSystem::ZonesMessages::ReleaseRequestEventMessage -> zoneMPS\"];\n"
      + "    \"MPSZoneReleased\" -> \"ReturnToStandbyCMD\" [label=\"ε / gotoStandby\"];\n"
      + "    \"ReturnToStandbyCMD\" -> \"CheckCB\" [label=\"VGRCommandSuccessEventMessage\"];\n"
      + "}";

  private final VacuumGripper2CBFailureMissionActions actions;

  private final VacuumGripperMachine vacuumGripper;

  private final Zone zoneCB;

  private final Zone zoneMPS;

  public VacuumGripper2CBFailureMission(VacuumGripperMachine vacuumGripper, Zone zoneCB,
      Zone zoneMPS, VacuumGripper2CBFailureMissionActions actions) {
    this.vacuumGripper = vacuumGripper;
    this.zoneCB = zoneCB;
    this.zoneMPS = zoneMPS;
    this.actions = actions;

    // States are built from the collected transitions.
    RuntimeState setupCMD = new RuntimeState("SetupCMD");
    RuntimeState standbyCMD = new RuntimeState("StandbyCMD");
    RuntimeState checkCB = new RuntimeState("CheckCB");
    RuntimeState selectCBSlot = new RuntimeState("SelectCBSlot");
    RuntimeState idle = new RuntimeState("Idle");
    RuntimeState pickCBCMD = new RuntimeState("PickCBCMD");
    RuntimeState retractFromCBCMD = new RuntimeState("RetractFromCBCMD");
    RuntimeState cBZoneReleased = new RuntimeState("CBZoneReleased");
    RuntimeState waitForMPSZoneAcquisition = new RuntimeState("WaitForMPSZoneAcquisition");
    RuntimeState placeMPSinCMD = new RuntimeState("PlaceMPSinCMD");
    RuntimeState retractFromMPSCMD = new RuntimeState("RetractFromMPSCMD");
    RuntimeState mPSZoneReleased = new RuntimeState("MPSZoneReleased");
    RuntimeState returnToStandbyCMD = new RuntimeState("ReturnToStandbyCMD");

    // Transitions connect triggers, runtime actions, and next-state targets.
    this.runtime.setEntryTransition(new RuntimeTransition(CompletionEvent.class, event -> true, event -> this.vacuumGripper.setup(), setupCMD));
    setupCMD.addTransition(new RuntimeTransition(VGRCommandSuccessEventMessage.class, event -> true, event -> this.actions.gotoStandby(event, this.vacuumGripper, this.zoneCB, this.zoneMPS), standbyCMD, this.vacuumGripper));
    standbyCMD.addTransition(new RuntimeTransition(VGRCommandSuccessEventMessage.class, event -> true, event -> { }, checkCB, this.vacuumGripper));
    checkCB.addTransition(new RuntimeTransition(CompletionEvent.class, event -> true, event -> this.actions.askCBForToken(event, this.vacuumGripper, this.zoneCB, this.zoneMPS), selectCBSlot));
    selectCBSlot.addTransition(new RuntimeTransition(PickFromFeedEventMessage.class, event -> true, event -> this.actions.pickCBfeed(event, this.vacuumGripper, this.zoneCB, this.zoneMPS), pickCBCMD, this.zoneCB));
    selectCBSlot.addTransition(new RuntimeTransition(PickFromSwapEventMessage.class, event -> true, event -> this.actions.pickCBswap(event, this.vacuumGripper, this.zoneCB, this.zoneMPS), pickCBCMD, this.zoneCB));
    selectCBSlot.addTransition(new RuntimeTransition(NoTokenOnCBEventMessage.class, event -> true, event -> { }, idle, this.zoneCB));
    idle.addTransition(new RuntimeTransition(TokenOnCBEventMessage.class, event -> true, event -> { }, checkCB, this.zoneCB));
    pickCBCMD.addTransition(new RuntimeTransition(VGRCommandSuccessEventMessage.class, event -> true, event -> this.vacuumGripper.retractArm(), retractFromCBCMD, this.vacuumGripper));
    retractFromCBCMD.addTransition(new RuntimeTransition(VGRCommandSuccessEventMessage.class, event -> true, event -> this.zoneCB.publish(new TokenPickedFromCBEventMessage()), cBZoneReleased, this.vacuumGripper));
    cBZoneReleased.addTransition(new RuntimeTransition(CompletionEvent.class, event -> true, event -> this.actions.requestMPSZone(event, this.vacuumGripper, this.zoneCB, this.zoneMPS), waitForMPSZoneAcquisition));
    waitForMPSZoneAcquisition.addTransition(new RuntimeTransition(AcquireResponseEventMessage.class, event -> true, event -> this.actions.placeMPSin(event, this.vacuumGripper, this.zoneCB, this.zoneMPS), placeMPSinCMD, this.zoneMPS));
    placeMPSinCMD.addTransition(new RuntimeTransition(VGRCommandSuccessEventMessage.class, event -> true, event -> this.vacuumGripper.retractArm(), retractFromMPSCMD, this.vacuumGripper));
    retractFromMPSCMD.addTransition(new RuntimeTransition(VGRCommandSuccessEventMessage.class, event -> true, event -> this.zoneMPS.publish(new ReleaseRequestEventMessage()), mPSZoneReleased, this.vacuumGripper));
    mPSZoneReleased.addTransition(new RuntimeTransition(CompletionEvent.class, event -> true, event -> this.actions.gotoStandby(event, this.vacuumGripper, this.zoneCB, this.zoneMPS), returnToStandbyCMD));
    returnToStandbyCMD.addTransition(new RuntimeTransition(VGRCommandSuccessEventMessage.class, event -> true, event -> { }, checkCB, this.vacuumGripper));

    // Subscribe each mission machine to trigger event types used by this mission.
    vacuumGripper.subscribe(VGRCommandSuccessEventMessage.class, event -> onEvent(vacuumGripper, event));
    zoneCB.subscribe(PickFromFeedEventMessage.class, event -> onEvent(zoneCB, event));
    zoneCB.subscribe(PickFromSwapEventMessage.class, event -> onEvent(zoneCB, event));
    zoneCB.subscribe(NoTokenOnCBEventMessage.class, event -> onEvent(zoneCB, event));
    zoneCB.subscribe(TokenOnCBEventMessage.class, event -> onEvent(zoneCB, event));
    zoneMPS.subscribe(AcquireResponseEventMessage.class, event -> onEvent(zoneMPS, event));
  }

  @Override
  public String getName() {
    return "VacuumGripper2CBFailureMission";
  }

  @Override
  public List<MachineAdapter> getMachines() {
    return List.<MachineAdapter>of(this.vacuumGripper, this.zoneCB, this.zoneMPS);
  }

  @Override
  public String getDescription() {
    return "VGR2 when the conveyor belt fails: takes the tokens from the slot of the belt the dead CB zone gives, oldest first, to the MPS";
  }

  public static String toDot() {
    return DOT_SCHEMA;
  }

  @Override
  public String getDotGraph() {
    return DOT_SCHEMA;
  }
}
