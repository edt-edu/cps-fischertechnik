package io.github.mbdo.factoryscada.domains.conveyorbelt;

import java.util.List;

import io.github.mbdo.factoryscada.core.AbstractMachine;
import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.domains.conveyorbelt.commands.MoveNbStepsCommand;
import io.github.mbdo.factoryscada.domains.conveyorbelt.commands.MoveOutCommand;
import io.github.mbdo.factoryscada.domains.conveyorbelt.commands.MoveToSensor;
import io.github.mbdo.factoryscada.domains.conveyorbelt.commands.StopCommand;
import io.github.mbdo.factoryscada.socket.Protocol;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConveyorBeltMachine extends AbstractMachine {

    public ConveyorBeltMachine(String name, Protocol protocol, List<String> rawCommandNames) {
        super(name, protocol, rawCommandNames);
    }

    public static String getType() {
        return "conveyorBelt";
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

    public void stop(@Valid @NotNull final GenericMachineCommandDTO<ConveyorBeltMachine> stopDTO) {
        log.info("Stop conveyor {}", stopDTO);
        new StopCommand(this, stopDTO).execute();
    }

}
