package io.github.mbdo.factoryscada.domains.conveyorbelt;

import io.github.mbdo.factoryscada.core.AbstractMachine;
import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.core.dtos.Parameter;
import io.github.mbdo.factoryscada.core.enums.DirectionKind;
import io.github.mbdo.factoryscada.domains.conveyorbelt.commands.MoveNbStepsCommand;
import io.github.mbdo.factoryscada.domains.conveyorbelt.commands.MoveOutCommand;
import io.github.mbdo.factoryscada.domains.conveyorbelt.commands.MoveToSensor;
import io.github.mbdo.factoryscada.domains.conveyorbelt.commands.StopCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
public class ConveyorBeltMachine extends AbstractMachine {

    private boolean tokenAtFeed;
    private boolean tokenAtSwap;

    public ConveyorBeltMachine(Parameters parameters) {
        super(parameters);
    }

    public static String getType() {
        return "conveyorBelt";
    }

    @Override
    public String getCommandMachineType() {
        return "CONVEYOR";
    }

    public void moveToSensor(DirectionKind direction) {
        moveToSensor(createCommandDTO("move_to_sensor", Parameter.direction(direction)));
    }

    public void moveToSensor(@Valid @NotNull final GenericMachineCommandDTO<ConveyorBeltMachine> forwardLeaveDTO) {
        log.info("Forward leave conveyor {}", forwardLeaveDTO);
        new MoveToSensor(this, forwardLeaveDTO).execute();
    }

    public void moveNbSteps(@Valid @NotNull final GenericMachineCommandDTO<ConveyorBeltMachine> gotoConfigDTO) {
        log.info("Goto config conveyor {}", gotoConfigDTO);
        new MoveNbStepsCommand(this, gotoConfigDTO).execute();
    }

    public void moveOut(@Valid @NotNull final GenericMachineCommandDTO<ConveyorBeltMachine> moveDTO) {
        log.info("Moving conveyor {}", moveDTO);
        new MoveOutCommand(this, moveDTO).execute();
    }

    @Override
    public void stop() {
        stop(createCommandDTO("stop"));
    }

    public void stop(@Valid @NotNull final GenericMachineCommandDTO<ConveyorBeltMachine> stopDTO) {
        log.info("Stop conveyor {}", stopDTO);
        new StopCommand(this, stopDTO).execute();
    }

}
