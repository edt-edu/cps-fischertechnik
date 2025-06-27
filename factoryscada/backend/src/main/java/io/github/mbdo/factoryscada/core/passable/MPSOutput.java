package io.github.mbdo.factoryscada.core.passable;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.mbdo.factoryscada.core.Passable;
import io.github.mbdo.factoryscada.core.enums.PositionMeaning;
import lombok.Data;

@Data
public class MPSOutput implements Passable {

    @JsonProperty("output")
    private io.github.mbdo.factoryscada.core.enums.MPSOutput output;

}
