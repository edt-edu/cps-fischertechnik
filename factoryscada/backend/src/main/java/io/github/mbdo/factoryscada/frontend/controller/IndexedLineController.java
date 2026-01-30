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

  @MessageMapping("/{machineName}/command/move_to_mill")
  public String receiveMoveToMillCommand(@DestinationVariable("machineName") String machineName,
                                         @Valid @Payload GenericMachineCommandDTO<IndexedLineMachine> moveToMillDTO) {
    return executeCommand(machineName, "move_to_mill", moveToMillDTO);
  }

  @MessageMapping("/{machineName}/command/mill")
  public String receiveMillCommand(@DestinationVariable("machineName") String machineName,
                                   @Valid @Payload GenericMachineCommandDTO<IndexedLineMachine> millDTO) {
    return executeCommand(machineName, "mill", millDTO);
  }

  @MessageMapping("/{machineName}/command/drill")
  public String receiveDrillCommand(@DestinationVariable("machineName") String machineName,
                                    @Valid @Payload GenericMachineCommandDTO<IndexedLineMachine> drillDTO) {
    return executeCommand(machineName, "drill", drillDTO);
  }

  @MessageMapping("/{machineName}/command/move_to_drill")
  public String receiveMoveToDrillCommand(@DestinationVariable("machineName") String machineName,
                                          @Valid @Payload GenericMachineCommandDTO<IndexedLineMachine> moveToDrillDTO) {
    return executeCommand(machineName, "move_to_drill", moveToDrillDTO);
  }

  @MessageMapping("/{machineName}/command/move_to_output")
  public String receiveMoveToOutputCommand(@DestinationVariable("machineName") String machineName,
                                           @Valid @Payload
                                           GenericMachineCommandDTO<IndexedLineMachine> moveToOutputDTO) {
    return executeCommand(machineName, "move_to_output", moveToOutputDTO);
  }

  @MessageMapping("/{machineName}/command/process1")
  public String receiveProcess1Command(@DestinationVariable("machineName") String machineName,
                                       @Valid @Payload GenericMachineCommandDTO<IndexedLineMachine> process1DTO) {
    return executeCommand(machineName, "process1", process1DTO);
  }

  @MessageMapping("/{machineName}/command/stop")
  public String receiveStopCommand(@DestinationVariable("machineName") String machineName,
                                   @Valid @Payload GenericMachineCommandDTO<IndexedLineMachine> stopDTO) {
    return executeCommand(machineName, "stop", stopDTO);
  }
}
