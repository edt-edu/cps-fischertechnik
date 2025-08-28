package io.github.mbdo.factoryscada.core.passable;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.mbdo.factoryscada.core.Passable;
import lombok.Data;

@Data
public class MPSTurntablePosition implements Passable {

    @JsonProperty("destination")
    private io.github.mbdo.factoryscada.core.enums.MPSTurntablePosition destination;

}
