package io.github.mbdo.factoryscada.domains.multiprocessingstation;

import java.util.List;

import io.github.mbdo.factoryscada.core.AbstractMachine;
import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.domains.multiprocessingstation.commands.ArmMoveCommand;
import io.github.mbdo.factoryscada.domains.multiprocessingstation.commands.ArmPickCommand;
import io.github.mbdo.factoryscada.domains.multiprocessingstation.commands.ArmPlaceCommand;
import io.github.mbdo.factoryscada.domains.multiprocessingstation.commands.ConveyorMoveOutCommand;
import io.github.mbdo.factoryscada.domains.multiprocessingstation.commands.ConveyorMoveToSensorCommand;
import io.github.mbdo.factoryscada.domains.multiprocessingstation.commands.MoveToSafePositionCommand;
import io.github.mbdo.factoryscada.domains.multiprocessingstation.commands.OvenHeatCommand;
import io.github.mbdo.factoryscada.domains.multiprocessingstation.commands.OvenLoadCommand;
import io.github.mbdo.factoryscada.domains.multiprocessingstation.commands.OvenProcessCommand;
import io.github.mbdo.factoryscada.domains.multiprocessingstation.commands.OvenUnloadCommand;
import io.github.mbdo.factoryscada.domains.multiprocessingstation.commands.Process1Command;
import io.github.mbdo.factoryscada.domains.multiprocessingstation.commands.ProcessCommand;
import io.github.mbdo.factoryscada.domains.multiprocessingstation.commands.SawCutCommand;
import io.github.mbdo.factoryscada.domains.multiprocessingstation.commands.SetupCommand;
import io.github.mbdo.factoryscada.domains.multiprocessingstation.commands.StopCommand;
import io.github.mbdo.factoryscada.domains.multiprocessingstation.commands.TurntableEjectCommand;
import io.github.mbdo.factoryscada.domains.multiprocessingstation.commands.TurntableRotateCommand;
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

    public void oven_load(@Valid @NotNull final GenericMachineCommandDTO<MultiProcessingStationMachine> dto) {
        log.info("Load the oven {}", dto);
        new OvenLoadCommand(this, dto).execute();
    }

    public void oven_unload(@Valid @NotNull final GenericMachineCommandDTO<MultiProcessingStationMachine> dto) {
        log.info("Unload the oven {}", dto);
        new OvenUnloadCommand(this, dto).execute();
    }

    public void oven_heat(@Valid @NotNull final GenericMachineCommandDTO<MultiProcessingStationMachine> dto) {
        log.info("Heat the oven {}", dto);
        new OvenHeatCommand(this, dto).execute();
    }

    public void oven_process(@Valid @NotNull final GenericMachineCommandDTO<MultiProcessingStationMachine> dto) {
        log.info("Process the payload in the oven {}", dto);
        new OvenProcessCommand(this, dto).execute();
    }

    public void arm_move(@Valid @NotNull final GenericMachineCommandDTO<MultiProcessingStationMachine> dto) {
        log.info("Move the arm {}", dto);
        new ArmMoveCommand(this, dto).execute();
    }

    public void arm_pick(@Valid @NotNull final GenericMachineCommandDTO<MultiProcessingStationMachine> dto) {
        log.info("Pick payload with the arm {}", dto);
        new ArmPickCommand(this, dto).execute();
    }

    public void arm_place(@Valid @NotNull final GenericMachineCommandDTO<MultiProcessingStationMachine> dto) {
        log.info("Place payload with the arm {}", dto);
        new ArmPlaceCommand(this, dto).execute();
    }

    public void turntable_rotate(@Valid @NotNull final GenericMachineCommandDTO<MultiProcessingStationMachine> dto) {
        log.info("Rotate the turntable {}", dto);
        new TurntableRotateCommand(this, dto).execute();
    }

    public void turntable_eject(@Valid @NotNull final GenericMachineCommandDTO<MultiProcessingStationMachine> dto) {
        log.info("Eject the payload from turntable {}", dto);
        new TurntableEjectCommand(this, dto).execute();
    }

    public void conveyor_move_to_sensor(@Valid @NotNull final GenericMachineCommandDTO<MultiProcessingStationMachine> dto) {
        log.info("Move payload to sensor on conveyor {}", dto);
        new ConveyorMoveToSensorCommand(this, dto).execute();
    }

    public void conveyor_move_out(@Valid @NotNull final GenericMachineCommandDTO<MultiProcessingStationMachine> dto) {
        log.info("Move payload out of conveyor {}", dto);
        new ConveyorMoveOutCommand(this, dto).execute();
    }

    public void saw_cut(@Valid @NotNull final GenericMachineCommandDTO<MultiProcessingStationMachine> dto) {
        log.info("Cut with saw {}", dto);
        new SawCutCommand(this, dto).execute();
    }



//    public void eject(@Valid @NotNull final EjectDTO ejectDTO) {
//        log.info("Eject sortingLine {}", ejectDTO);
//        new EjectCommand(this, ejectDTO);
//    }

}
