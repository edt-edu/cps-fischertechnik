package io.github.mbdo.factoryscada.domains.vacuumgripper;

import java.util.List;

import io.github.mbdo.factoryscada.core.AbstractMachine;
import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.domains.vacuumgripper.commands.GoToPositionCommand;
import io.github.mbdo.factoryscada.domains.vacuumgripper.commands.GripCommand;
import io.github.mbdo.factoryscada.domains.vacuumgripper.commands.MoveCommand;
import io.github.mbdo.factoryscada.domains.vacuumgripper.commands.MoveToSafePositionCommand;
import io.github.mbdo.factoryscada.domains.vacuumgripper.commands.PickCommand;
import io.github.mbdo.factoryscada.domains.vacuumgripper.commands.PlaceCommand;
import io.github.mbdo.factoryscada.domains.vacuumgripper.commands.OrderedMoveToCommand;
import io.github.mbdo.factoryscada.domains.vacuumgripper.commands.RetractArmCommand;
import io.github.mbdo.factoryscada.domains.vacuumgripper.commands.SetupCommand;
import io.github.mbdo.factoryscada.domains.vacuumgripper.commands.StatusCommand;
import io.github.mbdo.factoryscada.socket.Protocol;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VacuumGripperMachine extends AbstractMachine {

    public VacuumGripperMachine(String name, Protocol protocol, List<String> rawCommandNames) {
        super(name, protocol, rawCommandNames);
    }

    public static String getType() {
        return "vacuumGripper";
    }

    public void go_to_position(@Valid @NotNull final GenericMachineCommandDTO<VacuumGripperMachine> goToPositionDTO) {
        log.info("GoToPosition vacuum gripper {}", goToPositionDTO);
        new GoToPositionCommand(this, goToPositionDTO).execute();
    }

    public void move(@Valid @NotNull final GenericMachineCommandDTO<VacuumGripperMachine> moveDTO) {
        log.info("Moving vacuum gripper {}", moveDTO);
        new MoveCommand(this, moveDTO).execute();
    }

    public void pick(@Valid @NotNull final GenericMachineCommandDTO<VacuumGripperMachine> pickDTO) {
        log.info("Picking with vacuum gripper {}", pickDTO);
        new PickCommand(this, pickDTO).execute();
    }

    public void place(@Valid @NotNull final GenericMachineCommandDTO<VacuumGripperMachine> placeDTO) {
        log.info("Placing with vacuum gripper {}", placeDTO);
        new PlaceCommand(this, placeDTO).execute();
    }

    public void setup(@Valid @NotNull final GenericMachineCommandDTO<VacuumGripperMachine> setupDTO) {
        log.info("Setting up vacuum gripper {}", setupDTO);
        new SetupCommand(this, setupDTO).execute();
    }

    public void status(@Valid @NotNull final GenericMachineCommandDTO<VacuumGripperMachine> statusDTO) {
        log.info("Checking status of vacuum gripper {}", statusDTO);
        new StatusCommand(this, statusDTO).execute();
    }

    public void grip(@Valid @NotNull final GenericMachineCommandDTO<VacuumGripperMachine> gripDTO) {
        log.info("Activate grip of the vacuum gripper {}", gripDTO);
        new GripCommand(this, gripDTO).execute();
    }

    public void release(@Valid @NotNull final GenericMachineCommandDTO<VacuumGripperMachine> releaseDTO) {
        log.info("Release grip vacuum gripper {}", releaseDTO);
        new GripCommand(this, releaseDTO).execute();
    }

    public void stop(@Valid @NotNull final GenericMachineCommandDTO<VacuumGripperMachine> stopDTO) {
        log.info("Stopping vacuum gripper {}", stopDTO);
        new GripCommand(this, stopDTO).execute();
    }

    public void move_to_safe_position(@Valid @NotNull final GenericMachineCommandDTO<VacuumGripperMachine> dto) {
        log.info("Move To Safe Position {}", dto);
        new MoveToSafePositionCommand(this, dto).execute();
    }

    public void retract_arm(@Valid @NotNull final GenericMachineCommandDTO<VacuumGripperMachine> retract_armDTO) {
        log.info("Retracting vacuum gripper's arm {}", retract_armDTO);
        new RetractArmCommand(this, retract_armDTO).execute();
    }

    public void ordered_move_to(@Valid @NotNull final GenericMachineCommandDTO<VacuumGripperMachine> ordered_move_toDTO) {
        log.info("Moving vacuum gripper with priotized axis {}", ordered_move_toDTO);
        new OrderedMoveToCommand(this, ordered_move_toDTO).execute();
    }

}
