package io.github.mbdo.factoryscada.domains.punchingmachine.command;

import io.github.mbdo.factoryscada.core.AbstractCommand;
import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.domains.punchingmachine.PunchingMachine;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StopCommand extends AbstractCommand<PunchingMachine> {

  public StopCommand(PunchingMachine machine, GenericMachineCommandDTO<PunchingMachine> stopDTO) {
    super(machine, stopDTO);
  }

  @Override
  public GenericMachineCommandDTO<PunchingMachine> buildDTO() {
    //TODO Auto-generated method stub
    return null;
  }
}
