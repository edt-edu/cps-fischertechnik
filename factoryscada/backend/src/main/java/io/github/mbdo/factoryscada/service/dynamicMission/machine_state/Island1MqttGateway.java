package io.github.mbdo.factoryscada.service.dynamicMission.machine_state;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;

import java.io.IOException;

@Slf4j
public class Island1MqttGateway {
  private final MqttClient client;
  private final Island1State state;

  public Island1MqttGateway(Island1State state, MqttClient client) {
    this.client = client;
    this.state = state;
  }

  public void start() throws IOException {
    client.setCallback(connectTopicsToState());
    try {
      client.connect();
      client.subscribe("#");
    } catch (MqttException e) {
      throw new IOException(e);
    }
  }

  @Nonnull
  private MqttCallback connectTopicsToState() {
    return new MqttCallback() {
      @Override
      public void connectionLost(Throwable throwable) {
        log.error("Connection to MQTT lost", throwable);
      }

      @Override
      public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
        System.out.println("Got message on topic " + topic);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode n = mapper.readTree(mqttMessage.getPayload());
        if(!n.isObject()){
          log.warn("Got non object json, ignoring");
          return;
        }

        if(n.has("value")){
          log.warn("Got json object without value field, ignoring");
        }

        messageToState(topic, n);
      }

      @Override
      public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        log.trace("MQTT delivery complete: {}", iMqttDeliveryToken);
      }
    };
  }

  private void messageToState(String topic, JsonNode n) {
    switch (topic){
      case "PLC/Island 1/SortingLine/I1SortingLine01/measurements/input/sortingLineSensInputLightBarrier"
          -> {
        state.getSortingLine01().setInputLightBarrier(n.get("value").asBoolean());
        log.warn("Got value for sortingLineSensInputLightBarrier: {}", state.getSortingLine01().isInputLightBarrier());
      }
      default -> log.trace("Ignoring unknown MQTT topic: " + topic);
    }
  }

  public void stop() throws IOException {
    try {
      client.disconnect();
    } catch (MqttException e) {
      throw new IOException(e);
    }
  }
}
