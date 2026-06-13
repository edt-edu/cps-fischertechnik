package io.github.mbdo.factoryscada.mqtt.dto;

import java.time.Instant;

public record TimestampedPayload<T>(
        Instant timestamp,
        T payload
) {}
