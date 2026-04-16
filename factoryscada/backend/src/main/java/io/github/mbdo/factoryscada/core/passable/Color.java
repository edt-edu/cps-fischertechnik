package io.github.mbdo.factoryscada.core.passable;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.mbdo.factoryscada.core.Passable;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class Color implements Passable {

    @JsonProperty("color")
    public io.github.mbdo.factoryscada.core.enums.Color color;

}
