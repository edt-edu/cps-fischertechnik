package io.github.mbdo.factoryscada.frontend.controller.ws;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.domains.multiprocessingstation.MultiProcessingStationMachine;
import io.github.mbdo.factoryscada.frontend.WebSocketDoc;
import io.github.mbdo.factoryscada.service.FactoryScada;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.Valid;

@Slf4j
@Controller
@MessageMapping("/multiProcessingStation")
public class MultiProcessingStationWsController extends AbstractMachineWsController<MultiProcessingStationMachine> {

    @Autowired
    public MultiProcessingStationWsController(FactoryScada factoryScada) {
        super(factoryScada);
    }


    @MessageMapping("/{machineName}/command/setup")
    @WebSocketDoc (description = "Sets up the multi-processing station machine for operation.",
                indirectOutputs = { "/topic/{machineName}/command-status", "/topic/{machineName}/machine-status" })
    public String executeSetupCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<MultiProcessingStationMachine> setupDTO) {
        log.info("Received request on /{}/command/setup", machineName);
        return executeCommand(machineName, "setup", setupDTO);
    }

    @MessageMapping("/{machineName}/command/process1")
    @WebSocketDoc (description = "Execute process1, Specification not very clear, preferred command: process_Command  with arguments.",
                indirectOutputs = { "/topic/{machineName}/command-status", "/topic/{machineName}/machine-status" })
    public String executeProcess1Command(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<MultiProcessingStationMachine> setupDTO) {
        log.info("Received request on /{}/command/process1", machineName);
        return executeCommand(machineName, "setup", setupDTO);
    }

    @MessageMapping("/{machineName}/command/process")
    @WebSocketDoc (description = "Execute process according to oven_time (time in oven in sec), saw_time (time in saw in sec), and output the product on the specified output.",
                indirectOutputs = { "/topic/{machineName}/command-status", "/topic/{machineName}/machine-status" })
    public String executeProcessCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<MultiProcessingStationMachine> setupDTO) {
        log.info("Received request on /{}/command/process", machineName);
        return executeCommand(machineName, "setup", setupDTO);
    }

    @MessageMapping("/{machineName}/command/stop")
    @WebSocketDoc (description = "Stops the multi-processing station machine.",
                indirectOutputs = { "/topic/{machineName}/command-status", "/topic/{machineName}/machine-status" })
    public String receiveStopCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<MultiProcessingStationMachine> stopDTO
    ) {
        return executeCommand(machineName, "stop", stopDTO);
    }

    @MessageMapping("/{machineName}/command/move_to_safe_position")
    @WebSocketDoc (description = "Moves the multi-processing station machine to a predifined safe position.",
                indirectOutputs = { "/topic/{machineName}/command-status", "/topic/{machineName}/machine-status" })
    public String executeMoveToSafePositionCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<MultiProcessingStationMachine> move_to_safe_positionDTO) {
        log.info("Received request on /{}/command/move_to_safe_position", machineName);
        return executeCommand(machineName, "move_to_safe_position", move_to_safe_positionDTO);
    }

    @MessageMapping("/{machineName}/command/oven_load")
    @WebSocketDoc (description = "Loads the oven with a workpiece.",
                indirectOutputs = { "/topic/{machineName}/command-status", "/topic/{machineName}/machine-status" })
    public String executeOvenLoadCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<MultiProcessingStationMachine> DTO) {
        log.info("Received request on /{}/command/oven_load", machineName);
        return executeCommand(machineName, "oven_load", DTO);
    }

    @MessageMapping("/{machineName}/command/oven_unload")
    @WebSocketDoc (description = "Unloads the oven with a workpiece.",
                indirectOutputs = { "/topic/{machineName}/command-status", "/topic/{machineName}/machine-status" })
    public String executeOvenUnloadCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<MultiProcessingStationMachine> DTO) {
        log.info("Received request on /{}/command/oven_unload", machineName);
        return executeCommand(machineName, "oven_unload", DTO);
    }

    @MessageMapping("/{machineName}/command/oven_heat")
    @WebSocketDoc (description = "Heats the oven for a given time.",
                indirectOutputs = { "/topic/{machineName}/command-status", "/topic/{machineName}/machine-status" })
    public String executeOvenHeatCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<MultiProcessingStationMachine> DTO) {
        log.info("Received request on /{}/command/oven_heat", machineName);
        return executeCommand(machineName, "oven_heat", DTO);
    }

    @MessageMapping("/{machineName}/command/oven_process")
    @WebSocketDoc (description = "Processes a workpiece in the oven. Heating  time is specified in the command arguments.",
                indirectOutputs = { "/topic/{machineName}/command-status", "/topic/{machineName}/machine-status" })
    public String executeOvenProcessCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<MultiProcessingStationMachine> DTO) {
        log.info("Received request on /{}/command/oven_process", machineName);
        return executeCommand(machineName, "oven_process", DTO);
    }

    @MessageMapping("/{machineName}/command/arm_move")
    @WebSocketDoc (description = "Moves the robotic arm to a specified position (OVEN or TURNTABLE).",
                indirectOutputs = { "/topic/{machineName}/command-status", "/topic/{machineName}/machine-status" })
    public String executeArmMoveCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<MultiProcessingStationMachine> DTO) {
        log.info("Received request on /{}/command/arm_move", machineName);
        return executeCommand(machineName, "arm_move", DTO);
    }

    @MessageMapping("/{machineName}/command/arm_pick")
    @WebSocketDoc (description = "Commands the robotic arm to lower itself, grab the workpiece, and lift it.",
                indirectOutputs = { "/topic/{machineName}/command-status", "/topic/{machineName}/machine-status" })
    public String executeArmPickCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<MultiProcessingStationMachine> DTO) {
        log.info("Received request on /{}/command/arm_pick", machineName);
        return executeCommand(machineName, "arm_pick", DTO);
    }

    @MessageMapping("/{machineName}/command/arm_place")
    @WebSocketDoc (description = "Commands the robotic arm to place down the workpiece.",
                indirectOutputs = { "/topic/{machineName}/command-status", "/topic/{machineName}/machine-status" })
    public String executeArmPlaceCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<MultiProcessingStationMachine> DTO) {
        log.info("Received request on /{}/command/arm_place", machineName);
        return executeCommand(machineName, "arm_place", DTO);
    }

    @MessageMapping("/{machineName}/command/turntable_rotate")
    @WebSocketDoc (description = "Rotates the turntable to a specified position.",
                indirectOutputs = { "/topic/{machineName}/command-status", "/topic/{machineName}/machine-status" })
    public String executeTurntableRotateCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<MultiProcessingStationMachine> DTO) {
        log.info("Received request on /{}/command/turntable_rotate", machineName);
        return executeCommand(machineName, "turntable_rotate", DTO);
    }

    @MessageMapping("/{machineName}/command/turntable_eject")
    @WebSocketDoc (description = "Ejects the workpiece from the turntable.",
                indirectOutputs = { "/topic/{machineName}/command-status", "/topic/{machineName}/machine-status" })
    public String executeTurntableEjectCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<MultiProcessingStationMachine> DTO) {
        log.info("Received request on /{}/command/turntable_eject", machineName);
        return executeCommand(machineName, "turntable_eject", DTO);
    }

    @MessageMapping("/{machineName}/command/conveyor_move_to_sensor")
    @WebSocketDoc (description = "Moves the conveyor until the workpiece reaches the sensor position.",
                indirectOutputs = { "/topic/{machineName}/command-status", "/topic/{machineName}/machine-status" })
    public String executeConveyorMoveToSensorCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<MultiProcessingStationMachine> DTO) {
        log.info("Received request on /{}/command/conveyor_move_to_sensor", machineName);
        return executeCommand(machineName, "conveyor_move_to_sensor", DTO);
    }

    @MessageMapping("/{machineName}/command/conveyor_move_out")
    @WebSocketDoc (description = "Moves the conveyor until the workpiece is ejected.",
                indirectOutputs = { "/topic/{machineName}/command-status", "/topic/{machineName}/machine-status" })
    public String executeConveyorMoveOutCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<MultiProcessingStationMachine> DTO) {
        log.info("Received request on /{}/command/conveyor_move_out", machineName);
        return executeCommand(machineName, "conveyor_move_out", DTO);
    }

    @MessageMapping("/{machineName}/command/saw_cut")
    @WebSocketDoc (description = "Activates the saw for the specified duration.",
                indirectOutputs = { "/topic/{machineName}/command-status", "/topic/{machineName}/machine-status" })
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
