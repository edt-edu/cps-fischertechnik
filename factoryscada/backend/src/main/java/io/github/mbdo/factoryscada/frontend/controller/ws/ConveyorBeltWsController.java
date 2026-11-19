package io.github.mbdo.factoryscada.frontend.controller.ws;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.domains.conveyorbelt.ConveyorBeltMachine;
import io.github.mbdo.factoryscada.frontend.WebSocketDoc;
import io.github.mbdo.factoryscada.service.FactoryScada;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@MessageMapping("/conveyorBelt")
public class ConveyorBeltWsController extends AbstractMachineWsController<ConveyorBeltMachine> {

    @Autowired
    public ConveyorBeltWsController(FactoryScada factoryScada) {
        super(factoryScada);
    }

    
    @MessageMapping("/{machineName}/command/move_to_sensor")
    @WebSocketDoc(description = "Sends a command to the machine controller to move the conveyor belt in a given direction so its payload reaches the next sensor",
                  indirectOutputs = { "/topic/{machineName}/command-status", "/topic/{machineName}/machine-status" })
    public String receiveMoveToSensorCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<ConveyorBeltMachine> moveToLightBarrierDTO
    ) {
        return executeCommand(machineName, "moveToSensor", moveToLightBarrierDTO);
    }

    @MessageMapping("/{machineName}/command/move_nb_steps")
    @WebSocketDoc(description = "Sends a command to the machine controller to move the conveyor belt a specified number of steps",
                  indirectOutputs = { "/topic/{machineName}/command-status", "/topic/{machineName}/machine-status" })
    public String receiveMoveNbStepsCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<ConveyorBeltMachine> moveStepsDTO
    ) {
        return executeCommand(machineName, "moveNbSteps", moveStepsDTO);
    }

    @MessageMapping("/{machineName}/command/move_out")
    @WebSocketDoc(description = "Sends a command to the machine controller to move the conveyor belt in a given direction so its payload is ejected from the conveyor belt",
                  indirectOutputs = { "/topic/{machineName}/command-status", "/topic/{machineName}/machine-status" })
    public String receiveMoveCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<ConveyorBeltMachine> moveOutDTO
    ) {
        return executeCommand(machineName, "moveOut", moveOutDTO);
    }

    @MessageMapping("/{machineName}/command/stop")
    @WebSocketDoc(description = "Sends a command to the machine controller to stop the conveyor belt",
                  indirectOutputs = { "/topic/{machineName}/command-status", "/topic/{machineName}/machine-status" })
    public String receiveStopCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<ConveyorBeltMachine> stopDTO
    ) {
        return executeCommand(machineName, "stop", stopDTO);
    }
    
}
