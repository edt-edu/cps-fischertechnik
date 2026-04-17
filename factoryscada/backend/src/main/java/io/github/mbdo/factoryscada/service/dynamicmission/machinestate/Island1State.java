package io.github.mbdo.factoryscada.service.dynamicmission.machinestate;

import lombok.Getter;

@Getter
public class Island1State {
  private final SortingLineState sortingLine01;

  public Island1State() {
    sortingLine01 = new SortingLineState();
  }
}
