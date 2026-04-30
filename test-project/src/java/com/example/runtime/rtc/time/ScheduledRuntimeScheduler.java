package com.example.runtime.rtc.time;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import com.example.runtime.rtc.event.TimerFiredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ScheduledRuntimeScheduler implements RuntimeScheduler {
  private final ScheduledExecutorService exec;
  private final Map<String, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();
  private static final Logger logger = LoggerFactory.getLogger(ScheduledRuntimeScheduler.class);

  public ScheduledRuntimeScheduler() {
    this.exec = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r);
      t.setName("ScheduledRuntimeScheduler");
      t.setDaemon(true);
      return t;
    });
  }

  @Override
  public void scheduleTimeout(String timerId, long delayMillis, Consumer<TimerFiredEvent> sink) {
    logger.info("Scheduling timer {} in {}ms", timerId, delayMillis);
    ScheduledFuture<?> f = exec.schedule(() -> {
      logger.info("Firing timer {}", timerId);
      try {
        sink.accept(TimerFiredEvent.now(timerId));
      } catch (Throwable t) {
        logger.error("Timer sink threw", t);
      }
    }, delayMillis, TimeUnit.MILLISECONDS);
    tasks.put(timerId, f);
  }

  @Override
  public void cancelTimeout(String timerId) {
    logger.info("Cancel timer {}", timerId);
    ScheduledFuture<?> f = tasks.remove(timerId);
    if (f != null) f.cancel(false);
  }

  public void shutdown() {
    logger.info("Shutting down scheduler");
    exec.shutdownNow();
    tasks.clear();
  }
}
