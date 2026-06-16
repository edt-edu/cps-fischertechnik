package fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippermissions.vacuumgripper1nominalmission;

import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinesystem.sortingline.SortingLineMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgripper.VacuumGripperMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.Zone;
import fr.inria.mbdo.mission.runtime.rtc.event.Event;

/**
 * From VacuumGripperMissions::VacuumGripper1NominalMission
 * Nominal mission scenario for VacuumGripper n°1
 */
public interface VacuumGripper1NominalMissionActions {
  /**
   * TODO: implement transition action for fr.inria.mbdo.mission.ir.TransitionActionCustomIR:VacuumGripperMissions::VacuumGripper1NominalMission::pickWhite
   */
  void pickWhite(Event event, VacuumGripperMachine vacuumGripper, SortingLineMachine sortingLine,
      Zone zoneCB);

  /**
   * TODO: implement transition action for fr.inria.mbdo.mission.ir.TransitionActionCustomIR:VacuumGripperMissions::VacuumGripper1NominalMission::pickRed
   */
  void pickRed(Event event, VacuumGripperMachine vacuumGripper, SortingLineMachine sortingLine,
      Zone zoneCB);

  /**
   * TODO: implement transition action for fr.inria.mbdo.mission.ir.TransitionActionCustomIR:VacuumGripperMissions::VacuumGripper1NominalMission::goToStandby
   */
  void goToStandby(Event event, VacuumGripperMachine vacuumGripper, SortingLineMachine sortingLine,
      Zone zoneCB);

  /**
   * TODO: implement transition action for fr.inria.mbdo.mission.ir.TransitionActionCustomIR:VacuumGripperMissions::VacuumGripper1NominalMission::placeConveyoBeltFeed
   */
  void placeConveyoBeltFeed(Event event, VacuumGripperMachine vacuumGripper,
      SortingLineMachine sortingLine, Zone zoneCB);

  /**
   * TODO: implement transition action for fr.inria.mbdo.mission.ir.TransitionActionCustomIR:VacuumGripperMissions::VacuumGripper1NominalMission::pickBlue
   */
  void pickBlue(Event event, VacuumGripperMachine vacuumGripper, SortingLineMachine sortingLine,
      Zone zoneCB);
}
