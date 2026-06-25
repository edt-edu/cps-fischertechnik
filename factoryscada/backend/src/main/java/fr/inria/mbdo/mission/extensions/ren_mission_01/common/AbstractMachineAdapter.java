package fr.inria.mbdo.mission.extensions.ren_mission_01.common;

import java.lang.String;

/**
 * From Common::Machine
 */
public abstract class AbstractMachineAdapter extends fr.inria.mbdo.mission.runtime.api.AbstractMachineAdapter implements Machine {
  protected AbstractMachineAdapter(String id) {
    super(id);
  }
}
