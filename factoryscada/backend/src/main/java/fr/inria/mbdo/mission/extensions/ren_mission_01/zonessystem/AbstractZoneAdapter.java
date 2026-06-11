package fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem;

import fr.inria.mbdo.mission.runtime.api.AbstractAdapter;
import java.lang.Override;
import java.lang.String;

/**
 * From ZonesSystem::Zone
 */
public abstract class AbstractZoneAdapter extends AbstractAdapter implements Zone {
  protected volatile boolean isOccupied;

  protected AbstractZoneAdapter(String id) {
    super(id);
  }

  @Override
  public boolean getIsOccupied() {
    return this.isOccupied;
  }

  @Override
  public void setIsOccupied(boolean isOccupied) {
    this.isOccupied = isOccupied;
  }
}
