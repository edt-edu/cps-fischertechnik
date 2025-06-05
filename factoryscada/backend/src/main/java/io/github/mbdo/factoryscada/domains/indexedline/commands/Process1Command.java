package io.github.mbdo.factoryscada.domains.indexedline.commands;

import io.github.mbdo.factoryscada.core.AbstractCommand;
import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.domains.indexedline.IndexedLineMachine;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Process1Command extends AbstractCommand<IndexedLineMachine> {
  public Process1Command(IndexedLineMachine machine, GenericMachineCommandDTO<IndexedLineMachine> abstractDTO) {
    super(machine, abstractDTO);
  }

  @Override
  public GenericMachineCommandDTO<IndexedLineMachine> buildDTO() {
    // TODO Auto-generated method stub
    return null;
  }
}
