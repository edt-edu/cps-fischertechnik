package fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinemissions.sortinglinenominalmission;

import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinemissions.sortinglinenominalmission.customevents.AcceptWhenSortingLineSensorSLblueEqualstrueEvent;
import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinemissions.sortinglinenominalmission.customevents.AcceptWhenSortingLineSensorSLinEqualstrueEvent;
import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinemissions.sortinglinenominalmission.customevents.AcceptWhenSortingLineSensorSLredEqualstrueEvent;
import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinemissions.sortinglinenominalmission.customevents.AcceptWhenSortingLineSensorSLwhiteEqualstrueEvent;
import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinesystem.sortingline.SortingLineMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinesystem.sortinglinemessages.BlueTokenAvailableEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinesystem.sortinglinemessages.RedTokenAvailableEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinesystem.sortinglinemessages.SLCommandSuccessEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinesystem.sortinglinemessages.WhiteTokenAvailableEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgripper.VacuumGripperMachine;
import fr.inria.mbdo.mission.runtime.api.AbstractMissionStrategy;
import fr.inria.mbdo.mission.runtime.rtc.def.RuntimeState;
import fr.inria.mbdo.mission.runtime.rtc.def.RuntimeTransition;
import fr.inria.mbdo.mission.runtime.rtc.event.CompletionEvent;
import java.lang.Override;
import java.lang.String;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * From SortingLineMissions::SortingLineNominalMission
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
      + "    \"SendColorUpdate\";\n"
      + "\n"
      + "    __init__ -> \"Idle\" [label=\"ε\"];\n"
      + "    \"Idle\" -> \"SortCMD\" [label=\"when(sortingLine.sensor_SL_in == true)\"];\n"
      + "    \"SortCMD\" -> \"SendColorUpdate\" [label=\"SLCommandSuccessEventMessage\"];\n"
      + "    \"SendColorUpdate\" -> \"Idle\" [label=\"when(sortingLine.sensor_SL_blue == true) / send SortingLineSystem::SortingLineMessages::BlueTokenAvailableEventMessage -> vacuumGripper\"];\n"
      + "    \"SendColorUpdate\" -> \"Idle\" [label=\"when(sortingLine.sensor_SL_white == true) / send SortingLineSystem::SortingLineMessages::WhiteTokenAvailableEventMessage -> vacuumGripper\"];\n"
      + "    \"SendColorUpdate\" -> \"Idle\" [label=\"when(sortingLine.sensor_SL_red == true) / send SortingLineSystem::SortingLineMessages::RedTokenAvailableEventMessage -> vacuumGripper\"];\n"
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
    RuntimeState sendColorUpdate = new RuntimeState("SendColorUpdate");

    // Transitions connect triggers, runtime actions, and next-state targets.
    this.runtime.setEntryTransition(new RuntimeTransition(CompletionEvent.class, event -> true, event -> { }, idle));
    idle.addTransition(new RuntimeTransition(AcceptWhenSortingLineSensorSLinEqualstrueEvent.class, event -> true, event -> { }, sortCMD));
    sortCMD.addTransition(new RuntimeTransition(SLCommandSuccessEventMessage.class, event -> true, event -> { }, sendColorUpdate));
    sendColorUpdate.addTransition(new RuntimeTransition(AcceptWhenSortingLineSensorSLblueEqualstrueEvent.class, event -> true, event -> this.vacuumGripper.publish(new BlueTokenAvailableEventMessage()), idle));
    sendColorUpdate.addTransition(new RuntimeTransition(AcceptWhenSortingLineSensorSLwhiteEqualstrueEvent.class, event -> true, event -> this.vacuumGripper.publish(new WhiteTokenAvailableEventMessage()), idle));
    sendColorUpdate.addTransition(new RuntimeTransition(AcceptWhenSortingLineSensorSLredEqualstrueEvent.class, event -> true, event -> this.vacuumGripper.publish(new RedTokenAvailableEventMessage()), idle));

    // Subscribe each mission machine to trigger event types used by this mission.
    sortingLine.subscribe(SLCommandSuccessEventMessage.class, this::onEvent);
  }

  @Override
  public String getName() {
    return "SortingLineNominalMission";
  }

  public static String toDot() {
    return DOT_SCHEMA;
  }
}
