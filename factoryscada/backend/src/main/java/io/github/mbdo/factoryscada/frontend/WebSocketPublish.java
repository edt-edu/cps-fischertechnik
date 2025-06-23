package io.github.mbdo.factoryscada.frontend;

import  java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
/**
 * Custom annotation used help document generator by declaring outgoing topics
 */
public @interface WebSocketPublish {
    String value(); // the topic
    Class<?> payload() default Void.class; // optional, describe payload type
    String description() default "";
}
