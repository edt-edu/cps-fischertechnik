package io.github.mbdo.factoryscada.mqtt;

import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.Gateway;

@MessagingGateway(defaultRequestChannel = "mqttOutboundChannel")
public interface MqttGateway {

    @Gateway
    void sendToMqtt(String data);
}
