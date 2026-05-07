package com.example.runtime.rtc.event;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public record CompletionEvent(
    Instant timestamp,
    UUID correlationId,
    Optional<UUID> causationId
) implements InternalEvent {
  public static CompletionEvent now() {
    return new CompletionEvent(Instant.now(), UUID.randomUUID(), Optional.empty());
  }
}
