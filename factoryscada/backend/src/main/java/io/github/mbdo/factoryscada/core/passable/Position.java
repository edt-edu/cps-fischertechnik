package io.github.mbdo.factoryscada.core.passable;

import io.github.mbdo.factoryscada.core.Passable;
import io.github.mbdo.factoryscada.core.dtos.Parameter;

public interface Position extends Passable {
  Parameter toParameter();
}
