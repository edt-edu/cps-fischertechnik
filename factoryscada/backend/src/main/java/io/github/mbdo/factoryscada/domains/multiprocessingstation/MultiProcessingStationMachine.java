package io.github.mbdo.factoryscada.domains.multiprocessingstation;

import java.util.List;

import io.github.mbdo.factoryscada.core.AbstractMachine;
import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.domains.multiprocessingstation.commands.MoveToSafePositionCommand;
import io.github.mbdo.factoryscada.domains.multiprocessingstation.commands.Process1Command;
import io.github.mbdo.factoryscada.domains.multiprocessingstation.commands.ProcessCommand;
import io.github.mbdo.factoryscada.domains.multiprocessingstation.commands.SetupCommand;
import io.github.mbdo.factoryscada.domains.multiprocessingstation.commands.StopCommand;
import io.github.mbdo.factoryscada.socket.Protocol;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MultiProcessingStationMachine extends AbstractMachine {

    public MultiProcessingStationMachine(String name, Protocol protocol, List<String> rawCommandNames) {
        super(name, protocol, rawCommandNames);
    }

    public static String getType() {
        return "multiProcessingStation";
    }
    
    public void setup(@Valid @NotNull final GenericMachineCommandDTO<MultiProcessingStationMachine> dto) {
        log.info("Setting up MultiProcessingStation {}", dto);
        new SetupCommand(this, dto).execute();
    }

    public void process1(@Valid @NotNull final GenericMachineCommandDTO<MultiProcessingStationMachine> dto) {
        log.info("Process1 MultiProcessingStation {}", dto);
        new Process1Command(this, dto).execute();
    }

    public void process(@Valid @NotNull final GenericMachineCommandDTO<MultiProcessingStationMachine> dto) {
        log.info("Process MultiProcessingStation {}", dto);
        new ProcessCommand(this, dto).execute();
    }

    public void stop(@Valid @NotNull final GenericMachineCommandDTO<MultiProcessingStationMachine> dto) {
        log.info("Stop MultiProcessingStation {}", dto);
        new StopCommand(this, dto).execute();
    }

    public void move_to_safe_position(@Valid @NotNull final GenericMachineCommandDTO<MultiProcessingStationMachine> dto) {
        log.info("Move To Safe Position {}", dto);
        new MoveToSafePositionCommand(this, dto).execute();
    }



//    public void eject(@Valid @NotNull final EjectDTO ejectDTO) {
//        log.info("Eject sortingLine {}", ejectDTO);
//        new EjectCommand(this, ejectDTO);
//    }

}
