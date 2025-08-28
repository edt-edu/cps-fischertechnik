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

    @MessageMapping("/{machineName}/command/process")
    public String executeProcessCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<MultiProcessingStationMachine> setupDTO) {
        log.info("Received request on /{}/command/process", machineName);
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

    @MessageMapping("/{machineName}/command/oven_load")
    public String executeOvenLoadCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<MultiProcessingStationMachine> DTO) {
        log.info("Received request on /{}/command/oven_load", machineName);
        return executeCommand(machineName, "oven_load", DTO);
    }

    @MessageMapping("/{machineName}/command/oven_unload")
    public String executeOvenUnloadCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<MultiProcessingStationMachine> DTO) {
        log.info("Received request on /{}/command/oven_unload", machineName);
        return executeCommand(machineName, "oven_unload", DTO);
    }

    @MessageMapping("/{machineName}/command/oven_heat")
    public String executeOvenHeatCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<MultiProcessingStationMachine> DTO) {
        log.info("Received request on /{}/command/oven_heat", machineName);
        return executeCommand(machineName, "oven_heat", DTO);
    }

    @MessageMapping("/{machineName}/command/oven_process")
    public String executeOvenProcessCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<MultiProcessingStationMachine> DTO) {
        log.info("Received request on /{}/command/oven_process", machineName);
        return executeCommand(machineName, "oven_process", DTO);
    }

    @MessageMapping("/{machineName}/command/arm_move")
    public String executeArmMoveCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<MultiProcessingStationMachine> DTO) {
        log.info("Received request on /{}/command/arm_move", machineName);
        return executeCommand(machineName, "arm_move", DTO);
    }

    @MessageMapping("/{machineName}/command/arm_pick")
    public String executeArmPickCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<MultiProcessingStationMachine> DTO) {
        log.info("Received request on /{}/command/arm_pick", machineName);
        return executeCommand(machineName, "arm_pick", DTO);
    }

    @MessageMapping("/{machineName}/command/arm_place")
    public String executeArmPlaceCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<MultiProcessingStationMachine> DTO) {
        log.info("Received request on /{}/command/arm_place", machineName);
        return executeCommand(machineName, "arm_place", DTO);
    }

    @MessageMapping("/{machineName}/command/turntable_rotate")
    public String executeTurntableRotateCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<MultiProcessingStationMachine> DTO) {
        log.info("Received request on /{}/command/turntable_rotate", machineName);
        return executeCommand(machineName, "turntable_rotate", DTO);
    }

    @MessageMapping("/{machineName}/command/turntable_eject")
    public String executeTurntableEjectCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<MultiProcessingStationMachine> DTO) {
        log.info("Received request on /{}/command/turntable_eject", machineName);
        return executeCommand(machineName, "turntable_eject", DTO);
    }

    @MessageMapping("/{machineName}/command/conveyor_move_to_sensor")
    public String executeConveyorMoveToSensorCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<MultiProcessingStationMachine> DTO) {
        log.info("Received request on /{}/command/conveyor_move_to_sensor", machineName);
        return executeCommand(machineName, "conveyor_move_to_sensor", DTO);
    }

    @MessageMapping("/{machineName}/command/conveyor_move_out")
    public String executeConveyorMoveOutCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<MultiProcessingStationMachine> DTO) {
        log.info("Received request on /{}/command/conveyor_move_out", machineName);
        return executeCommand(machineName, "conveyor_move_out", DTO);
    }

    @MessageMapping("/{machineName}/command/saw_cut")
    public String executeSawCutCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<MultiProcessingStationMachine> DTO) {
        log.info("Received request on /{}/command/saw_cut", machineName);
        return executeCommand(machineName, "saw_cut", DTO);
    }
//    @MessageMapping("/{machineName}/command/eject")
//    public String executeBackwardCommand(
//            @DestinationVariable("machineName") String machineName,
//            @Valid @Payload EjectDTO ejectDTO
//    ) {
//        return executeCommand(machineName, "eject", ejectDTO);
//    }
    
}
