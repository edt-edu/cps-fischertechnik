package io.github.mbdo.factoryscada.service.dynamicmission.machinestate;

import io.github.mbdo.factoryscada.service.FactoryScada;

public abstract class MachineState {
  private final FactoryScada factoryScada;
  private final String machineName;

  public MachineState(FactoryScada factoryScada, String machineName) {
    this.factoryScada = factoryScada;
    this.machineName = machineName;
  }

  //TODO can that be determined via mqtt?
  public boolean isIdle() {
    var status = factoryScada.getMachineLastMachineStatusMap().get(machineName);
    if (status == null) return true;

    return status.getMachineFeedbackStatus().contains("IDLE");
  }
}
