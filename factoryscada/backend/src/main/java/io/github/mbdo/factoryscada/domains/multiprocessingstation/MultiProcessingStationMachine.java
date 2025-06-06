package io.github.mbdo.factoryscada.domains.multiprocessingstation;

import io.github.mbdo.factoryscada.core.AbstractMachine;
import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.socket.Protocol;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MultiProcessingStationMachine extends AbstractMachine {

    public MultiProcessingStationMachine(String name, Protocol protocol) {
        super(name, protocol);
    }

    public static String getType() {
        return "multiProcessingStation";
    }
    
    public void setup(@Valid @NotNull final GenericMachineCommandDTO<MultiProcessingStationMachine> setupDTO) {
        log.info("Setting up MultiProcessingStation {}", setupDTO);
        //new SetupCommand(this, setupDTO);
    }

    public void process1(@Valid @NotNull final GenericMachineCommandDTO<MultiProcessingStationMachine> setupDTO) {
        log.info("Process1 MultiProcessingStation {}", setupDTO);
        //new SetupCommand(this, setupDTO);
    }

    public void stop(@Valid @NotNull final GenericMachineCommandDTO<MultiProcessingStationMachine> setupDTO) {
        log.info("Stop MultiProcessingStation {}", setupDTO);
        //new SetupCommand(this, setupDTO);
    }



//    public void eject(@Valid @NotNull final EjectDTO ejectDTO) {
//        log.info("Eject sortingLine {}", ejectDTO);
//        new EjectCommand(this, ejectDTO);
//    }

}
