package fr.inria.mbdo.mission.runtime.rtc.time;

import java.util.function.Consumer;
import fr.inria.mbdo.mission.runtime.rtc.event.TimerFiredEvent;

public interface RuntimeScheduler {
  void scheduleTimeout(String timerId, long delayMillis, Consumer<TimerFiredEvent> sink);

  void cancelTimeout(String timerId);
}
