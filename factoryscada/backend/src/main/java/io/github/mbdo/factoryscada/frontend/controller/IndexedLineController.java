package io.github.mbdo.factoryscada.frontend.controller;

import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.domains.indexedline.IndexedLineMachine;
import io.github.mbdo.factoryscada.service.FactoryScada;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@MessageMapping("/indexedLine")
public class IndexedLineController extends AbstractMachineController<IndexedLineMachine> {

  @Autowired
  public IndexedLineController(FactoryScada factoryScada) {
    super(factoryScada);
  }

  @MessageMapping("/{machineName}/command/process1")
  public String receiveMoveCommand(
      @DestinationVariable("machineName") String machineName,
      @Valid @Payload GenericMachineCommandDTO<IndexedLineMachine> moveOutDTO
  ) {
    return executeCommand(machineName, "process1", moveOutDTO);
  }

  @MessageMapping("/{machineName}/command/stop")
  public String receiveStopCommand(
      @DestinationVariable("machineName") String machineName,
      @Valid @Payload GenericMachineCommandDTO<IndexedLineMachine> stopDTO
  ) {
    return executeCommand(machineName, "stop", stopDTO);
  }
}
