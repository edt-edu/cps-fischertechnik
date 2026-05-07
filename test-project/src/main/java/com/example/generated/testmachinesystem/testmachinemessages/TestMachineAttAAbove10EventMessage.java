package com.example.generated.testmachinesystem.testmachinemessages;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.example.runtime.rtc.event.DomainEvent;

public record TestMachineAttAAbove10EventMessage(
    int attA,
    Instant timestamp,
    UUID correlationId,
    Optional<UUID> causationId
) implements DomainEvent {
  public static TestMachineAttAAbove10EventMessage now(int attA) {
    return new TestMachineAttAAbove10EventMessage(attA, Instant.now(), UUID.randomUUID(), Optional.empty());
  }
}