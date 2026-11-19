package io.github.mbdo.factoryscada.frontend.controller;

import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.domains.indexedline.IndexedLineMachine;
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
@MessageMapping("/indexedLine")
public class IndexedLineController extends AbstractMachineController<IndexedLineMachine> {

  @Autowired
  public IndexedLineController(FactoryScada factoryScada) {
    super(factoryScada);
  }

  @MessageMapping("/{machineName}/command/move_to_mill")
  @WebSocketDoc (description = "Moves the workpiece to the mill position.",
              indirectOutputs = { "/topic/{machineName}/command-status", "/topic/{machineName}/machine-status" })
  public String receiveMoveToMillCommand(@DestinationVariable("machineName") String machineName,
                                         @Valid @Payload GenericMachineCommandDTO<IndexedLineMachine> moveToMillDTO) {
    return executeCommand(machineName, "move_to_mill", moveToMillDTO);
  }

  @MessageMapping("/{machineName}/command/mill")
  @WebSocketDoc (description = "Commands the indexed line machine to perform the milling operation.",
              indirectOutputs = { "/topic/{machineName}/command-status", "/topic/{machineName}/machine-status" })
  public String receiveMillCommand(@DestinationVariable("machineName") String machineName,
                                   @Valid @Payload GenericMachineCommandDTO<IndexedLineMachine> millDTO) {
    return executeCommand(machineName, "mill", millDTO);
  }

  @MessageMapping("/{machineName}/command/drill")
  @WebSocketDoc (description = "Commands the indexed line machine to perform the drilling operation.",
              indirectOutputs = { "/topic/{machineName}/command-status", "/topic/{machineName}/machine-status" })
  public String receiveDrillCommand(@DestinationVariable("machineName") String machineName,
                                    @Valid @Payload GenericMachineCommandDTO<IndexedLineMachine> drillDTO) {
    return executeCommand(machineName, "drill", drillDTO);
  }

  @MessageMapping("/{machineName}/command/move_to_drill")
  @WebSocketDoc (description = "Moves the workpiece to the drill position.",
              indirectOutputs = { "/topic/{machineName}/command-status", "/topic/{machineName}/machine-status" })
  public String receiveMoveToDrillCommand(@DestinationVariable("machineName") String machineName,
                                          @Valid @Payload GenericMachineCommandDTO<IndexedLineMachine> moveToDrillDTO) {
    return executeCommand(machineName, "move_to_drill", moveToDrillDTO);
  }

  @MessageMapping("/{machineName}/command/move_to_output")
  @WebSocketDoc (description = "Moves the workpiece to the output position.",
              indirectOutputs = { "/topic/{machineName}/command-status", "/topic/{machineName}/machine-status" })
  public String receiveMoveToOutputCommand(@DestinationVariable("machineName") String machineName,
                                           @Valid @Payload
                                           GenericMachineCommandDTO<IndexedLineMachine> moveToOutputDTO) {
    return executeCommand(machineName, "move_to_output", moveToOutputDTO);
  }

  @MessageMapping("/{machineName}/command/process")
  @WebSocketDoc (description = "Move a workpiece through all stations of the IndexedLine and process it for the specified timespans.",
              indirectOutputs = { "/topic/{machineName}/command-status", "/topic/{machineName}/machine-status" })
  public String receiveProcessCommand(@DestinationVariable("machineName") String machineName, @Valid @Payload GenericMachineCommandDTO<IndexedLineMachine> processDTO) {
    return executeCommand(machineName, "process", processDTO);
  }

  @MessageMapping("/{machineName}/command/process1")
  @WebSocketDoc (description = "Wait for a workpiece, mill it for 2 seconds, drill it for 2 seconds and move it to the output.",
              indirectOutputs = { "/topic/{machineName}/command-status", "/topic/{machineName}/machine-status" })
  public String receiveProcess1Command(@DestinationVariable("machineName") String machineName,
                                       @Valid @Payload GenericMachineCommandDTO<IndexedLineMachine> process1DTO) {
    return executeCommand(machineName, "process1", process1DTO);
  }

  @MessageMapping("/{machineName}/command/stop")
  @WebSocketDoc (description = "Stops the indexed line machine.",
              indirectOutputs = { "/topic/{machineName}/command-status", "/topic/{machineName}/machine-status" })
  public String receiveStopCommand(@DestinationVariable("machineName") String machineName,
                                   @Valid @Payload GenericMachineCommandDTO<IndexedLineMachine> stopDTO) {
    return executeCommand(machineName, "stop", stopDTO);
  }
}
