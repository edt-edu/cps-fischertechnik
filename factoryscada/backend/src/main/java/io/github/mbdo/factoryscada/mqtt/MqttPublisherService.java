package io.github.mbdo.factoryscada.mqtt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Mqtt publisher for any SCADA concerns
 * uses baseTopic +  as root of any published sub topics
 * This base topic is computed from MqttConfig
 */
@Slf4j
@Service
public class MqttPublisherService {

    private final MqttConfig mqttConfig;
    private final RawMqttOutboundGateway gateway;
    private final String baseTopic;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public MqttPublisherService(
            MqttConfig mqttConfig, RawMqttOutboundGateway gateway,
            @Qualifier("mqttPublisherBaseTopic") String baseTopic) {
        this.mqttConfig = mqttConfig;
        this.gateway = gateway;
        this.baseTopic = baseTopic;
    }

    public void publish(String relativeTopic, String payload) {
        gateway.sendToMqtt(payload, resolvedTopic(relativeTopic));
    }

    private String resolvedTopic(String relativeTopic) {
        return baseTopic + (!baseTopic.isBlank()? "/":"") + relativeTopic;
    }

    /**
     * special publish that can be used early in the backend startup process
     * @param relativeTopic
     * @param payload
     */
    public void publishWithRetry(String relativeTopic, String payload) {
        try {
            publish(relativeTopic, payload);
        } catch (Exception e) {
            log.warn(
                    "Failed to publish MQTT message to broker={} topic={}, MQTT system may be not ready: retrying in 1s",
                    mqttConfig.getMqttOutboundHost(), resolvedTopic(relativeTopic));
            scheduler.schedule(() -> publishWithRetry(payload, relativeTopic), 1, TimeUnit.SECONDS);
        }
    }
}
