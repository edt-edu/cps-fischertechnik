package io.github.mbdo.factoryscada.utilities;

import java.util.concurrent.atomic.AtomicLong;

import lombok.Getter;

/**
 * Simple id genereator using a simple increment strategie
 */
@Getter
public class CommandIdGenerator {

    private final AtomicLong counter = new AtomicLong(1);

    public long generateId() {
        return counter.getAndIncrement();
    }
}
