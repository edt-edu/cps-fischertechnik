package io.github.mbdo.factoryscada.service.dynamicmission.machinestate;

import io.github.mbdo.factoryscada.service.FactoryScada;
import lombok.Getter;

@Getter
public class Island1State {
  private final SortingLineState sortingLine01;


  public Island1State(FactoryScada factoryScada) {
    sortingLine01 = new SortingLineState(factoryScada, "I1SortingLine01");
  }
}
