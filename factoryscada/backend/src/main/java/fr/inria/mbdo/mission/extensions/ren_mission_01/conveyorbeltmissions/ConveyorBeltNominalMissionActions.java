package fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltmissions;

import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltsystem.conveyorbelt.ConveyorBeltMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgripper.VacuumGripperMachine;
import fr.inria.mbdo.mission.runtime.rtc.event.Event;

/**
 * From ConveyorBeltMissions::ConveyorBeltNominalMission
 */
public interface ConveyorBeltNominalMissionActions {
  /**
   * TODO: implement transition action for fr.inria.mbdo.mission.ir.TransitionActionCustomIR:PerformActionUsage
   */
  void performActionUsage(Event event, ConveyorBeltMachine conveyorBelt,
      VacuumGripperMachine vacuumGripper1, VacuumGripperMachine vacuumGripper2);
}
