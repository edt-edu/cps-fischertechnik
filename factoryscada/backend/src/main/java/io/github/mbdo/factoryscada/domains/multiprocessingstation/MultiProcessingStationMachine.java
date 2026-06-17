package io.github.mbdo.factoryscada.domains.multiprocessingstation;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.mbdo.factoryscada.core.AbstractMachine;
import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.core.dtos.Parameter;
import io.github.mbdo.factoryscada.core.enums.MPSOutput;
import io.github.mbdo.factoryscada.domains.multiprocessingstation.commands.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
public class MultiProcessingStationMachine extends AbstractMachine {

    private boolean tokenAtFeed;
    private boolean tokenAtSwap;

    public MultiProcessingStationMachine(Parameters parameters) {
        super(parameters);
    }

    public static String getType() {
        return "multiProcessingStation";
    }

    @Override
    protected void onMqttInputMessage(String inputName, JsonNode value) {
        switch (inputName) {
            case "multiProcessingSensOven" -> setTokenAtFeed(!value.asBoolean());
            case "multiProcessingSendEndConveyor" -> setTokenAtSwap(!value.asBoolean());
            default -> super.onMqttInputMessage(inputName, value);
        }
    }

    @Override
    public String getCommandMachineType() {
        return "MULTIPROCESSING";
    }

    public void setup() {
        setup(createCommandDTO("setup"));
    }

    public void setup(@Valid @NotNull final GenericMachineCommandDTO<MultiProcessingStationMachine> dto) {
        log.info("Setting up MultiProcessingStation {}", dto);
        new SetupCommand(this, dto).execute();
    }

    public void process1() {
        process1(createCommandDTO("process1"));
    }

    public void process1(@Valid @NotNull final GenericMachineCommandDTO<MultiProcessingStationMachine> dto) {
        log.info("Process1 MultiProcessingStation {}", dto);
        new Process1Command(this, dto).execute();
    }

    public void process(int ovenTime, int sawTime, MPSOutput output) {
        process(createCommandDTO("process",
                Parameter.numberNatural(ovenTime),
                Parameter.numberNatural(sawTime),
                Parameter.mpsOutput(output)));
    }

    public void process() {
        process(createCommandDTO("process"));
    }

    public void process(@Valid @NotNull final GenericMachineCommandDTO<MultiProcessingStationMachine> dto) {
        log.info("Process MultiProcessingStation {}", dto);
        new ProcessCommand(this, dto).execute();
    }

    @Override
    public void stop() {
        stop(createCommandDTO("stop"));
    }

    public void stop(@Valid @NotNull final GenericMachineCommandDTO<MultiProcessingStationMachine> dto) {
        log.info("Stop MultiProcessingStation {}", dto);
        new StopCommand(this, dto).execute();
    }

    public void moveToSafePosition() {
        move_to_safe_position(createCommandDTO("move_to_safe_position"));
    }

    public void move_to_safe_position(
            @Valid @NotNull final GenericMachineCommandDTO<MultiProcessingStationMachine> dto) {
        log.info("Move To Safe Position {}", dto);
        new MoveToSafePositionCommand(this, dto).execute();
    }

    public void ovenLoad() { oven_load(createCommandDTO("oven_load")); }
    public void ovenUnload() { oven_unload(createCommandDTO("oven_unload")); }
    public void ovenHeat() { oven_heat(createCommandDTO("oven_heat")); }
    public void ovenProcess() { oven_process(createCommandDTO("oven_process")); }
    public void armMove() { arm_move(createCommandDTO("arm_move")); }
    public void armPick() { arm_pick(createCommandDTO("arm_pick")); }
    public void armPlace() { arm_place(createCommandDTO("arm_place")); }
    public void turntableRotate() { turntable_rotate(createCommandDTO("turntable_rotate")); }
    public void turntableEject() { turntable_eject(createCommandDTO("turntable_eject")); }
    public void conveyorMoveToSensor() { conveyor_move_to_sensor(createCommandDTO("conveyor_move_to_sensor")); }
    public void conveyorMoveOut() { conveyor_move_out(createCommandDTO("conveyor_move_out")); }
    public void sawCut() { saw_cut(createCommandDTO("saw_cut")); }

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

    public void conveyor_move_to_sensor(
            @Valid @NotNull final GenericMachineCommandDTO<MultiProcessingStationMachine> dto) {
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

    // public void eject(@Valid @NotNull final EjectDTO ejectDTO) {
    // log.info("Eject sortingLine {}", ejectDTO);
    // new EjectCommand(this, ejectDTO);
    // }

}
