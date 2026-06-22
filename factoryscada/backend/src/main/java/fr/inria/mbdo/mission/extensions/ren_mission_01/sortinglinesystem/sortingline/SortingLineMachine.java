package fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinesystem.sortingline;

import fr.inria.mbdo.mission.runtime.api.MachineAdapter;

/**
 * From SortingLineSystem::SortingLine::SortingLineMachine
 */
public interface SortingLineMachine extends MachineAdapter {
  boolean getSensor_SL_in();

  void setSensor_SL_in(boolean sensor_SL_in);

  boolean getSensor_SL_blue();

  void setSensor_SL_blue(boolean sensor_SL_blue);

  boolean getSensor_SL_white();

  void setSensor_SL_white(boolean sensor_SL_white);

  boolean getSensor_SL_red();

  void setSensor_SL_red(boolean sensor_SL_red);

  /**
   * From SortingLineSystem::SortingLine::SortingLineMachine::eject
   */
  void eject();

  /**
   * From SortingLineSystem::SortingLine::SortingLineMachine::stop
   */
  void stop();
}
