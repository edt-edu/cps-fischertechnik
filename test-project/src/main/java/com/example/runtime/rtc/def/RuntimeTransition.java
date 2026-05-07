package com.example.runtime.rtc.def;

import java.util.function.Supplier;
import com.example.runtime.rtc.event.Event;
import com.example.runtime.rtc.exec.RuntimeAction;
import com.example.runtime.rtc.exec.RuntimeGuard;

public record RuntimeTransition(
    Class<? extends Event> triggerType,
    RuntimeGuard guard,
    RuntimeAction effect,
    Supplier<RuntimeState> target
) {
  public RuntimeState targetState() {
    return target.get();
  }
}
