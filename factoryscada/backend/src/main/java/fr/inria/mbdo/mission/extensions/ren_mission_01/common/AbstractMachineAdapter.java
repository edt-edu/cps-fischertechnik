package fr.inria.mbdo.mission.extensions.ren_mission_01.common;

import fr.inria.mbdo.mission.runtime.api.AbstractAdapter;
import java.lang.String;

/**
 * From Common::Machine
 */
public abstract class AbstractMachineAdapter extends AbstractAdapter implements Machine {
  protected AbstractMachineAdapter(String id) {
    super(id);
  }
}
