package fr.inria.mbdo.mission.runtime.rtc.event;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public sealed interface Event permits DomainEvent, InternalEvent, TimerEvent {
    Instant timestamp();

    UUID correlationId();

    Optional<UUID> causationId();
}
