package io.github.mbdo.factoryscada.frontend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.domains.sortingline.SortingLineMachine;
import io.github.mbdo.factoryscada.service.FactoryScada;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@MessageMapping("/sortingLine")
public class SortingLineController extends AbstractMachineController<SortingLineMachine> {

    @Autowired
    public SortingLineController(FactoryScada factoryScada) {
        super(factoryScada);
    }

    @MessageMapping("/{machineName}/command/eject")
    public String executeEjectCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<SortingLineMachine> ejectDTO
    ) {
        return executeCommand(machineName, "eject", ejectDTO);
    }
    
    @MessageMapping("/{machineName}/command/stop")
    public String receiveStopCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<SortingLineMachine> stopDTO
    ) {
        return executeCommand(machineName, "stop", stopDTO);
    }
}
