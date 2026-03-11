package io.github.mbdo.factoryscada.domains.indexedline.commands;

import io.github.mbdo.factoryscada.core.AbstractCommand;
import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.domains.indexedline.IndexedLineMachine;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MoveToDrillCommand extends AbstractCommand<IndexedLineMachine> {
  public MoveToDrillCommand(IndexedLineMachine machine, GenericMachineCommandDTO<IndexedLineMachine> abstractDTO) {
    super(machine, abstractDTO);
  }

  @Override
  public GenericMachineCommandDTO<IndexedLineMachine> buildDTO() {
    return null;
  }
}
