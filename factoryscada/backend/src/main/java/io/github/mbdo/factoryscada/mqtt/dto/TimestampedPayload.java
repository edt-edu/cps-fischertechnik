package io.github.mbdo.factoryscada.mqtt.dto;

import java.time.Instant;

public record TimestampedPayload<T>(
        T value,
        Instant timestamp
) {}
