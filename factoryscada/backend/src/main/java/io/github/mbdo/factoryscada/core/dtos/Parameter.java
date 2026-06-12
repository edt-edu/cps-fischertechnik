package io.github.mbdo.factoryscada.core.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.mbdo.factoryscada.core.Passable;
import io.github.mbdo.factoryscada.core.enums.Color;
import io.github.mbdo.factoryscada.core.enums.DirectionKind;
import io.github.mbdo.factoryscada.core.enums.MPSOutput;
import io.github.mbdo.factoryscada.core.enums.PassableType;
import io.github.mbdo.factoryscada.core.passable.NamedPosition;
import io.github.mbdo.factoryscada.core.passable.NumberNatural;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonDeserialize(using = ParameterDeserializer.class)
public class Parameter {

    @JsonProperty("passableType")
    private PassableType passableType;

    @JsonProperty("passable")
    private Passable passable;

    public static Parameter color(Color color) {
        return new Parameter(PassableType.COLOR, new io.github.mbdo.factoryscada.core.passable.Color(color));
    }

    public static Parameter namedPosition(String name) {
        return new Parameter(PassableType.NAMEDPOSITION, new NamedPosition(name));
    }

    public static Parameter direction(DirectionKind direction) {
        return new Parameter(PassableType.DIRECTION, new io.github.mbdo.factoryscada.core.passable.Direction(direction));
    }

    public static Parameter numberNatural(int number) {
        return new Parameter(PassableType.NUMBERNATURAL, new NumberNatural(number));
    }

    public static Parameter mpsOutput(MPSOutput output) {
        return new Parameter(PassableType.MPSOUTPUT, new io.github.mbdo.factoryscada.core.passable.MPSOutput(output));
    }
}
