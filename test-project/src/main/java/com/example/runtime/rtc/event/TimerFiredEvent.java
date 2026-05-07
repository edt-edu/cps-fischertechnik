package com.example.runtime.rtc.event;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public record TimerFiredEvent(
    String timerId,
    Instant timestamp,
    UUID correlationId,
    Optional<UUID> causationId
) implements TimerEvent {
  public static TimerFiredEvent now(String timerId) {
    return new TimerFiredEvent(timerId, Instant.now(), UUID.randomUUID(), Optional.empty());
  }
}
