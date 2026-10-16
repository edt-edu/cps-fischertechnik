package fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippercbfailuremissions.vacuumgripper2cbfailuremission;

import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgripper.VacuumGripperMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.Zone;
import fr.inria.mbdo.mission.runtime.rtc.event.Event;

/**
 * From VacuumGripperCBFailureMissions::VacuumGripper2CBFailureMission
 * VGR2 when the conveyor belt fails: takes the tokens from the slot of the belt the dead CB zone gives, oldest first, to the MPS
 */
public interface VacuumGripper2CBFailureMissionActions {
  /**
   * TODO: implement transition action for fr.inria.mbdo.mission.ir.TransitionActionCustomIR:VacuumGripperCBFailureMissions::VacuumGripper2CBFailureMission::pickCBfeed
   */
  void pickCBfeed(Event event, VacuumGripperMachine vacuumGripper, Zone zoneCB, Zone zoneMPS);

  /**
   * TODO: implement transition action for fr.inria.mbdo.mission.ir.TransitionActionCustomIR:VacuumGripperCBFailureMissions::VacuumGripper2CBFailureMission::askCBForToken
   */
  void askCBForToken(Event event, VacuumGripperMachine vacuumGripper, Zone zoneCB, Zone zoneMPS);

  /**
   * TODO: implement transition action for fr.inria.mbdo.mission.ir.TransitionActionCustomIR:VacuumGripperCBFailureMissions::VacuumGripper2CBFailureMission::pickCBswap
   */
  void pickCBswap(Event event, VacuumGripperMachine vacuumGripper, Zone zoneCB, Zone zoneMPS);

  /**
   * TODO: implement transition action for fr.inria.mbdo.mission.ir.TransitionActionCustomIR:VacuumGripperCBFailureMissions::VacuumGripper2CBFailureMission::gotoStandby
   */
  void gotoStandby(Event event, VacuumGripperMachine vacuumGripper, Zone zoneCB, Zone zoneMPS);

  /**
   * TODO: implement transition action for fr.inria.mbdo.mission.ir.TransitionActionCustomIR:VacuumGripperCBFailureMissions::VacuumGripper2CBFailureMission::placeMPSin
   */
  void placeMPSin(Event event, VacuumGripperMachine vacuumGripper, Zone zoneCB, Zone zoneMPS);

  /**
   * TODO: implement transition action for fr.inria.mbdo.mission.ir.TransitionActionCustomIR:VacuumGripperCBFailureMissions::VacuumGripper2CBFailureMission::requestMPSZone
   */
  void requestMPSZone(Event event, VacuumGripperMachine vacuumGripper, Zone zoneCB, Zone zoneMPS);
}
