package io.github.mbdo.factoryscada.domains.highbaywarehouse;

import io.github.mbdo.factoryscada.core.AbstractMachine;
import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.domains.highbaywarehouse.commands.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HighBayWarehouseMachine extends AbstractMachine {

    public HighBayWarehouseMachine(Parameters parameters) {
        super(parameters);
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

    public void crane_goto_column(@Valid @NotNull final GenericMachineCommandDTO<HighBayWarehouseMachine> dto) {
        log.info("Go To Column HighBayWarehouse {}", dto);
        new GoToColumnCommand(this, dto).execute();
    }

    public void crane_goto_row(@Valid @NotNull final GenericMachineCommandDTO<HighBayWarehouseMachine> dto) {
        log.info("Go To Row HighBayWarehouse {}", dto);
        new GoToRowCommand(this, dto).execute();
    }

    public void crane_goto_horizontal_position(@Valid @NotNull final GenericMachineCommandDTO<HighBayWarehouseMachine> dto) {
        log.info("Horizontal To HighBayWarehouse {}", dto);
        new HorizontalToCommand(this, dto).execute();
    }

    public void pickup_from(@Valid @NotNull final GenericMachineCommandDTO<HighBayWarehouseMachine> dto) {
        log.info("pickup_from HighBayWarehouse {}", dto);
        new PickupFromCommand(this, dto).execute();
    }

    public void setup(@Valid @NotNull final GenericMachineCommandDTO<HighBayWarehouseMachine> dto) {
        log.info("Setting up HighBayWarehouse {}", dto);
        new SetupCommand(this, dto).execute();
    }

    public void stop(@Valid @NotNull final GenericMachineCommandDTO<HighBayWarehouseMachine> dto) {
        log.info("Stop HighBayWarehouse {}", dto);
        new StopCommand(this, dto).execute();
    }

    public void store_to(@Valid @NotNull final GenericMachineCommandDTO<HighBayWarehouseMachine> dto) {
        log.info("Store_to HighBayWarehouse {}", dto);
        new StoreToCommand(this, dto).execute();
    }

    public void crane_goto_vertical_position(@Valid @NotNull final GenericMachineCommandDTO<HighBayWarehouseMachine> dto) {
        log.info("Vertical To HighBayWarehouse {}", dto);
        new VerticalToCommand(this, dto).execute();
    }

    public void move_to_safe_position(@Valid @NotNull final GenericMachineCommandDTO<HighBayWarehouseMachine> dto) {
        log.info("Move To Safe Position {}", dto);
        new MoveToSafePositionCommand(this, dto).execute();
    }

//    public void eject(@Valid @NotNull final EjectDTO ejectDTO) {
//        log.info("Eject sortingLine {}", ejectDTO);
//        new EjectCommand(this, ejectDTO);
//    }

}
