package com.example.runtime.rtc.exec;

import com.example.runtime.rtc.event.Event;

public final class RuntimeActions {
  private RuntimeActions() {
  }

  public static RuntimeAction noop() {
    return event -> {
    };
  }
}
