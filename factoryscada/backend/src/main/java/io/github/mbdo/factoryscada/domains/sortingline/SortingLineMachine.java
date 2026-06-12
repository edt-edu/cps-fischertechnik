package io.github.mbdo.factoryscada.domains.sortingline;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.mbdo.factoryscada.core.AbstractMachine;
import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.core.dtos.Parameter;
import io.github.mbdo.factoryscada.core.enums.Color;
import io.github.mbdo.factoryscada.domains.sortingline.commands.EjectCommand;
import io.github.mbdo.factoryscada.domains.sortingline.commands.StopCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
public class SortingLineMachine extends AbstractMachine {

    private boolean tokenAtFeed;
    private boolean tokenAtWhite;
    private boolean tokenAtRed;
    private boolean tokenAtBlue;

    public SortingLineMachine(Parameters parameters) {
        super(parameters);
    }

    public static String getType() {
        return "sortingLine";
    }

    @Override
    protected void onMqttInputMessage(String inputName, JsonNode value) {
        switch (inputName) {
            case "sortingLineSensInputLightBarrier" -> setTokenAtFeed(!value.asBoolean());
            case "sortingLineSensWhiteLightBarrier" -> setTokenAtWhite(!value.asBoolean());
            case "sortingLineSensRedLightBarrier" -> setTokenAtRed(!value.asBoolean());
            case "sortingLineSensBlueLightBarrier" -> setTokenAtBlue(!value.asBoolean());
            default -> super.onMqttInputMessage(inputName, value);
        }
    }

    @Override
    public String getCommandMachineType() {
        return "SORTING";
    }

    public void eject(Color color) {
        eject(createCommandDTO("eject", Parameter.color(color)));
    }

    public void eject(@Valid @NotNull final GenericMachineCommandDTO<SortingLineMachine> ejectDTO) {
        log.info("Eject sortingLine {}", ejectDTO);
        new EjectCommand(this, ejectDTO).execute();
    }

    @Override
    public void stop() {
        stop(createCommandDTO("stop"));
    }

    public void stop(@Valid @NotNull final GenericMachineCommandDTO<SortingLineMachine> stopDTO) {
        log.info("Stop sortingLine {}", stopDTO);
        new StopCommand(this, stopDTO).execute();
    }

}
