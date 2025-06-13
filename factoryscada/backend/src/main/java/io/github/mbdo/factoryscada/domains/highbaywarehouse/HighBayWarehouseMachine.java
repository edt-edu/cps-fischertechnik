package io.github.mbdo.factoryscada.domains.highbaywarehouse;

import java.util.List;

import io.github.mbdo.factoryscada.core.AbstractMachine;
import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.domains.highbaywarehouse.commands.CantileverBackwardCommand;
import io.github.mbdo.factoryscada.domains.highbaywarehouse.commands.CantileverForwardCommand;
import io.github.mbdo.factoryscada.domains.highbaywarehouse.commands.ConveyorBackwardCommand;
import io.github.mbdo.factoryscada.domains.highbaywarehouse.commands.ConveyorForwardCommand;
import io.github.mbdo.factoryscada.domains.highbaywarehouse.commands.ConveyorStopCommand;
import io.github.mbdo.factoryscada.domains.highbaywarehouse.commands.GoToColumnCommand;
import io.github.mbdo.factoryscada.domains.highbaywarehouse.commands.GoToRowCommand;
import io.github.mbdo.factoryscada.domains.highbaywarehouse.commands.HorizontalToCommand;
import io.github.mbdo.factoryscada.domains.highbaywarehouse.commands.RetrieveCommand;
import io.github.mbdo.factoryscada.domains.highbaywarehouse.commands.SetupCommand;
import io.github.mbdo.factoryscada.domains.highbaywarehouse.commands.StopCommand;
import io.github.mbdo.factoryscada.domains.highbaywarehouse.commands.StoreCommand;
import io.github.mbdo.factoryscada.domains.highbaywarehouse.commands.VerticalToCommand;
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

    public void cantilever_backward(@Valid @NotNull final GenericMachineCommandDTO<HighBayWarehouseMachine> dto) {
        log.info("Cantilever Backward HighBayWarehouse {}", dto);
        new CantileverBackwardCommand(this, dto).execute();
    }

    public void cantilever_forward(@Valid @NotNull final GenericMachineCommandDTO<HighBayWarehouseMachine> dto) {
        log.info("Cantilever Forward HighBayWarehouse {}", dto);
        new CantileverForwardCommand(this, dto).execute();
    }

    public void conveyor_backward(@Valid @NotNull final GenericMachineCommandDTO<HighBayWarehouseMachine> dto) {
        log.info("Conveyor Backward HighBayWarehouse {}", dto);
        new ConveyorBackwardCommand(this, dto).execute();
    }

    public void conveyor_forward(@Valid @NotNull final GenericMachineCommandDTO<HighBayWarehouseMachine> dto) {
        log.info("Conveyor Forward HighBayWarehouse {}", dto);
        new ConveyorForwardCommand(this, dto).execute();
    }

    public void conveyor_stop(@Valid @NotNull final GenericMachineCommandDTO<HighBayWarehouseMachine> dto) {
        log.info("Conveyor Stop HighBayWarehouse {}", dto);
        new ConveyorStopCommand(this, dto).execute();
    }

    public void go_to_column(@Valid @NotNull final GenericMachineCommandDTO<HighBayWarehouseMachine> dto) {
        log.info("Go To Column HighBayWarehouse {}", dto);
        new GoToColumnCommand(this, dto).execute();
    }

    public void go_to_row(@Valid @NotNull final GenericMachineCommandDTO<HighBayWarehouseMachine> dto) {
        log.info("Go To Row HighBayWarehouse {}", dto);
        new GoToRowCommand(this, dto).execute();
    }

    public void horizontal_to(@Valid @NotNull final GenericMachineCommandDTO<HighBayWarehouseMachine> dto) {
        log.info("Horizontal To HighBayWarehouse {}", dto);
        new HorizontalToCommand(this, dto).execute();
    }

    public void retrieve(@Valid @NotNull final GenericMachineCommandDTO<HighBayWarehouseMachine> dto) {
        log.info("Retrieve HighBayWarehouse {}", dto);
        new RetrieveCommand(this, dto).execute();
    }

    public void setup(@Valid @NotNull final GenericMachineCommandDTO<HighBayWarehouseMachine> dto) {
        log.info("Setting up HighBayWarehouse {}", dto);
        new SetupCommand(this, dto).execute();
    }

    public void stop(@Valid @NotNull final GenericMachineCommandDTO<HighBayWarehouseMachine> dto) {
        log.info("Stop HighBayWarehouse {}", dto);
        new StopCommand(this, dto).execute();
    }

    public void store(@Valid @NotNull final GenericMachineCommandDTO<HighBayWarehouseMachine> dto) {
        log.info("Store HighBayWarehouse {}", dto);
        new StoreCommand(this, dto).execute();
    }

    public void vertical_to(@Valid @NotNull final GenericMachineCommandDTO<HighBayWarehouseMachine> dto) {
        log.info("Vertical To HighBayWarehouse {}", dto);
        new VerticalToCommand(this, dto).execute();
    }

//    public void eject(@Valid @NotNull final EjectDTO ejectDTO) {
//        log.info("Eject sortingLine {}", ejectDTO);
//        new EjectCommand(this, ejectDTO);
//    }

}
