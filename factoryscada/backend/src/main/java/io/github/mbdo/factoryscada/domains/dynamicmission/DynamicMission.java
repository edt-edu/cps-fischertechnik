package io.github.mbdo.factoryscada.domains.dynamicmission;

import java.util.Collection;

public interface DynamicMission {
  String getName();
  String getDescription();

  Collection<String> getInvolvedMachineNames();

  void start();
  void stop();
  boolean isActive();
}
