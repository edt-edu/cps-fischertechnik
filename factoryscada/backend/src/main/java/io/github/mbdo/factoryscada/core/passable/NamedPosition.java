package io.github.mbdo.factoryscada.core.passable;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.mbdo.factoryscada.core.Passable;
import lombok.Data;

@Data
public class NamedPosition implements Passable {
  @JsonProperty("name")
  private String name;
}
