package fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippermissions.vacuumgripper2nominalmission;

import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltsystem.conveyorbelt.ConveyorBeltMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltsystem.conveyorbeltmessages.SwapBusyEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgripper.VacuumGripperMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgrippermessages.VGRCommandSuccessEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.Zone;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.AcquireRequestEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.AcquireResponseEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.CBTokenRequestEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.NoTokenOnCBEventMessage;
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
 * From VacuumGripperMissions::VacuumGripper2NominalMission
 * Nominal mission scenario for VacuumGripper n°2
 */
public class VacuumGripper2NominalMission extends AbstractMissionStrategy {
  private static final Logger logger = LoggerFactory.getLogger("VacuumGripper2NominalMission");

  public static final String DOT_SCHEMA = "digraph VacuumGripper2NominalMission {\n"
      + "    fontname=\"Helvetica,Arial,sans-serif\"\n"
      + "    node [fontname=\"Helvetica,Arial,sans-serif\"]\n"
      + "    edge [fontname=\"Helvetica,Arial,sans-serif\"]\n"
      + "    rankdir=LR;\n"
      + "    node [shape=point, label=\"\"]; __init__;\n"
      + "    node [shape=circle, style=\"\", fillcolor=\"\"];\n"
      + "    \"SetupCMD\";\n"
      + "    \"StandbyCMD\";\n"
      + "    \"CheckSwap\";\n"
      + "    \"Idle\";\n"
      + "    \"WaitForCBZoneAcquisition\";\n"
      + "    \"PickCBswapCMD\";\n"
      + "    \"RetractFromCBCMD\";\n"
      + "    \"CBZoneReleased\";\n"
      + "    \"StandbyBeforeMPSCMD\";\n"
      + "    \"WaitForMPSZoneAcquisition\";\n"
      + "    \"PlaceMPSinCMD\";\n"
      + "    \"RetractFromMPSCMD\";\n"
      + "    \"MPSZoneReleased\";\n"
      + "    \"ReturnToStandbyCMD\";\n"
      + "\n"
      + "    __init__ -> \"SetupCMD\" [label=\"ε / vacuumGripper.setup()\"];\n"
      + "    \"SetupCMD\" -> \"StandbyCMD\" [label=\"VGRCommandSuccessEventMessage / gotoStandby\"];\n"
      + "    \"StandbyCMD\" -> \"CheckSwap\" [label=\"VGRCommandSuccessEventMessage\"];\n"
      + "    \"CheckSwap\" -> \"Idle\" [label=\"ε / checkSwap\"];\n"
      + "    \"Idle\" -> \"WaitForCBZoneAcquisition\" [label=\"SwapBusyEventMessage / send ZonesSystem::ZonesMessages::CBTokenRequestEventMessage -> zoneCB\"];\n"
      + "    \"Idle\" -> \"WaitForCBZoneAcquisition\" [label=\"TokenOnCBEventMessage / send ZonesSystem::ZonesMessages::CBTokenRequestEventMessage -> zoneCB\"];\n"
      + "    \"WaitForCBZoneAcquisition\" -> \"PickCBswapCMD\" [label=\"PickFromSwapEventMessage / pickCBswap\"];\n"
      + "    \"WaitForCBZoneAcquisition\" -> \"Idle\" [label=\"NoTokenOnCBEventMessage\"];\n"
      + "    \"WaitForCBZoneAcquisition\" -> \"WaitForCBZoneAcquisition\" [label=\"TokenOnCBEventMessage / send ZonesSystem::ZonesMessages::CBTokenRequestEventMessage -> zoneCB\"];\n"
      + "    \"PickCBswapCMD\" -> \"RetractFromCBCMD\" [label=\"VGRCommandSuccessEventMessage / vacuumGripper.retractArm()\"];\n"
      + "    \"RetractFromCBCMD\" -> \"CBZoneReleased\" [label=\"VGRCommandSuccessEventMessage / send ZonesSystem::ZonesMessages::TokenPickedFromCBEventMessage -> zoneCB\"];\n"
      + "    \"CBZoneReleased\" -> \"StandbyBeforeMPSCMD\" [label=\"ε / gotoStandby\"];\n"
      + "    \"StandbyBeforeMPSCMD\" -> \"WaitForMPSZoneAcquisition\" [label=\"VGRCommandSuccessEventMessage / send ZonesSystem::ZonesMessages::AcquireRequestEventMessage -> zoneMPS\"];\n"
      + "    \"WaitForMPSZoneAcquisition\" -> \"PlaceMPSinCMD\" [label=\"AcquireResponseEventMessage / placeMPSin\"];\n"
      + "    \"PlaceMPSinCMD\" -> \"RetractFromMPSCMD\" [label=\"VGRCommandSuccessEventMessage / vacuumGripper.retractArm()\"];\n"
      + "    \"RetractFromMPSCMD\" -> \"MPSZoneReleased\" [label=\"VGRCommandSuccessEventMessage / send ZonesSystem::ZonesMessages::ReleaseRequestEventMessage -> zoneMPS\"];\n"
      + "    \"MPSZoneReleased\" -> \"ReturnToStandbyCMD\" [label=\"ε / gotoStandby\"];\n"
      + "    \"ReturnToStandbyCMD\" -> \"CheckSwap\" [label=\"VGRCommandSuccessEventMessage\"];\n"
      + "}";

  private final VacuumGripper2NominalMissionActions actions;

  private final VacuumGripperMachine vacuumGripper;

  private final ConveyorBeltMachine conveyorBelt;

  private final Zone zoneCB;

  private final Zone zoneMPS;

  public VacuumGripper2NominalMission(VacuumGripperMachine vacuumGripper,
      ConveyorBeltMachine conveyorBelt, Zone zoneCB, Zone zoneMPS,
      VacuumGripper2NominalMissionActions actions) {
    this.vacuumGripper = vacuumGripper;
    this.conveyorBelt = conveyorBelt;
    this.zoneCB = zoneCB;
    this.zoneMPS = zoneMPS;
    this.actions = actions;

    // States are built from the collected transitions.
    RuntimeState setupCMD = new RuntimeState("SetupCMD");
    RuntimeState standbyCMD = new RuntimeState("StandbyCMD");
    RuntimeState checkSwap = new RuntimeState("CheckSwap");
    RuntimeState idle = new RuntimeState("Idle");
    RuntimeState waitForCBZoneAcquisition = new RuntimeState("WaitForCBZoneAcquisition");
    RuntimeState pickCBswapCMD = new RuntimeState("PickCBswapCMD");
    RuntimeState retractFromCBCMD = new RuntimeState("RetractFromCBCMD");
    RuntimeState cBZoneReleased = new RuntimeState("CBZoneReleased");
    RuntimeState standbyBeforeMPSCMD = new RuntimeState("StandbyBeforeMPSCMD");
    RuntimeState waitForMPSZoneAcquisition = new RuntimeState("WaitForMPSZoneAcquisition");
    RuntimeState placeMPSinCMD = new RuntimeState("PlaceMPSinCMD");
    RuntimeState retractFromMPSCMD = new RuntimeState("RetractFromMPSCMD");
    RuntimeState mPSZoneReleased = new RuntimeState("MPSZoneReleased");
    RuntimeState returnToStandbyCMD = new RuntimeState("ReturnToStandbyCMD");

    // Transitions connect triggers, runtime actions, and next-state targets.
    this.runtime.setEntryTransition(new RuntimeTransition(CompletionEvent.class, event -> true, event -> this.vacuumGripper.setup(), setupCMD));
    setupCMD.addTransition(new RuntimeTransition(VGRCommandSuccessEventMessage.class, event -> true, event -> this.actions.gotoStandby(event, this.vacuumGripper, this.conveyorBelt, this.zoneCB, this.zoneMPS), standbyCMD, this.vacuumGripper));
    standbyCMD.addTransition(new RuntimeTransition(VGRCommandSuccessEventMessage.class, event -> true, event -> { }, checkSwap, this.vacuumGripper));
    checkSwap.addTransition(new RuntimeTransition(CompletionEvent.class, event -> true, event -> this.actions.checkSwap(event, this.vacuumGripper, this.conveyorBelt, this.zoneCB, this.zoneMPS), idle));
    idle.addTransition(new RuntimeTransition(SwapBusyEventMessage.class, event -> true, event -> this.zoneCB.publish(new CBTokenRequestEventMessage()), waitForCBZoneAcquisition, this.vacuumGripper));
    idle.addTransition(new RuntimeTransition(TokenOnCBEventMessage.class, event -> true, event -> this.zoneCB.publish(new CBTokenRequestEventMessage()), waitForCBZoneAcquisition, this.zoneCB));
    waitForCBZoneAcquisition.addTransition(new RuntimeTransition(PickFromSwapEventMessage.class, event -> true, event -> this.actions.pickCBswap(event, this.vacuumGripper, this.conveyorBelt, this.zoneCB, this.zoneMPS), pickCBswapCMD, this.zoneCB));
    waitForCBZoneAcquisition.addTransition(new RuntimeTransition(NoTokenOnCBEventMessage.class, event -> true, event -> { }, idle, this.zoneCB));
    waitForCBZoneAcquisition.addTransition(new RuntimeTransition(TokenOnCBEventMessage.class, event -> true, event -> this.zoneCB.publish(new CBTokenRequestEventMessage()), waitForCBZoneAcquisition, this.zoneCB));
    pickCBswapCMD.addTransition(new RuntimeTransition(VGRCommandSuccessEventMessage.class, event -> true, event -> this.vacuumGripper.retractArm(), retractFromCBCMD, this.vacuumGripper));
    retractFromCBCMD.addTransition(new RuntimeTransition(VGRCommandSuccessEventMessage.class, event -> true, event -> this.zoneCB.publish(new TokenPickedFromCBEventMessage()), cBZoneReleased, this.vacuumGripper));
    cBZoneReleased.addTransition(new RuntimeTransition(CompletionEvent.class, event -> true, event -> this.actions.gotoStandby(event, this.vacuumGripper, this.conveyorBelt, this.zoneCB, this.zoneMPS), standbyBeforeMPSCMD));
    standbyBeforeMPSCMD.addTransition(new RuntimeTransition(VGRCommandSuccessEventMessage.class, event -> true, event -> this.zoneMPS.publish(new AcquireRequestEventMessage()), waitForMPSZoneAcquisition, this.vacuumGripper));
    waitForMPSZoneAcquisition.addTransition(new RuntimeTransition(AcquireResponseEventMessage.class, event -> true, event -> this.actions.placeMPSin(event, this.vacuumGripper, this.conveyorBelt, this.zoneCB, this.zoneMPS), placeMPSinCMD, this.zoneMPS));
    placeMPSinCMD.addTransition(new RuntimeTransition(VGRCommandSuccessEventMessage.class, event -> true, event -> this.vacuumGripper.retractArm(), retractFromMPSCMD, this.vacuumGripper));
    retractFromMPSCMD.addTransition(new RuntimeTransition(VGRCommandSuccessEventMessage.class, event -> true, event -> this.zoneMPS.publish(new ReleaseRequestEventMessage()), mPSZoneReleased, this.vacuumGripper));
    mPSZoneReleased.addTransition(new RuntimeTransition(CompletionEvent.class, event -> true, event -> this.actions.gotoStandby(event, this.vacuumGripper, this.conveyorBelt, this.zoneCB, this.zoneMPS), returnToStandbyCMD));
    returnToStandbyCMD.addTransition(new RuntimeTransition(VGRCommandSuccessEventMessage.class, event -> true, event -> { }, checkSwap, this.vacuumGripper));

    // Subscribe each mission machine to trigger event types used by this mission.
    vacuumGripper.subscribe(VGRCommandSuccessEventMessage.class, event -> onEvent(vacuumGripper, event));
    vacuumGripper.subscribe(SwapBusyEventMessage.class, event -> onEvent(vacuumGripper, event));
    zoneCB.subscribe(TokenOnCBEventMessage.class, event -> onEvent(zoneCB, event));
    zoneCB.subscribe(PickFromSwapEventMessage.class, event -> onEvent(zoneCB, event));
    zoneCB.subscribe(NoTokenOnCBEventMessage.class, event -> onEvent(zoneCB, event));
    zoneMPS.subscribe(AcquireResponseEventMessage.class, event -> onEvent(zoneMPS, event));
  }

  @Override
  public String getName() {
    return "VacuumGripper2NominalMission";
  }

  @Override
  public List<MachineAdapter> getMachines() {
    return List.<MachineAdapter>of(this.vacuumGripper, this.conveyorBelt, this.zoneCB, this.zoneMPS);
  }

  @Override
  public String getDescription() {
    return "Nominal mission scenario for VacuumGripper n°2";
  }

  public static String toDot() {
    return DOT_SCHEMA;
  }

  @Override
  public String getDotGraph() {
    return DOT_SCHEMA;
  }
}
