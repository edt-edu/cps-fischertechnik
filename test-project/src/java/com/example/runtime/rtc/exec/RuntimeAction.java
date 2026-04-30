package com.example.runtime.rtc.exec;

import com.example.runtime.rtc.event.Event;

@FunctionalInterface
public interface RuntimeAction {
  void execute(Event event);
}
