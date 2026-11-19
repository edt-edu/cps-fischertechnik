package io.github.mbdo.factoryscada.frontend;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Documentation of a REST handler (a method of a {@code @RestController} annotated with {@code @RequestMapping}
 * or one of its variants such as {@code @GetMapping} or {@code @PostMapping}), used by the RestDocGenerator.
 * <p>
 * REST counterpart of {@link WebSocketDoc}. The HTTP method, path, parameters, request body and response
 * types are detected by the generator; the fields of the DTOs can be documented with
 * {@code @JsonPropertyDescription}, which is rendered in their JSON schema.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RestDoc {

    /** What the endpoint does, including the meaning of its non-2xx status codes */
    String description() default "";

    /** Meaning of the response body, or of the response status when there is no body */
    String responseDescription() default "";
}
