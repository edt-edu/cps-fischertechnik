package io.github.mbdo.factoryscada.frontend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.domains.conveyorbelt.ConveyorBeltMachine;
import io.github.mbdo.factoryscada.service.FactoryScada;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@MessageMapping("/conveyorBelt")
public class ConveyorBeltController extends AbstractMachineController<ConveyorBeltMachine> {

    @Autowired
    public ConveyorBeltController(FactoryScada factoryScada) {
        super(factoryScada);
    }

    
    @MessageMapping("/{machineName}/command/move_to_sensor")
    public String receiveMoveToSensorCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<ConveyorBeltMachine> moveToLightBarrierDTO
    ) {
        return executeCommand(machineName, "moveToSensor", moveToLightBarrierDTO);
    }

    @MessageMapping("/{machineName}/command/move_nb_steps")
    public String receiveMoveNbStepsCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<ConveyorBeltMachine> moveStepsDTO
    ) {
        return executeCommand(machineName, "moveNbSteps", moveStepsDTO);
    }

    @MessageMapping("/{machineName}/command/move_out")
    public String receiveMoveCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<ConveyorBeltMachine> moveOutDTO
    ) {
        return executeCommand(machineName, "moveOut", moveOutDTO);
    }

    @MessageMapping("/{machineName}/command/stop")
    public String receiveStopCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<ConveyorBeltMachine> stopDTO
    ) {
        return executeCommand(machineName, "stop", stopDTO);
    }
    
}
