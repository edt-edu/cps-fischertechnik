package fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinemissions.sortinglinenominalmission;

import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinemissions.sortinglinenominalmission.customevents.AcceptWhenSortingLineSensorSLblueEqualsfalseEvent;
import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinemissions.sortinglinenominalmission.customevents.AcceptWhenSortingLineSensorSLinEqualsfalseEvent;
import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinemissions.sortinglinenominalmission.customevents.AcceptWhenSortingLineSensorSLredEqualsfalseEvent;
import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinemissions.sortinglinenominalmission.customevents.AcceptWhenSortingLineSensorSLwhiteEqualsfalseEvent;
import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinesystem.sortingline.SortingLineMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinesystem.sortinglinemessages.BlueTokenAvailableEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinesystem.sortinglinemessages.RedTokenAvailableEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinesystem.sortinglinemessages.WhiteTokenAvailableEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgripper.VacuumGripperMachine;
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
 * From SortingLineMissions::SortingLineNominalMission
 * Nominal mission scenario for SortingLine
 */
public class SortingLineNominalMission extends AbstractMissionStrategy {
  private static final Logger logger = LoggerFactory.getLogger("SortingLineNominalMission");

  public static final String DOT_SCHEMA = "digraph SortingLineNominalMission {\n"
      + "    fontname=\"Helvetica,Arial,sans-serif\"\n"
      + "    node [fontname=\"Helvetica,Arial,sans-serif\"]\n"
      + "    edge [fontname=\"Helvetica,Arial,sans-serif\"]\n"
      + "    rankdir=LR;\n"
      + "    node [shape=point, label=\"\"]; __init__;\n"
      + "    node [shape=circle, style=\"\", fillcolor=\"\"];\n"
      + "    \"Idle\";\n"
      + "    \"SortCMD\";\n"
      + "\n"
      + "    __init__ -> \"Idle\" [label=\"ε\"];\n"
      + "    \"Idle\" -> \"SortCMD\" [label=\"when(sortingLine.sensor_SL_in == false) / sortingLine.eject()\"];\n"
      + "    \"SortCMD\" -> \"Idle\" [label=\"when(sortingLine.sensor_SL_blue == false) / send SortingLineSystem::SortingLineMessages::BlueTokenAvailableEventMessage -> vacuumGripper\"];\n"
      + "    \"SortCMD\" -> \"Idle\" [label=\"when(sortingLine.sensor_SL_white == false) / send SortingLineSystem::SortingLineMessages::WhiteTokenAvailableEventMessage -> vacuumGripper\"];\n"
      + "    \"SortCMD\" -> \"Idle\" [label=\"when(sortingLine.sensor_SL_red == false) / send SortingLineSystem::SortingLineMessages::RedTokenAvailableEventMessage -> vacuumGripper\"];\n"
      + "}";

  private final SortingLineNominalMissionActions actions;

  private final SortingLineMachine sortingLine;

  private final VacuumGripperMachine vacuumGripper;

  public SortingLineNominalMission(SortingLineMachine sortingLine,
      VacuumGripperMachine vacuumGripper, SortingLineNominalMissionActions actions) {
    this.sortingLine = sortingLine;
    this.vacuumGripper = vacuumGripper;
    this.actions = actions;

    // States are built from the collected transitions.
    RuntimeState idle = new RuntimeState("Idle");
    RuntimeState sortCMD = new RuntimeState("SortCMD");

    // Transitions connect triggers, runtime actions, and next-state targets.
    this.runtime.setEntryTransition(new RuntimeTransition(CompletionEvent.class, event -> true, event -> { }, idle));
    idle.addTransition(new RuntimeTransition(AcceptWhenSortingLineSensorSLinEqualsfalseEvent.class, event -> true, event -> this.sortingLine.eject(), sortCMD));
    sortCMD.addTransition(new RuntimeTransition(AcceptWhenSortingLineSensorSLblueEqualsfalseEvent.class, event -> true, event -> this.sortingLine.publish(new BlueTokenAvailableEventMessage()), idle));
    sortCMD.addTransition(new RuntimeTransition(AcceptWhenSortingLineSensorSLwhiteEqualsfalseEvent.class, event -> true, event -> this.sortingLine.publish(new WhiteTokenAvailableEventMessage()), idle));
    sortCMD.addTransition(new RuntimeTransition(AcceptWhenSortingLineSensorSLredEqualsfalseEvent.class, event -> true, event -> this.sortingLine.publish(new RedTokenAvailableEventMessage()), idle));

    // Subscribe each mission machine to trigger event types used by this mission.
    sortingLine.subscribe(AcceptWhenSortingLineSensorSLinEqualsfalseEvent.class, event -> onEvent(sortingLine, event));
    sortingLine.subscribe(AcceptWhenSortingLineSensorSLblueEqualsfalseEvent.class, event -> onEvent(sortingLine, event));
    sortingLine.subscribe(AcceptWhenSortingLineSensorSLwhiteEqualsfalseEvent.class, event -> onEvent(sortingLine, event));
    sortingLine.subscribe(AcceptWhenSortingLineSensorSLredEqualsfalseEvent.class, event -> onEvent(sortingLine, event));
  }

  @Override
  public String getName() {
    return "SortingLineNominalMission";
  }

  @Override
  public List<MachineAdapter> getMachines() {
    return List.<MachineAdapter>of(this.sortingLine, this.vacuumGripper);
  }

  @Override
  public String getDescription() {
    return "Nominal mission scenario for SortingLine";
  }

  public static String toDot() {
    return DOT_SCHEMA;
  }

  @Override
  public String getDotGraph() {
    return DOT_SCHEMA;
  }
}
