package fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippermissions.vacuumgripper1secondarymission;

import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinesystem.sortingline.SortingLineMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgripper.VacuumGripperMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.Zone;
import fr.inria.mbdo.mission.runtime.rtc.event.Event;

/**
 * From VacuumGripperMissions::VacuumGripper1SecondaryMission
 * Nominal mission scenario for VacuumGripper n°1
 */
public interface VacuumGripper1SecondaryMissionActions {
  /**
   * TODO: implement transition action for fr.inria.mbdo.mission.ir.TransitionActionCustomIR:VacuumGripperMissions::VacuumGripper1SecondaryMission::gotoRedPosition
   */
  void gotoRedPosition(Event event, VacuumGripperMachine vacuumGripper,
      SortingLineMachine sortingLine, Zone zoneCB);

  /**
   * TODO: implement transition action for fr.inria.mbdo.mission.ir.TransitionActionCustomIR:VacuumGripperMissions::VacuumGripper1SecondaryMission::gotoWhitePosition
   */
  void gotoWhitePosition(Event event, VacuumGripperMachine vacuumGripper,
      SortingLineMachine sortingLine, Zone zoneCB);

  /**
   * TODO: implement transition action for fr.inria.mbdo.mission.ir.TransitionActionCustomIR:VacuumGripperMissions::VacuumGripper1SecondaryMission::placeConveyoBeltFeed
   */
  void placeConveyoBeltFeed(Event event, VacuumGripperMachine vacuumGripper,
      SortingLineMachine sortingLine, Zone zoneCB);

  /**
   * TODO: implement transition action for fr.inria.mbdo.mission.ir.TransitionActionCustomIR:VacuumGripperMissions::VacuumGripper1SecondaryMission::gotoStandby
   */
  void gotoStandby(Event event, VacuumGripperMachine vacuumGripper, SortingLineMachine sortingLine,
      Zone zoneCB);

  /**
   * TODO: implement transition action for fr.inria.mbdo.mission.ir.TransitionActionCustomIR:VacuumGripperMissions::VacuumGripper1SecondaryMission::gotoBluePosition
   */
  void gotoBluePosition(Event event, VacuumGripperMachine vacuumGripper,
      SortingLineMachine sortingLine, Zone zoneCB);

  /**
   * TODO: implement transition action for fr.inria.mbdo.mission.ir.TransitionActionCustomIR:VacuumGripperMissions::VacuumGripper1SecondaryMission::pickColor
   */
  void pickColor(Event event, VacuumGripperMachine vacuumGripper, SortingLineMachine sortingLine,
      Zone zoneCB);
}
