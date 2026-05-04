package io.github.mbdo.factoryscada.mqtt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MqttGatewayService {

  private final MqttGateway mqttGateway;

  @Autowired
  public MqttGatewayService(MqttGateway mqttGateway) {
    this.mqttGateway = mqttGateway;
  }

  public void sendToMqtt(String payload, String topic) {
    mqttGateway.sendToMqtt(payload, topic);
  }

  public void onMessage(String topic, byte[] payload) {
    log.info("Received MQTT message on topic {}", topic);
    //TODO implement state updating
  }
}
