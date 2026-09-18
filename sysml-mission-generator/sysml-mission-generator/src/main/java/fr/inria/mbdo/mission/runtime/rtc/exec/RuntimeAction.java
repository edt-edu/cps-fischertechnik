package fr.inria.mbdo.mission.runtime.rtc.exec;

import fr.inria.mbdo.mission.runtime.rtc.event.Event;

@FunctionalInterface
public interface RuntimeAction {
  void execute(Event event);
}
