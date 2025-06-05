package io.github.mbdo.factoryscada.domains.indexedline.commands;

import io.github.mbdo.factoryscada.core.AbstractCommand;
import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.domains.indexedline.IndexedLineMachine;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StopCommand extends AbstractCommand<IndexedLineMachine> {
  public StopCommand(IndexedLineMachine machine, GenericMachineCommandDTO<IndexedLineMachine> stopDTO) {
    super(machine, stopDTO);
  }

  @Override
  public GenericMachineCommandDTO<IndexedLineMachine> buildDTO() {
    // TODO Auto-generated method stub
    return null;
  }
}
