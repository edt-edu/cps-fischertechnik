package io.github.mbdo.factoryscada.frontend.controller.ws;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.core.GenericMachineStatusRequestDTO;
import io.github.mbdo.factoryscada.domains.vacuumgripper.VacuumGripperMachine;
import io.github.mbdo.factoryscada.frontend.WebSocketDoc;
import io.github.mbdo.factoryscada.service.FactoryScada;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@MessageMapping("/vacuumGripper")
public class VacuumGripperWsController extends AbstractMachineWsController<VacuumGripperMachine> {

    @Autowired
    public VacuumGripperWsController(FactoryScada factoryScada) {
        super(factoryScada);
    }

    @MessageMapping("/{machineName}/command/move")
    @WebSocketDoc (description = "Sends a move command to the specified vacuum gripper machine. It picks a workpiece on the startPos and drop it on the endPos",
                indirectOutputs = {"/topic/{machineName}/command-status"})
    public String executeMoveCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<VacuumGripperMachine> moveDTO) {
        log.info("Received request on /vacuumGripper/{}/command/move", machineName);
        return executeCommand(machineName, "move", moveDTO);
    }

    @MessageMapping("/{machineName}/command/pick")
    @WebSocketDoc (description = "Sends a pick command to the specified vacuum gripper machine. It picks a workpiece on the startPos.",
                indirectOutputs = {"/topic/{machineName}/command-status"})
    public String executePickCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<VacuumGripperMachine> pickDTO) {
        log.info("Received request on /vacuumGripper/{}/command/pick", machineName);
        return executeCommand(machineName, "pick", pickDTO);
    }

    @MessageMapping("/{machineName}/command/place")
    @WebSocketDoc (description = "Sends a place command to the specified vacuum gripper machine. It drops a workpiece on the endPos.",
                indirectOutputs = {"/topic/{machineName}/command-status"})
    public String executePlaceCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<VacuumGripperMachine> placeDTO) {
        log.info("Received request on /vacuumGripper/{}/command/place", machineName);
        return executeCommand(machineName, "place", placeDTO);
    }

    @MessageMapping("/{machineName}/command/go_to_position")
    @WebSocketDoc (description = "Sends a go_to_position command to the specified vacuum gripper machine. Moves the gripper to the position without changing the valve or compressor status.",
                indirectOutputs = {"/topic/{machineName}/command-status"})
    public String executeGoToPositionCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<VacuumGripperMachine> goToPositionDTO) {
        log.info("Received request on /vacuumGripper/{}/command/go_to_position", machineName);
        return executeCommand(machineName, "go_to_position", goToPositionDTO);
    }

    @MessageMapping("/{machineName}/command/setup")
    @WebSocketDoc (description = "Sends a setup command to the specified vacuum gripper machine. It resets the encoders by moving the gripper to the home position.",
                indirectOutputs = {"/topic/{machineName}/command-status"})
    public String executeSetupCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<VacuumGripperMachine> setupDTO) {
        log.info("Received request on /vacuumGripper/{}/command/setup", machineName);
        return executeCommand(machineName, "setup", setupDTO);
    }

    @MessageMapping("/{machineName}/request/status")
    @WebSocketDoc (description = "Requests the status of the specified vacuum gripper machine.")
    public String executeStatusRequest(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineStatusRequestDTO<VacuumGripperMachine> statusDTO) {
        log.info("Received request on /vacuumGripper/{}/command/status", machineName);
        return executeRequest(machineName, statusDTO);
    }

    @MessageMapping("/{machineName}/command/stop")
    @WebSocketDoc (description = "Sends a stop command to the specified vacuum gripper machine.",
                indirectOutputs = {"/topic/{machineName}/command-status"})
    public String executeStopCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<VacuumGripperMachine> stopDTO) {
        log.info("Received request on /vacuumGripper/{}/command/stop", machineName);
        return executeCommand(machineName, "stop", stopDTO);
    }
    @MessageMapping("/{machineName}/command/grip")
    @WebSocketDoc (description = "Sends a grip command to the specified vacuum gripper machine. It activates the vacuum gripper without moving the arm.",
                indirectOutputs = {"/topic/{machineName}/command-status"})
    public String executeGripCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<VacuumGripperMachine> gripDTO) {
        log.info("Received request on /vacuumGripper/{}/command/grip", machineName);
        return executeCommand(machineName, "grip", gripDTO);
    }
    @MessageMapping("/{machineName}/command/release")
    @WebSocketDoc (description = "Sends a release command to the specified vacuum gripper machine. It deactivates the vacuum gripper without moving the arm.",
                indirectOutputs = {"/topic/{machineName}/command-status"})
    public String executeReleaseCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<VacuumGripperMachine> releaseDTO) {
        log.info("Received request on /vacuumGripper/{}/command/release", machineName);
        return executeCommand(machineName, "release", releaseDTO);
    }

    @MessageMapping("/{machineName}/command/move_to_safe_position")
    @WebSocketDoc (description = "Sends a move_to_safe_position command to the specified vacuum gripper machine. Moves the gripper to a predefined safe position without changing the valve or compressor status.",
                indirectOutputs = {"/topic/{machineName}/command-status"})
    public String executeMoveToSafePositionCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<VacuumGripperMachine> move_to_safe_positionDTO) {
        log.info("Received request on /{}/command/move_to_safe_position", machineName);
        return executeCommand(machineName, "move_to_safe_position", move_to_safe_positionDTO);
    }

    @MessageMapping("/{machineName}/command/retract_arm")
    @WebSocketDoc (description = "Sends a retract_arm command to the specified vacuum gripper machine. It retracts the arm without changing the gripper status or other axes.",
                indirectOutputs = {"/topic/{machineName}/command-status"})
    public String executeRetractArmCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<VacuumGripperMachine> retract_armDTO) {
        log.info("Received request on /vacuumGripper/{}/command/retract_arm", machineName);
        return executeCommand(machineName, "retract_arm", retract_armDTO);
    }

    @MessageMapping("/{machineName}/command/ordered_go_to")
    @WebSocketDoc (description = "Sends an ordered_go_to command to the specified vacuum gripper machine. Go to the specified position, moving the prioritized axes first and then the others.",
                indirectOutputs = {"/topic/{machineName}/command-status"})
    public String executeOrderedGoToCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<VacuumGripperMachine> ordered_go_toDTO) {
        log.info("Received request on /vacuumGripper/{}/command/ordered_go_to", machineName);
        return executeCommand(machineName, "ordered_go_to", ordered_go_toDTO);
    }
}
