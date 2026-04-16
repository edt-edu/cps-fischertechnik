package io.github.mbdo.factoryscada.service.dynamicmission.machinestate;

import io.github.mbdo.factoryscada.service.FactoryScada;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SortingLineState extends MachineState {
  private boolean inputLightBarrier = false;

  public SortingLineState(FactoryScada factoryScada, String machineName) {
    super(factoryScada, machineName);
  }
}
