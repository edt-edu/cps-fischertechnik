package io.github.mbdo.factoryscada.core.passable;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.github.mbdo.factoryscada.core.Passable;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class NumberNatural implements Passable{


    @JsonProperty("number")
    @PositiveOrZero(message = "The number must be positive or zero")
    private int number;

}
