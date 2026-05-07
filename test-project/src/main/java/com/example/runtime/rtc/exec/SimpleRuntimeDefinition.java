package com.example.runtime.rtc.exec;

import java.util.Optional;
import com.example.runtime.rtc.def.RuntimeState;
import com.example.runtime.rtc.def.RuntimeTransition;
import com.example.runtime.rtc.event.Event;

public final class SimpleRuntimeDefinition implements RuntimeDefinition {
  private final RuntimeState initial;

  public SimpleRuntimeDefinition(RuntimeState initial) {
    this.initial = initial;
  }

  @Override
  public RuntimeState initialState() {
    return initial;
  }

  @Override
  public Optional<RuntimeTransition> findTransition(RuntimeState from, Event event) {
    return from.transitions().stream()
        .filter(t -> t.triggerType().isInstance(event) && t.guard().test(event))
        .findFirst();
  }

  @Override
  public Optional<RuntimeTransition> findCompletion(RuntimeState from) {
    return from.transitions().stream()
        .filter(t -> t.triggerType().equals(com.example.runtime.rtc.event.CompletionEvent.class))
        .findFirst();
  }

  @Override
  public boolean isDeferred(RuntimeState from, Event event) {
    return false;
  }
}
