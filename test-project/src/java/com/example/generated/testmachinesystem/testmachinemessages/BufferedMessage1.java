package com.example.generated.testmachinesystem.testmachinemessages;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import com.example.runtime.rtc.event.DomainEvent;

public record BufferedMessage1(
    Instant timestamp,
    UUID correlationId,
    Optional<UUID> causationId
) implements DomainEvent {
  public static BufferedMessage1 now() {
    return new BufferedMessage1(Instant.now(), UUID.randomUUID(), Optional.empty());
  }
}
