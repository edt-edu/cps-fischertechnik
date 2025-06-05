package io.github.mbdo.factoryscada.core.passable;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.mbdo.factoryscada.core.Passable;
import io.github.mbdo.factoryscada.core.enums.PositionMeaning;
import lombok.Data;

@Data
public class PositionParameterThreeD implements Passable {

    @JsonProperty("meaning")
    private PositionMeaning meaning;

    @JsonProperty("vertical")
    private int vertical;

    @JsonProperty("horizontal")
    private int horizontal;

    @JsonProperty("rot")
    private int rot;

}
