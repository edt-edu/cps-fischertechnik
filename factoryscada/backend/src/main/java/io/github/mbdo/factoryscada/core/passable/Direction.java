package io.github.mbdo.factoryscada.core.passable;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.github.mbdo.factoryscada.core.Passable;
import io.github.mbdo.factoryscada.core.enums.DirectionKind;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class Direction implements Passable {
    @JsonProperty("direction")
    private DirectionKind direction;
}
