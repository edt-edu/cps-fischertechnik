package io.github.mbdo.factoryscada.mqtt;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Mqtt publisher for any SCADA concerns
 * uses baseTopic +  as root of any published sub topics
 * This base topic is computed from MqttConfig
 */
@Service
public class MqttPublisher {

    private final RawMqttOutboundGateway gateway;
    private final String baseTopic;

    public MqttPublisher(
            RawMqttOutboundGateway gateway,
            @Qualifier("mqttPublisherBaseTopic") String baseTopic) {
        this.gateway = gateway;
        this.baseTopic = baseTopic;
    }

    public void publish(String relativeTopic, String payload) {
        gateway.sendToMqtt(payload, baseTopic + (!baseTopic.isBlank()? "/":"") + relativeTopic);
    }
}
