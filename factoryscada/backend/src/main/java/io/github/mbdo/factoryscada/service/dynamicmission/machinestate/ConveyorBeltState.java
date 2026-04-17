package io.github.mbdo.factoryscada.service.dynamicmission.machinestate;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ConveyorBeltState extends MachineState {
  private boolean feedLightBarrier = true;
  private boolean swapLightBarrier = true;
}
