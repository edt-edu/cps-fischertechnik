package io.github.mbdo.factoryscada.mqtt;

import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;

/**
 * Low level transport gateway that send the payload to an Mqtt topic
 */
@MessagingGateway(defaultRequestChannel = "mqttOutboundChannel")
public interface RawMqttOutboundGateway {
    void sendToMqtt(@Header("mqtt_topic") String topic, @Payload String payload);
}