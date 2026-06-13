package io.github.mbdo.factoryscada.mqtt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mbdo.factoryscada.mqtt.dto.TimestampedPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Mqtt publisher for any SCADA concerns
 * uses baseTopic +  as root of any published sub topics
 * This base topic is computed from MqttConfig
 * Every payload is wrapped into a TimestampedPayload if this is not already of that kind
 */
@Slf4j
@Service
public class MqttPublisherService {


    private final ObjectMapper objectMapper;
    private final MqttConfig mqttConfig;
    private final RawMqttOutboundGateway gateway;
    private final String baseTopic;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public MqttPublisherService(
            ObjectMapper objectMapper, MqttConfig mqttConfig, RawMqttOutboundGateway gateway,
            @Qualifier("mqttPublisherBaseTopic") String baseTopic) {
        this.objectMapper = objectMapper;
        this.mqttConfig = mqttConfig;
        this.gateway = gateway;
        this.baseTopic = baseTopic;
    }

    /**
     *  wrap any object into TimestampedPayload
     */
    public <T> void publish(String relativeTopic, T payload) {
        publish(relativeTopic, new TimestampedPayload<>(
                Instant.now(),
                payload
        ));
    }

    public void publish(String relativeTopic, TimestampedPayload<?> payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            gateway.sendToMqtt(json, resolvedTopic(relativeTopic));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize MQTT payload", e);
        }
    }

    private String resolvedTopic(String relativeTopic) {
        return baseTopic + (!baseTopic.isBlank()? "/":"") + relativeTopic;
    }

    /**
     * special publish that can be used early in the backend startup process
     * @param relativeTopic
     * @param payload
     */
    public <T> void publishWithRetry(String relativeTopic, T payload) {
        try {
            publish(relativeTopic, payload);
        } catch (Exception e) {
            log.warn(
                    "Failed to publish MQTT message to broker={} topic={}, MQTT system may be not ready: retrying in 1s",
                    mqttConfig.getMqttOutboundHost(), resolvedTopic(relativeTopic));
            scheduler.schedule(() -> publishWithRetry( relativeTopic, payload), 1, TimeUnit.SECONDS);
        }
    }
}
