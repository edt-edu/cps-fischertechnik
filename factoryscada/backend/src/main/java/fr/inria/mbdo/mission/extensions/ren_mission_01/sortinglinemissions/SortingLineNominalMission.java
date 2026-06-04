package fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinemissions;

import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinemissions.sortinglinenominalmission.transitionusage.AcceptWhenIdleToSortCMDEvent;
import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinemissions.sortinglinenominalmission.transitionusage.AcceptWhenSendColorUpdateToIdleEvent;
import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinesystem.sortingline.SortingLineMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinesystem.sortinglinemessages.RedTokenAvailableEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinesystem.sortinglinemessages.SLCommandSuccessEventMessage;
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
  private static final Logger logger = LoggerFactory.getLogger(SortingLineNominalMission.class);

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
    idle.addTransition(new RuntimeTransition(AcceptWhenIdleToSortCMDEvent.class, event -> true, event -> { }, sortCMD));
    sortCMD.addTransition(new RuntimeTransition(SLCommandSuccessEventMessage.class, event -> true, event -> { }, sendColorUpdate));
    sendColorUpdate.addTransition(new RuntimeTransition(AcceptWhenSendColorUpdateToIdleEvent.class, event -> true, event -> this.vacuumGripper.publish(new RedTokenAvailableEventMessage()), idle));

    // Subscribe each mission machine to trigger event types used by this mission.
    sortingLine.subscribe(SLCommandSuccessEventMessage.class, this::onEvent);
  }

  @Override
  public String getName() {
    return "SortingLineNominalMission";
  }
}
