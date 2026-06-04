package fr.inria.mbdo.mission.extensions.ren_mission_01.multiprocessingstationmissions;

import fr.inria.mbdo.mission.extensions.ren_mission_01.multiprocessingstationsystem.multiprocessingstation.MultiProcessingStationMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.Zone;
import fr.inria.mbdo.mission.runtime.rtc.event.Event;

/**
 * From MultiProcessingStationMissions::MultiProcessingStationNominalMission
 */
public interface MultiProcessingStationNominalMissionActions {
  /**
   * TODO: implement transition action for fr.inria.mbdo.mission.ir.TransitionActionCustomIR:broadcastCompletion
   */
  void broadcastCompletion(Event event, MultiProcessingStationMachine multiProcessingStation,
      Zone zoneMPS);
}
