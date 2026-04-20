package io.github.mbdo.factoryscada.core.passable;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.mbdo.factoryscada.core.Passable;
import io.github.mbdo.factoryscada.core.dtos.Parameter;
import io.github.mbdo.factoryscada.core.enums.PassableType;
import io.github.mbdo.factoryscada.core.enums.PositionMeaning;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class PositionParameterThreeD implements Passable, Position {

    @JsonProperty("meaning")
    private PositionMeaning meaning;

    @JsonProperty("vertical")
    private int vertical;

    @JsonProperty("horizontal")
    private int horizontal;

    @JsonProperty("rot")
    private int rot;

    @Override
    public Parameter toParameter() {
        return new Parameter(PassableType.POSITIONPARAMETERTHREED, this);
    }
}
