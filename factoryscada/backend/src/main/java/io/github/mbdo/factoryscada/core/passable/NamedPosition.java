package io.github.mbdo.factoryscada.core.passable;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.mbdo.factoryscada.core.Passable;
import io.github.mbdo.factoryscada.core.dtos.Parameter;
import io.github.mbdo.factoryscada.core.enums.PassableType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class NamedPosition implements Passable, Position {
  @JsonProperty("name")
  private String name;

  @Override
  public Parameter toParameter() {
    return new Parameter(PassableType.NAMEDPOSITION, this);
  }
}
