package io.github.mbdo.factoryscada.core.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.mbdo.factoryscada.core.Passable;
import io.github.mbdo.factoryscada.core.enums.Color;
import io.github.mbdo.factoryscada.core.enums.PassableType;
import io.github.mbdo.factoryscada.core.passable.NamedPosition;
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
}
