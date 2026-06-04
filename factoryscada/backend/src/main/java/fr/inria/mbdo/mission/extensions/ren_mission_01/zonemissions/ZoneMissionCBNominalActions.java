package fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions;

import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgripper.VacuumGripperMachine;
import fr.inria.mbdo.mission.runtime.rtc.event.Event;

/**
 * From ZoneMissions::ZoneMissionCBNominal
 */
public interface ZoneMissionCBNominalActions {
  /**
   * TODO: implement transition action for fr.inria.mbdo.mission.ir.TransitionActionCustomIR:sendAcquireResponseEventMessage
   */
  void sendAcquireResponseEventMessage(Event event, VacuumGripperMachine vacuumGripper1,
      VacuumGripperMachine vacuumGripper2);
}
