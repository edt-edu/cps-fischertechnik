package io.github.mbdo.factoryscada.frontend;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Documentation of a WebSocket handler (a method annotated with {@code @MessageMapping}), used by the
 * WebSocketDocGenerator.
 * <p>
 * Complements what the generator detects by itself ({@code @SendTo} and direct calls to
 * {@link WebSocketPublish} methods). The fields of the payload DTOs can be documented with
 * {@code @JsonPropertyDescription}, which is rendered in their JSON schema.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface WebSocketDoc {

    /** What the incoming message does */
    String description() default "";

    /** Meaning of the reply sent to the {@code @SendTo} topic, if any */
    String responseDescription() default "";

    /**
     * Outgoing topics published indirectly as a consequence of this message (through services, machine
     * feedback...), which cannot be detected automatically. Each must be a topic declared by a
     * {@link WebSocketPublish} or {@code @SendTo}, e.g. {@code "/topic/{machineName}/command-status"}.
     */
    String[] indirectOutputs() default {};
}
