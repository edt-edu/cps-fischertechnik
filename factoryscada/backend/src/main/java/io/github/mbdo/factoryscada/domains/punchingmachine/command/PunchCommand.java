package io.github.mbdo.factoryscada.domains.punchingmachine.command;

import io.github.mbdo.factoryscada.core.AbstractCommand;
import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.domains.punchingmachine.PunchingMachine;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PunchCommand extends AbstractCommand<PunchingMachine> {
  public PunchCommand(PunchingMachine machine, GenericMachineCommandDTO<PunchingMachine> punchDTO) {
    super(machine, punchDTO);
  }

  @Override
  public GenericMachineCommandDTO<PunchingMachine> buildDTO() {
    //TODO Auto-generated method stub
    return null;
  }
}
