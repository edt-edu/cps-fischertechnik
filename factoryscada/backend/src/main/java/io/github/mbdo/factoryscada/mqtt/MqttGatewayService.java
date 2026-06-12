package io.github.mbdo.factoryscada.mqtt;

import io.github.mbdo.factoryscada.core.MqttMessageRouter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

@Slf4j
@Service
public class MqttGatewayService implements MqttMessageRouter {

  private final MqttGateway mqttGateway;
  private final List<Subscription> subscriptions = new CopyOnWriteArrayList<>();

  @Autowired
  public MqttGatewayService(MqttGateway mqttGateway) {
    this.mqttGateway = mqttGateway;
  }

  public void sendToMqtt(String payload, String topic) {
    mqttGateway.sendToMqtt(payload, topic);
  }

  @Override
  public void subscribe(String topicFilter, BiConsumer<String, String> callback) {
    subscriptions.add(new Subscription(topicFilter, callback));
    log.info("MQTT subscription registered: {}", topicFilter);
  }

  public void onMessage(String topic, byte[] payload) {
    log.debug("Received MQTT message on topic {}", topic);
    route(topic, new String(payload, StandardCharsets.UTF_8));
  }

  /**
   * Route an incoming message to all matching subscribers.
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
        return true;
      }
      if (i >= topicParts.length) {
        return false;
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
