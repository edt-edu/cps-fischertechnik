package io.github.mbdo.factoryscada.service.dynamicmission.machinestate;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Setter
@Getter
public class MultiProcessingState {
  private boolean inputLightBarrier = true;
}
