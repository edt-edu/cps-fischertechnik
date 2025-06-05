package io.github.mbdo.factoryscada.core.passable;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.github.mbdo.factoryscada.core.Passable;
import io.github.mbdo.factoryscada.core.enums.DirectionKind;

public class Direction implements Passable {
    @JsonProperty("direction")
    private DirectionKind direction;
}
