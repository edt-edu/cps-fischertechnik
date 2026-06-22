package fr.inria.mbdo.mission.extensions.ren_mission_01.multiprocessingstationmissions.multiprocessingstationnominalmission;

import fr.inria.mbdo.mission.extensions.ren_mission_01.multiprocessingstationsystem.multiprocessingstation.MultiProcessingStationMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.Zone;
import fr.inria.mbdo.mission.runtime.rtc.event.Event;

/**
 * From MultiProcessingStationMissions::MultiProcessingStationNominalMission
 * Nominal mission for the multi-processing station
 */
public interface MultiProcessingStationNominalMissionActions {
  /**
   * TODO: implement transition action for fr.inria.mbdo.mission.ir.TransitionActionCustomIR:null::broadcastCompletion
   */
  void broadcastCompletion(Event event, MultiProcessingStationMachine multiProcessingStation,
      Zone zoneMPS);
}
