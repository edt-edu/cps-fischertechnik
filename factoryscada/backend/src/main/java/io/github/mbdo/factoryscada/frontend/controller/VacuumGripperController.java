package io.github.mbdo.factoryscada.frontend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.core.GenericMachineStatusRequestDTO;
import io.github.mbdo.factoryscada.domains.vacuumgripper.VacuumGripperMachine;
import io.github.mbdo.factoryscada.service.FactoryScada;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@MessageMapping("/vacuumGripper")
public class VacuumGripperController extends AbstractMachineController<VacuumGripperMachine> {

    @Autowired
    public VacuumGripperController(FactoryScada factoryScada) {
        super(factoryScada);
    }

    @MessageMapping("/{machineName}/command/move")
    public String executeMoveCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<VacuumGripperMachine> moveDTO) {
        log.info("Received request on /vacuumGripper/{}/command/move", machineName);
        return executeCommand(machineName, "move", moveDTO);
    }

    @MessageMapping("/{machineName}/command/pick")
    public String executePickCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<VacuumGripperMachine> pickDTO) {
        log.info("Received request on /vacuumGripper/{}/command/pick", machineName);
        return executeCommand(machineName, "pick", pickDTO);
    }

    @MessageMapping("/{machineName}/command/place")
    public String executePlaceCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<VacuumGripperMachine> placeDTO) {
        log.info("Received request on /vacuumGripper/{}/command/place", machineName);
        return executeCommand(machineName, "place", placeDTO);
    }

    @MessageMapping("/{machineName}/command/go_to_position")
    public String executeGoToPositionCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<VacuumGripperMachine> goToPositionDTO) {
        log.info("Received request on /vacuumGripper/{}/command/go_to_position", machineName);
        return executeCommand(machineName, "go_to_position", goToPositionDTO);
    }

    @MessageMapping("/{machineName}/command/setup")
    public String executeSetupCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<VacuumGripperMachine> setupDTO) {
        log.info("Received request on /vacuumGripper/{}/command/setup", machineName);
        return executeCommand(machineName, "setup", setupDTO);
    }

    @MessageMapping("/{machineName}/request/status")
    public String executeStatusRequest(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineStatusRequestDTO<VacuumGripperMachine> statusDTO) {
        log.info("Received request on /vacuumGripper/{}/command/status", machineName);
        return executeRequest(machineName, statusDTO);
    }

    @MessageMapping("/{machineName}/command/stop")
    public String executeStopCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<VacuumGripperMachine> stopDTO) {
        log.info("Received request on /vacuumGripper/{}/command/stop", machineName);
        return executeCommand(machineName, "stop", stopDTO);
    }
    @MessageMapping("/{machineName}/command/grip")
    public String executeGripCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<VacuumGripperMachine> gripDTO) {
        log.info("Received request on /vacuumGripper/{}/command/grip", machineName);
        return executeCommand(machineName, "grip", gripDTO);
    }
    @MessageMapping("/{machineName}/command/release")
    public String executeReleaseCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<VacuumGripperMachine> releaseDTO) {
        log.info("Received request on /vacuumGripper/{}/command/release", machineName);
        return executeCommand(machineName, "release", releaseDTO);
    }

    @MessageMapping("/{machineName}/command/move_to_safe_position")
    public String executeMoveToSafePositionCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<VacuumGripperMachine> move_to_safe_positionDTO) {
        log.info("Received request on /{}/command/move_to_safe_position", machineName);
        return executeCommand(machineName, "move_to_safe_position", move_to_safe_positionDTO);
    }

    @MessageMapping("/{machineName}/command/retract_arm")
    public String executeRetractArmCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload GenericMachineCommandDTO<VacuumGripperMachine> retract_armDTO) {
        log.info("Received request on /vacuumGripper/{}/command/retract_arm", machineName);
        return executeCommand(machineName, "retract_arm", retract_armDTO);
    }
}
