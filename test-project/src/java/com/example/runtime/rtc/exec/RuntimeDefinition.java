package com.example.runtime.rtc.exec;

import java.util.Optional;
import com.example.runtime.rtc.def.RuntimeState;
import com.example.runtime.rtc.def.RuntimeTransition;
import com.example.runtime.rtc.event.Event;

public interface RuntimeDefinition {
  RuntimeState initialState();

  Optional<RuntimeTransition> findTransition(RuntimeState from, Event event);

  Optional<RuntimeTransition> findCompletion(RuntimeState from);

  boolean isDeferred(RuntimeState from, Event event);
}
