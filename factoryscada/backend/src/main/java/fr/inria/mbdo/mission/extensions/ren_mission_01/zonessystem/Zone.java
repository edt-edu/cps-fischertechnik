package fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem;

import fr.inria.mbdo.mission.runtime.api.MachineAdapter;

/**
 * From ZonesSystem::Zone
 */
public interface Zone extends MachineAdapter {
  boolean getIsOccupied();

  void setIsOccupied(boolean isOccupied);
}
