package io.github.mbdo.factoryscada.frontend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.domains.highbaywarehouse.HighBayWarehouseMachine;
import io.github.mbdo.factoryscada.service.FactoryScada;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.Valid;

@Slf4j
@Controller
@MessageMapping("/highBayWarehouse")
public class HighBayWarehouseController extends AbstractMachineController<HighBayWarehouseMachine> {

    @Autowired
    public HighBayWarehouseController(FactoryScada factoryScada) {
        super(factoryScada);
    }



    @MessageMapping("/{machineName}/command/setup")
    public String executeSetupCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<HighBayWarehouseMachine> setupDTO) {
        log.info("Received request on /{}/command/setup", machineName);
        return executeCommand(machineName, "setup", setupDTO);
    }

    @MessageMapping("/{machineName}/command/process1")
    public String executeProcess1Command(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<HighBayWarehouseMachine> retrieveDTO) {
        log.info("Received request on /{}/command/retrieve", machineName);
        return executeCommand(machineName, "retrieve", retrieveDTO);
    }

    @MessageMapping("/{machineName}/command/stop")
    public String receiveStopCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<HighBayWarehouseMachine> storeDTO) {
        log.info("Received request on /{}/command/store", machineName);
        return executeCommand(machineName, "store", storeDTO);
    }


//    @MessageMapping("/{machineName}/command/eject")
//    public String executeBackwardCommand(
//            @DestinationVariable("machineName") String machineName,
//            @Valid @Payload EjectDTO ejectDTO
//    ) {
//        return executeCommand(machineName, "eject", ejectDTO);
//    }

}
