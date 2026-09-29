package fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippermissions.vacuumgripper2nominalmission;

import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltsystem.conveyorbeltmessages.SwapBusyEventMessage;
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
      + "    \"SetupIdle\";\n"
      + "    \"GotoStandbyCMD\";\n"
      + "    \"Idle\";\n"
      + "    \"WaitForCBZoneAcquisition\";\n"
      + "    \"PickCBswapCMD\";\n"
      + "    \"ReleaseCBZone\";\n"
      + "    \"WaitForMPSZoneAcquisition\";\n"
      + "    \"PlaceMPSinCMD\";\n"
      + "    \"ReleaseMPSZone\";\n"
      + "\n"
      + "    __init__ -> \"SetupIdle\" [label=\"ε / vacuumGripper.setup()\"];\n"
      + "    \"SetupIdle\" -> \"GotoStandbyCMD\" [label=\"VGRCommandSuccessEventMessage / gotoStandby\"];\n"
      + "    \"GotoStandbyCMD\" -> \"Idle\" [label=\"VGRCommandSuccessEventMessage\"];\n"
      + "    \"Idle\" -> \"WaitForCBZoneAcquisition\" [label=\"SwapBusyEventMessage / send ZonesSystem::ZonesMessages::AcquireRequestEventMessage -> zoneCB\"];\n"
      + "    \"WaitForCBZoneAcquisition\" -> \"PickCBswapCMD\" [label=\"AcquireResponseEventMessage / pickCBswap\"];\n"
      + "    \"PickCBswapCMD\" -> \"ReleaseCBZone\" [label=\"VGRCommandSuccessEventMessage / gotoStandby\"];\n"
      + "    \"ReleaseCBZone\" -> \"WaitForMPSZoneAcquisition\" [label=\"VGRCommandSuccessEventMessage / releaseCBZoneAndAcquireMPSZone\"];\n"
      + "    \"WaitForMPSZoneAcquisition\" -> \"PlaceMPSinCMD\" [label=\"AcquireResponseEventMessage / placeMPSin\"];\n"
      + "    \"PlaceMPSinCMD\" -> \"ReleaseMPSZone\" [label=\"VGRCommandSuccessEventMessage / gotoStandby\"];\n"
      + "    \"ReleaseMPSZone\" -> \"GotoStandbyCMD\" [label=\"VGRCommandSuccessEventMessage / send ZonesSystem::ZonesMessages::ReleaseRequestEventMessage -> zoneMPS\"];\n"
      + "}";

  private final VacuumGripper2NominalMissionActions actions;

  private final VacuumGripperMachine vacuumGripper;

  private final Zone zoneCB;

  private final Zone zoneMPS;

  public VacuumGripper2NominalMission(VacuumGripperMachine vacuumGripper, Zone zoneCB, Zone zoneMPS,
      VacuumGripper2NominalMissionActions actions) {
    this.vacuumGripper = vacuumGripper;
    this.zoneCB = zoneCB;
    this.zoneMPS = zoneMPS;
    this.actions = actions;

    // States are built from the collected transitions.
    RuntimeState setupIdle = new RuntimeState("SetupIdle");
    RuntimeState gotoStandbyCMD = new RuntimeState("GotoStandbyCMD");
    RuntimeState idle = new RuntimeState("Idle");
    RuntimeState waitForCBZoneAcquisition = new RuntimeState("WaitForCBZoneAcquisition");
    RuntimeState pickCBswapCMD = new RuntimeState("PickCBswapCMD");
    RuntimeState releaseCBZone = new RuntimeState("ReleaseCBZone");
    RuntimeState waitForMPSZoneAcquisition = new RuntimeState("WaitForMPSZoneAcquisition");
    RuntimeState placeMPSinCMD = new RuntimeState("PlaceMPSinCMD");
    RuntimeState releaseMPSZone = new RuntimeState("ReleaseMPSZone");

    // Transitions connect triggers, runtime actions, and next-state targets.
    this.runtime.setEntryTransition(new RuntimeTransition(CompletionEvent.class, event -> true, event -> this.vacuumGripper.setup(), setupIdle));
    setupIdle.addTransition(new RuntimeTransition(VGRCommandSuccessEventMessage.class, event -> true, event -> this.actions.gotoStandby(event, this.vacuumGripper, this.zoneCB, this.zoneMPS), gotoStandbyCMD));
    gotoStandbyCMD.addTransition(new RuntimeTransition(VGRCommandSuccessEventMessage.class, event -> true, event -> { }, idle));
    idle.addTransition(new RuntimeTransition(SwapBusyEventMessage.class, event -> true, event -> this.zoneCB.publish(new AcquireRequestEventMessage()), waitForCBZoneAcquisition));
    waitForCBZoneAcquisition.addTransition(new RuntimeTransition(AcquireResponseEventMessage.class, event -> true, event -> this.actions.pickCBswap(event, this.vacuumGripper, this.zoneCB, this.zoneMPS), pickCBswapCMD));
    pickCBswapCMD.addTransition(new RuntimeTransition(VGRCommandSuccessEventMessage.class, event -> true, event -> this.actions.gotoStandby(event, this.vacuumGripper, this.zoneCB, this.zoneMPS), releaseCBZone));
    releaseCBZone.addTransition(new RuntimeTransition(VGRCommandSuccessEventMessage.class, event -> true, event -> this.actions.releaseCBZoneAndAcquireMPSZone(event, this.vacuumGripper, this.zoneCB, this.zoneMPS), waitForMPSZoneAcquisition));
    waitForMPSZoneAcquisition.addTransition(new RuntimeTransition(AcquireResponseEventMessage.class, event -> true, event -> this.actions.placeMPSin(event, this.vacuumGripper, this.zoneCB, this.zoneMPS), placeMPSinCMD));
    placeMPSinCMD.addTransition(new RuntimeTransition(VGRCommandSuccessEventMessage.class, event -> true, event -> this.actions.gotoStandby(event, this.vacuumGripper, this.zoneCB, this.zoneMPS), releaseMPSZone));
    releaseMPSZone.addTransition(new RuntimeTransition(VGRCommandSuccessEventMessage.class, event -> true, event -> this.zoneMPS.publish(new ReleaseRequestEventMessage()), gotoStandbyCMD));

    // Subscribe each mission machine to trigger event types used by this mission.
    vacuumGripper.subscribe(VGRCommandSuccessEventMessage.class, this::onEvent);
    vacuumGripper.subscribe(SwapBusyEventMessage.class, this::onEvent);
    zoneCB.subscribe(AcquireResponseEventMessage.class, this::onEvent);
    zoneMPS.subscribe(AcquireResponseEventMessage.class, this::onEvent);
  }

  @Override
  public String getName() {
    return "VacuumGripper2NominalMission";
  }

  @Override
  public List<MachineAdapter> getMachines() {
    return List.<MachineAdapter>of(this.vacuumGripper, this.zoneCB, this.zoneMPS);
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
