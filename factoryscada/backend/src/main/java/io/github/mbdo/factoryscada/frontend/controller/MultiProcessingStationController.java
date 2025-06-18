package io.github.mbdo.factoryscada.frontend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.domains.multiprocessingstation.MultiProcessingStationMachine;
import io.github.mbdo.factoryscada.service.FactoryScada;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.Valid;

@Slf4j
@Controller
@MessageMapping("/multiProcessingStation")
public class MultiProcessingStationController extends AbstractMachineController<MultiProcessingStationMachine> {

    @Autowired
    public MultiProcessingStationController(FactoryScada factoryScada) {
        super(factoryScada);
    }


    @MessageMapping("/{machineName}/command/setup")
    public String executeSetupCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<MultiProcessingStationMachine> setupDTO) {
        log.info("Received request on /{}/command/setup", machineName);
        return executeCommand(machineName, "setup", setupDTO);
    }

    @MessageMapping("/{machineName}/command/process1")
    public String executeProcess1Command(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<MultiProcessingStationMachine> setupDTO) {
        log.info("Received request on /{}/command/process1", machineName);
        return executeCommand(machineName, "setup", setupDTO);
    }

    @MessageMapping("/{machineName}/command/stop")
    public String receiveStopCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<MultiProcessingStationMachine> stopDTO
    ) {
        return executeCommand(machineName, "stop", stopDTO);
    }

    @MessageMapping("/{machineName}/command/move_to_safe_position")
    public String executeMoveToSafePositionCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<MultiProcessingStationMachine> move_to_safe_positionDTO) {
        log.info("Received request on /{}/command/move_to_safe_position", machineName);
        return executeCommand(machineName, "move_to_safe_position", move_to_safe_positionDTO);
    }
//    @MessageMapping("/{machineName}/command/eject")
//    public String executeBackwardCommand(
//            @DestinationVariable("machineName") String machineName,
//            @Valid @Payload EjectDTO ejectDTO
//    ) {
//        return executeCommand(machineName, "eject", ejectDTO);
//    }
    
}
