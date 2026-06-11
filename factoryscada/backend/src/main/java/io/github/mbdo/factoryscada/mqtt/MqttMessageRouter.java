package io.github.mbdo.factoryscada.mqtt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

/**
 * A topic-based subscription registry for inbound MQTT messages.
 *
 * <p>
 * Any component (typically a machine adapter) can subscribe to a specific
 * topic filter with a callback. When a message arrives, the router matches
 * the topic against all registered filters and invokes matching callbacks.
 *
 * <p>
 * Topic filters support the MQTT-style {@code +} (single-level) and
 * {@code #} (multi-level) wildcards.
 *
 * <h2>Usage example</h2>
 * 
 * <pre>
 * mqttMessageRouter.subscribe("PLC/+/ConveyorBelt/ConveyorBelt01", (topic, payload) -> {
 *     // handle message
 * });
 * </pre>
 */
@Component
public class MqttMessageRouter {

    private static final Logger log = LoggerFactory.getLogger(MqttMessageRouter.class);

    private final List<Subscription> subscriptions = new CopyOnWriteArrayList<>();

    /**
     * Subscribe to messages matching a topic filter.
     *
     * @param topicFilter MQTT-style topic filter (supports + and # wildcards)
     * @param callback    invoked with (topic, payload) on match
     */
    public void subscribe(String topicFilter, BiConsumer<String, String> callback) {
        subscriptions.add(new Subscription(topicFilter, callback));
        log.info("MQTT subscription registered: {}", topicFilter);
    }

    /**
     * Route an incoming message to all matching subscribers.
     *
     * @param topic   the actual MQTT topic
     * @param payload the message payload
     */
    public void route(String topic, String payload) {
        for (Subscription sub : subscriptions) {
            if (topicMatches(sub.topicFilter(), topic)) {
                try {
                    sub.callback().accept(topic, payload);
                } catch (Exception e) {
                    log.error("Error in MQTT subscription callback for filter '{}': {}",
                            sub.topicFilter(), e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Matches a topic against an MQTT-style topic filter.
     * <ul>
     * <li>{@code +} matches exactly one level</li>
     * <li>{@code #} matches zero or more levels (must be last segment)</li>
     * </ul>
     */
    static boolean topicMatches(String filter, String topic) {
        String[] filterParts = filter.split("/");
        String[] topicParts = topic.split("/");

        for (int i = 0; i < filterParts.length; i++) {
            if ("#".equals(filterParts[i])) {
                return true; // # matches everything remaining
            }
            if (i >= topicParts.length) {
                return false; // topic is shorter than filter
            }
            if (!"+".equals(filterParts[i]) && !filterParts[i].equals(topicParts[i])) {
                return false;
            }
        }
        return filterParts.length == topicParts.length;
    }

    private record Subscription(String topicFilter, BiConsumer<String, String> callback) {
    }
}
