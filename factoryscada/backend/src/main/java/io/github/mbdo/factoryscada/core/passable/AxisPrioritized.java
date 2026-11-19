package io.github.mbdo.factoryscada.core.passable;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.mbdo.factoryscada.core.Passable;
import lombok.Data;

@Data
public class AxisPrioritized implements Passable {

    @JsonProperty("vertical")
    private boolean vertical;

    @JsonProperty("horizontal")
    private boolean horizontal;

    @JsonProperty("rot")
    private boolean rot;

}
