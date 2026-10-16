package fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinemissions.sortinglinenominalmission;

import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinesystem.sortingline.SortingLineMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgripper.VacuumGripperMachine;
import fr.inria.mbdo.mission.runtime.rtc.event.Event;

/**
 * From SortingLineMissions::SortingLineNominalMission
 * Nominal mission scenario for SortingLine
 */
public interface SortingLineNominalMissionActions {
  /**
   * TODO: implement transition action for fr.inria.mbdo.mission.ir.TransitionActionCustomIR:SortingLineMissions::SortingLineNominalMission::answerTokenRequest
   */
  void answerTokenRequest(Event event, SortingLineMachine sortingLine,
      VacuumGripperMachine vacuumGripper);
}
