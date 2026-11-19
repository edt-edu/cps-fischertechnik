/**
 * REST (HTTP) controllers of the backend, used by the Angular frontend and by external clients such as the
 * digital twin.
 * <p>
 * Endpoints are plain HTTP requests to {@code http://<host>:9000/api/...}: the class-level {@code @RequestMapping}
 * followed by the {@code @GetMapping} / {@code @PostMapping}... of the method.
 * <p>
 * Conventions: classes are named {@code *RestController}, are annotated with {@code @RestController}, their base path
 * starts with {@code /api}, and their handlers are documented with {@link io.github.mbdo.factoryscada.frontend.RestDoc}.
 * The API documentation is generated in {@code docgen/rest-endpoints.adoc}.
 * <p>
 * The WebSocket (STOMP) counterpart is {@link io.github.mbdo.factoryscada.frontend.controller.ws}; see the
 * "Communication with the backend" section of the backend README to choose between both protocols.
 */
package io.github.mbdo.factoryscada.frontend.controller.rest;
