package io.github.mbdo.factoryscada.frontend.controller;

import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.domains.highbaywarehouse.HighBayWarehouseMachine;
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
@MessageMapping("/highBayWarehouse")
public class HighBayWarehouseController extends AbstractMachineController<HighBayWarehouseMachine> {

    @Autowired
    public HighBayWarehouseController(FactoryScada factoryScada) {
        super(factoryScada);
    }

    @MessageMapping("/{machineName}/command/cantilever_backward")
    @WebSocketDoc (description = "Executes the cantilever_backward command on the specified HighBayWarehouse machine. Retracts the cantilever, moving it away from the conveyor or the storage racks.",
                   indirectOutputs = { "/topic/{machineName}/command-status", "/topic/{machineName}/machine-status" })
    public String executeCantileverBackwardCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<HighBayWarehouseMachine> cantilever_backwardDTO) {
        log.info("Received request on /{}/command/cantilever_backward", machineName);
        return executeCommand(machineName, "cantilever_backward", cantilever_backwardDTO);
    }

    @MessageMapping("/{machineName}/command/cantilever_forward")
    @WebSocketDoc (description = "Executes the cantilever_forward command on the specified HighBayWarehouse machine. Extends the cantilever towards the conveyor or the storage racks.",
                   indirectOutputs = { "/topic/{machineName}/command-status", "/topic/{machineName}/machine-status" })
    public String executeCantileverFrontendCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<HighBayWarehouseMachine> cantilever_forwardDTO) {
        log.info("Received request on /{}/command/cantilever_forward", machineName);
        return executeCommand(machineName, "cantilever_forward", cantilever_forwardDTO);
    }

    @MessageMapping("/{machineName}/command/conveyor_backward")
    @WebSocketDoc (description = "Executes the conveyor_backward command on the specified HighBayWarehouse machine. Moves the conveyor backward, towards the crane.",
                   indirectOutputs = { "/topic/{machineName}/command-status", "/topic/{machineName}/machine-status" })
    public String executeConveyorBackwardCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<HighBayWarehouseMachine> conveyor_backwardDTO) {
        log.info("Received request on /{}/command/conveyor_backward", machineName);
        return executeCommand(machineName, "conveyor_backward", conveyor_backwardDTO);
    }

    @MessageMapping("/{machineName}/command/conveyor_forward")
    @WebSocketDoc (description = "Executes the conveyor_forward command on the specified HighBayWarehouse machine. Moves the conveyor belt forward, towards the exit.",
                   indirectOutputs = { "/topic/{machineName}/command-status", "/topic/{machineName}/machine-status" })
    public String executeConveyorFrontendCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<HighBayWarehouseMachine> conveyor_forwardDTO) {
        log.info("Received request on /{}/command/conveyor_forward", machineName);
        return executeCommand(machineName, "conveyor_forward", conveyor_forwardDTO);
    }

    @MessageMapping("/{machineName}/command/conveyor_stop")
    @WebSocketDoc (description = "Executes the conveyor_stop command on the specified HighBayWarehouse machine.",
                   indirectOutputs = { "/topic/{machineName}/command-status", "/topic/{machineName}/machine-status" })
    public String executeConveyorStopCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<HighBayWarehouseMachine> conveyor_stopDTO) {
        log.info("Received request on /{}/command/conveyor_stop", machineName);
        return executeCommand(machineName, "conveyor_stop", conveyor_stopDTO);
    }

    @MessageMapping("/{machineName}/command/go_to_column")
    @WebSocketDoc (description = "Executes the crane_goto_column command on the specified HighBayWarehouse machine. Moves the crane horizontally to the specified column.",
                   indirectOutputs = { "/topic/{machineName}/command-status", "/topic/{machineName}/machine-status" })
    public String executeGoToColumnCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<HighBayWarehouseMachine> go_to_columnDTO) {
        log.info("Received request on /{}/command/go_to_column", machineName);
        return executeCommand(machineName, "crane_goto_column", go_to_columnDTO);
    }

    @MessageMapping("/{machineName}/command/go_to_row")
    @WebSocketDoc (description = "Executes the crane_goto_row command on the specified HighBayWarehouse machine. Move the crane vertically to the specified row.",
                   indirectOutputs = { "/topic/{machineName}/command-status", "/topic/{machineName}/machine-status" })
    public String executeGoToRowCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<HighBayWarehouseMachine> go_to_rowDTO) {
        log.info("Received request on /{}/command/go_to_row", machineName);
        return executeCommand(machineName, "crane_goto_row", go_to_rowDTO);
    }

    @MessageMapping("/{machineName}/command/horizontal_to")
    @WebSocketDoc (description = "Executes the crane_goto_horizontal_position command on the specified HighBayWarehouse machine. Move the crane vertically to a specified counter value.",
                   indirectOutputs = { "/topic/{machineName}/command-status", "/topic/{machineName}/machine-status" })
    public String executeHorizontalToCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<HighBayWarehouseMachine> horizontal_toDTO) {
        log.info("Received request on /{}/command/horizontal_to", machineName);
        return executeCommand(machineName, "crane_goto_horizontal_position", horizontal_toDTO);
    }

    @MessageMapping("/{machineName}/command/pickup_from")
    @WebSocketDoc (description = "Executes the pickup_from command on the specified HighBayWarehouse machine. Move a payload from the specified rack position onto the conveyor exit. A setupis performed after the command to reset the encoder and recalibrate the counters.",
                   indirectOutputs = { "/topic/{machineName}/command-status", "/topic/{machineName}/machine-status" })
    public String executePickupFromCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<HighBayWarehouseMachine> pickup_fromDTO) {
        log.info("Received request on /{}/command/pickup_from", machineName);
        return executeCommand(machineName, "pickup_from", pickup_fromDTO);
    }

    @MessageMapping("/{machineName}/command/setup")
    @WebSocketDoc (description = "Executes the setup command on the specified HighBayWarehouse machine. Set up the HighBay and calibrate the counters by reaching the reference switches.",
                   indirectOutputs = { "/topic/{machineName}/command-status", "/topic/{machineName}/machine-status" })
    public String executeSetupCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<HighBayWarehouseMachine> setupDTO) {
        log.info("Received request on /{}/command/setup", machineName);
        return executeCommand(machineName, "setup", setupDTO);
    }

    @MessageMapping("/{machineName}/command/stop")
    @WebSocketDoc (description = "Executes the stop command on the specified HighBayWarehouse machine.",
                   indirectOutputs = { "/topic/{machineName}/command-status", "/topic/{machineName}/machine-status" })
    public String executeStopCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<HighBayWarehouseMachine> stopDTO) {
        log.info("Received request on /{}/command/stop", machineName);
        return executeCommand(machineName, "stop", stopDTO);
    }

    @MessageMapping("/{machineName}/command/store_to")
    @WebSocketDoc (description = "Executes the store_to command on the specified HighBayWarehouse machine. Stores an item from the conveyor to the specified row and column in the rack (ie. place the cantilevel in front of the conveyor, activate conveyor to load the payload on the cantilever, and move to place the payload in the rack), a reset of the encoder is performed after the operation.",
                   indirectOutputs = { "/topic/{machineName}/command-status", "/topic/{machineName}/machine-status" })
    public String executeStoreToCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<HighBayWarehouseMachine> dto) {
        log.info("Received request on /{}/command/store_to", machineName);
        return executeCommand(machineName, "store_to", dto);
    }

    @MessageMapping("/{machineName}/command/vertical_to")
    @WebSocketDoc (description = "Executes the crane_goto_vertical_position command on the specified HighBayWarehouse machine. Move the crane vertically to a specified counter value.",
                   indirectOutputs = { "/topic/{machineName}/command-status", "/topic/{machineName}/machine-status" })
    public String executeVerticalToCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<HighBayWarehouseMachine> vertical_toDTO) {
        log.info("Received request on /{}/command/vertical_to", machineName);
        return executeCommand(machineName, "crane_goto_vertical_position", vertical_toDTO);
    }

    @MessageMapping("/{machineName}/command/move_to_safe_position")
    @WebSocketDoc (description = "Executes the move_to_safe_position command on the specified HighBayWarehouse machine. Move towards a configured safe-position",
                   indirectOutputs = { "/topic/{machineName}/command-status", "/topic/{machineName}/machine-status" })
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
