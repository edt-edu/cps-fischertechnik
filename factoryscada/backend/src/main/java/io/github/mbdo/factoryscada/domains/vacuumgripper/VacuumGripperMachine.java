package io.github.mbdo.factoryscada.domains.vacuumgripper;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.mbdo.factoryscada.core.AbstractMachine;
import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.core.passable.Position;
import io.github.mbdo.factoryscada.domains.vacuumgripper.commands.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
public class VacuumGripperMachine extends AbstractMachine {

    private boolean armRetracted;

    public VacuumGripperMachine(Parameters parameters) {
        super(parameters);
    }

    public static String getType() {
        return "vacuumGripper";
    }

    @Override
    protected void onMqttInputMessage(String inputName, JsonNode value) {
        switch (inputName) {
            case "vacuumSensArmEndIn" -> setArmRetracted(value.asBoolean());
            default -> super.onMqttInputMessage(inputName, value);
        }
    }

    @Override
    public String getCommandMachineType() {
        return "VACUUM";
    }

    public void go_to_position(@Valid @NotNull final GenericMachineCommandDTO<VacuumGripperMachine> goToPositionDTO) {
        log.info("GoToPosition vacuum gripper {}", goToPositionDTO);
        new GoToPositionCommand(this, goToPositionDTO).execute();
    }

    public void move(Position origin, Position destination) {
        move(createCommandDTO("move", origin.toParameter(), destination.toParameter()));
    }

    public void move(@Valid @NotNull final GenericMachineCommandDTO<VacuumGripperMachine> moveDTO) {
        log.info("Moving vacuum gripper {}", moveDTO);
        new MoveCommand(this, moveDTO).execute();
    }

    public void pick(Position position) {
        pick(createCommandDTO("pick", position.toParameter()));
    }

    public void pick(@Valid @NotNull final GenericMachineCommandDTO<VacuumGripperMachine> pickDTO) {
        log.info("Picking with vacuum gripper {}", pickDTO);
        new PickCommand(this, pickDTO).execute();
    }

    public void place(Position position) {
        place(createCommandDTO("place", position.toParameter()));
    }

    public void place(@Valid @NotNull final GenericMachineCommandDTO<VacuumGripperMachine> placeDTO) {
        log.info("Placing with vacuum gripper {}", placeDTO);
        new PlaceCommand(this, placeDTO).execute();
    }

    public void goToPosition(Position position) {
        go_to_position(createCommandDTO("go_to_position", position.toParameter()));
    }

    public void setup() {
        setup(createCommandDTO("setup"));
    }

    public void setup(@Valid @NotNull final GenericMachineCommandDTO<VacuumGripperMachine> setupDTO) {
        log.info("Setting up vacuum gripper {}", setupDTO);
        new SetupCommand(this, setupDTO).execute();
    }

    public void statusRequest() {
        status(createCommandDTO("statusRequest"));
    }

    public void status(@Valid @NotNull final GenericMachineCommandDTO<VacuumGripperMachine> statusDTO) {
        log.info("Checking status of vacuum gripper {}", statusDTO);
        new StatusCommand(this, statusDTO).execute();
    }

    public void grip() {
        grip(createCommandDTO("grip"));
    }

    public void grip(@Valid @NotNull final GenericMachineCommandDTO<VacuumGripperMachine> gripDTO) {
        log.info("Activate grip of the vacuum gripper {}", gripDTO);
        new GripCommand(this, gripDTO).execute();
    }

    public void release() {
        release(createCommandDTO("release"));
    }

    public void release(@Valid @NotNull final GenericMachineCommandDTO<VacuumGripperMachine> releaseDTO) {
        log.info("Release grip vacuum gripper {}", releaseDTO);
        new GripCommand(this, releaseDTO).execute();
    }

    @Override
    public void stop() {
        stop(createCommandDTO("stop"));
    }

    public void stop(@Valid @NotNull final GenericMachineCommandDTO<VacuumGripperMachine> stopDTO) {
        log.info("Stopping vacuum gripper {}", stopDTO);
        new GripCommand(this, stopDTO).execute();
    }

    public void go_to_safe_position() {
        move_to_safe_position(createCommandDTO("move_to_safe_position"));
    }

    public void move_to_safe_position(@Valid @NotNull final GenericMachineCommandDTO<VacuumGripperMachine> dto) {
        log.info("Move To Safe Position {}", dto);
        new MoveToSafePositionCommand(this, dto).execute();
    }

    public void retract_arm() {
        retract_arm(createCommandDTO("retract_arm"));
    }

    public void retract_arm(@Valid @NotNull final GenericMachineCommandDTO<VacuumGripperMachine> retract_armDTO) {
        log.info("Retracting vacuum gripper's arm {}", retract_armDTO);
        new RetractArmCommand(this, retract_armDTO).execute();
    }

    public void ordered_go_to(@Valid @NotNull final GenericMachineCommandDTO<VacuumGripperMachine> ordered_go_toDTO) {
        log.info("Moving vacuum gripper with priotized axis {}", ordered_go_toDTO);
        new OrderedGoToCommand(this, ordered_go_toDTO).execute();
    }

}
