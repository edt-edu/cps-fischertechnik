package fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippermissions.vacuumgripper2nominalmission;

import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgripper.VacuumGripperMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.Zone;
import fr.inria.mbdo.mission.runtime.rtc.event.Event;

/**
 * From VacuumGripperMissions::VacuumGripper2NominalMission
 * Nominal mission scenario for VacuumGripper n°2
 */
public interface VacuumGripper2NominalMissionActions {
  /**
   * TODO: implement transition action for fr.inria.mbdo.mission.ir.TransitionActionCustomIR:VacuumGripperMissions::VacuumGripper2NominalMission::placeMPSin
   */
  void placeMPSin(Event event, VacuumGripperMachine vacuumGripper, Zone zoneCB, Zone zoneMPS);

  /**
   * TODO: implement transition action for fr.inria.mbdo.mission.ir.TransitionActionCustomIR:VacuumGripperMissions::VacuumGripper2NominalMission::pickCBswap
   */
  void pickCBswap(Event event, VacuumGripperMachine vacuumGripper, Zone zoneCB, Zone zoneMPS);

  /**
   * TODO: implement transition action for fr.inria.mbdo.mission.ir.TransitionActionCustomIR:VacuumGripperMissions::VacuumGripper2NominalMission::gotoStandby
   */
  void gotoStandby(Event event, VacuumGripperMachine vacuumGripper, Zone zoneCB, Zone zoneMPS);
}
