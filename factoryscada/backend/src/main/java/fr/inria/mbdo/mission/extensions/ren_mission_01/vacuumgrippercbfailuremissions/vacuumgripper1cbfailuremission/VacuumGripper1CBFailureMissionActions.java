package fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippercbfailuremissions.vacuumgripper1cbfailuremission;

import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinesystem.sortingline.SortingLineMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgripper.VacuumGripperMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.Zone;
import fr.inria.mbdo.mission.runtime.rtc.event.Event;

/**
 * From VacuumGripperCBFailureMissions::VacuumGripper1CBFailureMission
 * VGR1 when the conveyor belt fails: takes the tokens at the sorting line outputs to the slot of the belt the dead CB zone gives
 */
public interface VacuumGripper1CBFailureMissionActions {
  /**
   * TODO: implement transition action for fr.inria.mbdo.mission.ir.TransitionActionCustomIR:VacuumGripperCBFailureMissions::VacuumGripper1CBFailureMission::pickWhite
   */
  void pickWhite(Event event, VacuumGripperMachine vacuumGripper, SortingLineMachine sortingLine,
      Zone zoneCB);

  /**
   * TODO: implement transition action for fr.inria.mbdo.mission.ir.TransitionActionCustomIR:VacuumGripperCBFailureMissions::VacuumGripper1CBFailureMission::pickBlue
   */
  void pickBlue(Event event, VacuumGripperMachine vacuumGripper, SortingLineMachine sortingLine,
      Zone zoneCB);

  /**
   * TODO: implement transition action for fr.inria.mbdo.mission.ir.TransitionActionCustomIR:VacuumGripperCBFailureMissions::VacuumGripper1CBFailureMission::askSortingLine
   */
  void askSortingLine(Event event, VacuumGripperMachine vacuumGripper,
      SortingLineMachine sortingLine, Zone zoneCB);

  /**
   * TODO: implement transition action for fr.inria.mbdo.mission.ir.TransitionActionCustomIR:VacuumGripperCBFailureMissions::VacuumGripper1CBFailureMission::gotoStandby
   */
  void gotoStandby(Event event, VacuumGripperMachine vacuumGripper, SortingLineMachine sortingLine,
      Zone zoneCB);

  /**
   * TODO: implement transition action for fr.inria.mbdo.mission.ir.TransitionActionCustomIR:VacuumGripperCBFailureMissions::VacuumGripper1CBFailureMission::placeConveyorBeltSwap
   */
  void placeConveyorBeltSwap(Event event, VacuumGripperMachine vacuumGripper,
      SortingLineMachine sortingLine, Zone zoneCB);

  /**
   * TODO: implement transition action for fr.inria.mbdo.mission.ir.TransitionActionCustomIR:VacuumGripperCBFailureMissions::VacuumGripper1CBFailureMission::placeConveyorBeltFeed
   */
  void placeConveyorBeltFeed(Event event, VacuumGripperMachine vacuumGripper,
      SortingLineMachine sortingLine, Zone zoneCB);

  /**
   * TODO: implement transition action for fr.inria.mbdo.mission.ir.TransitionActionCustomIR:VacuumGripperCBFailureMissions::VacuumGripper1CBFailureMission::askCBSlot
   */
  void askCBSlot(Event event, VacuumGripperMachine vacuumGripper, SortingLineMachine sortingLine,
      Zone zoneCB);

  /**
   * TODO: implement transition action for fr.inria.mbdo.mission.ir.TransitionActionCustomIR:VacuumGripperCBFailureMissions::VacuumGripper1CBFailureMission::pickRed
   */
  void pickRed(Event event, VacuumGripperMachine vacuumGripper, SortingLineMachine sortingLine,
      Zone zoneCB);
}
