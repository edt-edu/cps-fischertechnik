package com.example.runtime.rtc.time;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import com.example.runtime.rtc.event.TimerFiredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ManualScheduler implements RuntimeScheduler {
  private final Map<String, ScheduledTimeout> timeouts;
  private long currentMillis;
  private static final Logger logger = LoggerFactory.getLogger(ManualScheduler.class);

  public ManualScheduler() {
    this.timeouts = new HashMap<>();
    this.currentMillis = 0L;
  }

  @Override
  public void scheduleTimeout(String timerId, long delayMillis, Consumer<TimerFiredEvent> sink) {
    long dueAt = currentMillis + delayMillis;
    logger.info("Manual schedule {} due in {}ms (at {})", timerId, delayMillis, dueAt);
    timeouts.put(timerId, new ScheduledTimeout(dueAt, sink));
  }

  @Override
  public void cancelTimeout(String timerId) {
    logger.info("Manual cancel {}", timerId);
    timeouts.remove(timerId);
  }

  public void advanceTime(long deltaMillis) {
    currentMillis += deltaMillis;
    for (Map.Entry<String, ScheduledTimeout> entry : Map.copyOf(timeouts).entrySet()) {
      if (entry.getValue().dueAt <= currentMillis) {
        String id = entry.getKey();
        timeouts.remove(id);
        logger.info("Manual firing timer {}", id);
        entry.getValue().sink.accept(new TimerFiredEvent(
            id,
            Instant.now(),
            java.util.UUID.randomUUID(),
            java.util.Optional.empty()
        ));
      }
    }
  }

  private static final class ScheduledTimeout {
    private final long dueAt;
    private final Consumer<TimerFiredEvent> sink;

    private ScheduledTimeout(long dueAt, Consumer<TimerFiredEvent> sink) {
      this.dueAt = dueAt;
      this.sink = sink;
    }
  }
}
