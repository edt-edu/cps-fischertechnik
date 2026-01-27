package io.github.mbdo.factoryscada.domains.indexedline;

import io.github.mbdo.factoryscada.core.AbstractMachine;
import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.domains.indexedline.commands.*;
import io.github.mbdo.factoryscada.socket.Protocol;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class IndexedLineMachine extends AbstractMachine {
  public IndexedLineMachine(String name, Protocol protocol, List<String> rawCommandNames) {
    super(name, protocol, rawCommandNames);
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

  public void move_to_mill(@Valid @NotNull GenericMachineCommandDTO<IndexedLineMachine> moveToMillDTO) {
    log.info("Move to mill indexedLine {}", moveToMillDTO);
    new MoveToMillCommand(this, moveToMillDTO).execute();
  }

  public void move_to_drill(@Valid @NotNull GenericMachineCommandDTO<IndexedLineMachine> moveToDrillDTO) {
    log.info("Move to drill indexedLine {}", moveToDrillDTO);
    new MoveToDrillCommand(this, moveToDrillDTO).execute();
  }

  public void move_to_output(@Valid @NotNull GenericMachineCommandDTO<IndexedLineMachine> moveToOutputDTO) {
    log.info("Move to output indexedLine {}", moveToOutputDTO);
    new MoveToOutputCommand(this, moveToOutputDTO).execute();
  }
}
