package io.github.mbdo.factoryscada.domains.indexedline;

import io.github.mbdo.factoryscada.core.AbstractMachine;
import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.domains.indexedline.commands.Process1Command;
import io.github.mbdo.factoryscada.domains.indexedline.commands.StopCommand;
import io.github.mbdo.factoryscada.socket.Protocol;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IndexedLineMachine extends AbstractMachine {
  public IndexedLineMachine(String name, Protocol protocol) {
    super(name, protocol);
  }

  public static String getType() {
    return "indexedLine";
  }

  public void process1(@Valid @NotNull final GenericMachineCommandDTO<IndexedLineMachine> process1DTO) {
    log.info("Process 1 indexedLine {}", process1DTO);
    new Process1Command(this, process1DTO).execute();
  }

  public void stop(@Valid @NotNull final GenericMachineCommandDTO<IndexedLineMachine> stopDTO) {
    log.info("Stop indexedLine {}", stopDTO);
    new StopCommand(this, stopDTO).execute();
  }
}
