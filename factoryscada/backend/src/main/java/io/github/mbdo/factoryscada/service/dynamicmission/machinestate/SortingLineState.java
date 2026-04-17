package io.github.mbdo.factoryscada.service.dynamicmission.machinestate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SortingLineState extends MachineState {
  private boolean inputLightBarrier = true;
}
