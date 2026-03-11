package io.github.mbdo.factoryscada.core.passable;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.mbdo.factoryscada.core.Passable;

public class Bool implements Passable {
  @JsonProperty("bool")
  private boolean bool;
}
