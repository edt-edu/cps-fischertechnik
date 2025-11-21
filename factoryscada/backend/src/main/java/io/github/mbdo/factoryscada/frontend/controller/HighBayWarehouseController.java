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

    @MessageMapping("/{machineName}/command/cantilever_backward")
    public String executeCantileverBackwardCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<HighBayWarehouseMachine> cantilever_backwardDTO) {
        log.info("Received request on /{}/command/cantilever_backward", machineName);
        return executeCommand(machineName, "cantilever_backward", cantilever_backwardDTO);
    }

    @MessageMapping("/{machineName}/command/cantilever_forward")
    public String executeCantileverFrontendCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<HighBayWarehouseMachine> cantilever_forwardDTO) {
        log.info("Received request on /{}/command/cantilever_forward", machineName);
        return executeCommand(machineName, "cantilever_forward", cantilever_forwardDTO);
    }

    @MessageMapping("/{machineName}/command/conveyor_backward")
    public String executeConveyorBackwardCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<HighBayWarehouseMachine> conveyor_backwardDTO) {
        log.info("Received request on /{}/command/conveyor_backward", machineName);
        return executeCommand(machineName, "conveyor_backward", conveyor_backwardDTO);
    }

    @MessageMapping("/{machineName}/command/conveyor_forward")
    public String executeConveyorFrontendCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<HighBayWarehouseMachine> conveyor_forwardDTO) {
        log.info("Received request on /{}/command/conveyor_forward", machineName);
        return executeCommand(machineName, "conveyor_forward", conveyor_forwardDTO);
    }

    @MessageMapping("/{machineName}/command/conveyor_stop")
    public String executeConveyorStopCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<HighBayWarehouseMachine> conveyor_stopDTO) {
        log.info("Received request on /{}/command/conveyor_stop", machineName);
        return executeCommand(machineName, "conveyor_stop", conveyor_stopDTO);
    }

    @MessageMapping("/{machineName}/command/go_to_column")
    public String executeGoToColumnCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<HighBayWarehouseMachine> go_to_columnDTO) {
        log.info("Received request on /{}/command/go_to_column", machineName);
        return executeCommand(machineName, "go_to_column", go_to_columnDTO);
    }

    @MessageMapping("/{machineName}/command/go_to_row")
    public String executeGoToRowCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<HighBayWarehouseMachine> go_to_rowDTO) {
        log.info("Received request on /{}/command/go_to_row", machineName);
        return executeCommand(machineName, "go_to_row", go_to_rowDTO);
    }

    @MessageMapping("/{machineName}/command/horizontal_to")
    public String executeHorizontalToCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<HighBayWarehouseMachine> horizontal_toDTO) {
        log.info("Received request on /{}/command/horizontal_to", machineName);
        return executeCommand(machineName, "horizontal_to", horizontal_toDTO);
    }

    @MessageMapping("/{machineName}/command/pickup_from")
    public String executeRetrieveCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<HighBayWarehouseMachine> pickup_fromDTO) {
        log.info("Received request on /{}/command/pickup_from", machineName);
        return executeCommand(machineName, "pickup_from", pickup_fromDTO);
    }

    @MessageMapping("/{machineName}/command/setup")
    public String executeSetupCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<HighBayWarehouseMachine> setupDTO) {
        log.info("Received request on /{}/command/setup", machineName);
        return executeCommand(machineName, "setup", setupDTO);
    }

    @MessageMapping("/{machineName}/command/stop")
    public String executeStopCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<HighBayWarehouseMachine> stopDTO) {
        log.info("Received request on /{}/command/stop", machineName);
        return executeCommand(machineName, "stop", stopDTO);
    }

    @MessageMapping("/{machineName}/command/store")
    public String executeStoreCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<HighBayWarehouseMachine> storeDTO) {
        log.info("Received request on /{}/command/store", machineName);
        return executeCommand(machineName, "store", storeDTO);
    }

    @MessageMapping("/{machineName}/command/vertical_to")
    public String executeVerticalToCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<HighBayWarehouseMachine> vertical_toDTO) {
        log.info("Received request on /{}/command/vertical_to", machineName);
        return executeCommand(machineName, "vertical_to", vertical_toDTO);
    }

    @MessageMapping("/{machineName}/command/move_to_safe_position")
    public String executeMoveToSafePositionCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<HighBayWarehouseMachine> move_to_safe_positionDTO) {
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
