package com.example.generated.testmachinesystem.testmachinemessages;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import com.example.runtime.rtc.event.DomainEvent;

public record EventMessage2(
    Instant timestamp,
    UUID correlationId,
    Optional<UUID> causationId
) implements DomainEvent {
  public static EventMessage2 now() {
    return new EventMessage2(Instant.now(), UUID.randomUUID(), Optional.empty());
  }
}
