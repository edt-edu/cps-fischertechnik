package io.github.mbdo.factoryscada.domains.highbaywarehouse;

import java.util.List;

import io.github.mbdo.factoryscada.core.AbstractMachine;
import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.domains.highbaywarehouse.commands.RetrieveCommand;
import io.github.mbdo.factoryscada.domains.highbaywarehouse.commands.SetupCommand;
import io.github.mbdo.factoryscada.domains.highbaywarehouse.commands.StoreCommand;
import io.github.mbdo.factoryscada.socket.Protocol;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HighBayWarehouseMachine extends AbstractMachine {

    public HighBayWarehouseMachine(String name, Protocol protocol, List<String> rawCommandNames) {
        super(name, protocol, rawCommandNames);
    }

    public static String getType() {
        return "highBayWarehouse";
    }

    public void retrieve(@Valid @NotNull final GenericMachineCommandDTO<HighBayWarehouseMachine> dto) {
        log.info("Retrieve HighBayWarehouse {}", dto);
        new RetrieveCommand(this, dto).execute();
    }

    public void setup(@Valid @NotNull final GenericMachineCommandDTO<HighBayWarehouseMachine> dto) {
        log.info("Setting up HighBayWarehouse {}", dto);
        new SetupCommand(this, dto).execute();
    }

    public void store(@Valid @NotNull final GenericMachineCommandDTO<HighBayWarehouseMachine> dto) {
        log.info("Store HighBayWarehouse {}", dto);
        new StoreCommand(this, dto).execute();
    }

//    public void eject(@Valid @NotNull final EjectDTO ejectDTO) {
//        log.info("Eject sortingLine {}", ejectDTO);
//        new EjectCommand(this, ejectDTO);
//    }

}
