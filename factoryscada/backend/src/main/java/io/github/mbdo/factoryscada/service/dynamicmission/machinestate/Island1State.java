package io.github.mbdo.factoryscada.service.dynamicmission.machinestate;

import lombok.Getter;

@Getter
public class Island1State {
  private final SortingLineState sortingLine01 = new SortingLineState();
  private final ConveyorBeltState conveyorBelt01 = new ConveyorBeltState();
}
