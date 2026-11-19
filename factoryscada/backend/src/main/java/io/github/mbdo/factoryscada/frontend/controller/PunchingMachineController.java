package io.github.mbdo.factoryscada.frontend.controller;

import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.domains.punchingmachine.PunchingMachine;
import io.github.mbdo.factoryscada.frontend.WebSocketDoc;
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
@MessageMapping("/punchingMachine")
public class PunchingMachineController extends AbstractMachineController<PunchingMachine> {

  @Autowired
  public PunchingMachineController(FactoryScada factoryScada) {
    super(factoryScada);
  }

  @MessageMapping("/{machineName}/command/punch")
  @WebSocketDoc (description = "Sends a punch command to the specified punching machine.",
          indirectOutputs = {"/topic/{machineName}/command-status"})
  public String receivePunchCommand(
      @DestinationVariable("machineName") String machineName,
      @Valid @Payload GenericMachineCommandDTO<PunchingMachine> punchDTO
  ) {
    return executeCommand(machineName, "punch", punchDTO);
  }

  @MessageMapping("/{machineName}/command/stop")
  @WebSocketDoc (description = "Sends a stop command to the specified punching machine.",
          indirectOutputs = {"/topic/{machineName}/command-status"})
  public String receiveStopCommand(
          @DestinationVariable("machineName") String machineName,
          @Valid @Payload GenericMachineCommandDTO<PunchingMachine> stopDTO
  ) {
    return executeCommand(machineName, "stop", stopDTO);
  }
}
