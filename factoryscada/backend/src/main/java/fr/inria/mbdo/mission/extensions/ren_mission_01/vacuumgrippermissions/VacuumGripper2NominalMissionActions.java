package fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippermissions;

import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgripper.VacuumGripperMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.Zone;
import fr.inria.mbdo.mission.runtime.rtc.event.Event;

/**
 * From VacuumGripperMissions::VacuumGripper2NominalMission
 */
public interface VacuumGripper2NominalMissionActions {
  /**
   * TODO: implement transition action for fr.inria.mbdo.mission.ir.TransitionActionCustomIR:VacuumGripperMissions::VacuumGripper2NominalMission::placeMPSin
   */
  void placeMPSin(Event event, VacuumGripperMachine vacuumGripper, Zone zoneCB, Zone zoneMPS);

  /**
   * TODO: implement transition action for fr.inria.mbdo.mission.ir.TransitionActionCustomIR:PerformActionUsage
   */
  void performActionUsage(Event event, VacuumGripperMachine vacuumGripper, Zone zoneCB,
      Zone zoneMPS);

  /**
   * TODO: implement transition action for fr.inria.mbdo.mission.ir.TransitionActionCustomIR:VacuumGripperMissions::VacuumGripper2NominalMission::pickCBswap
   */
  void pickCBswap(Event event, VacuumGripperMachine vacuumGripper, Zone zoneCB, Zone zoneMPS);

  /**
   * TODO: implement transition action for fr.inria.mbdo.mission.ir.TransitionActionCustomIR:VacuumGripperMissions::VacuumGripper2NominalMission::goToStandby
   */
  void goToStandby(Event event, VacuumGripperMachine vacuumGripper, Zone zoneCB, Zone zoneMPS);
}
