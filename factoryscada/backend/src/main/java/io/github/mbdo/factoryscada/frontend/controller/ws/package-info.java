/**
 * WebSocket (STOMP) controllers of the backend, mainly used by the Angular frontend.
 * <p>
 * Clients connect to the STOMP endpoint {@code ws://<host>:9000/mbdo-factory-scada}, send messages to
 * {@code /app/...} destinations, which are dispatched to the {@code @MessageMapping} methods of the
 * {@code *WsController} classes of this package, and subscribe to {@code /topic/...} destinations
 * (see {@link io.github.mbdo.factoryscada.configs.WebSocketConfig}).
 * <p>
 * Conventions: classes are named {@code *WsController}, are annotated with {@code @Controller} (never
 * {@code @RestController}), and their handlers are documented with
 * {@link io.github.mbdo.factoryscada.frontend.WebSocketDoc}; outgoing topics are published through
 * {@link io.github.mbdo.factoryscada.frontend.WebSocketPublish} methods. The protocol documentation is generated
 * in {@code docgen/websocket-endpoints-topics.adoc}.
 * <p>
 * The REST counterpart is {@link io.github.mbdo.factoryscada.frontend.controller.rest}; see the
 * "Communication with the backend" section of the backend README to choose between both protocols.
 */
package io.github.mbdo.factoryscada.frontend.controller.ws;
