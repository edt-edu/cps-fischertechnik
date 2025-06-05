package io.github.mbdo.factoryscada.domains.sortingline;

import io.github.mbdo.factoryscada.core.AbstractMachine;
import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.domains.sortingline.commands.EjectCommand;
import io.github.mbdo.factoryscada.domains.sortingline.commands.StopCommand;
import io.github.mbdo.factoryscada.socket.Protocol;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SortingLineMachine extends AbstractMachine {

    public SortingLineMachine(String name, Protocol protocol) {
        super(name, protocol);
    }

    public static String getType() {
        return "sortingLine";
    }

    public void eject(@Valid @NotNull final GenericMachineCommandDTO<SortingLineMachine> ejectDTO) {
        log.info("Eject sortingLine {}", ejectDTO);
        new EjectCommand(this, ejectDTO).execute();
    }

    public void deteject(@Valid @NotNull final GenericMachineCommandDTO<SortingLineMachine> ejectDTO) {
        log.info("Eject sortingLine {}", ejectDTO);
        new EjectCommand(this, ejectDTO).execute();
    }

    public void stop(@Valid @NotNull final GenericMachineCommandDTO<SortingLineMachine> stopDTO) {
        log.info("Stop sortingLine {}", stopDTO);
        new StopCommand(this, stopDTO).execute();
    }
    
}
