package io.github.mbdo.factoryscada.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mbdo.factoryscada.domains.conveyorbelt.ConveyorBeltMachine;
import io.github.mbdo.factoryscada.domains.highbaywarehouse.HighBayWarehouseMachine;
import io.github.mbdo.factoryscada.domains.indexedline.IndexedLineMachine;
import io.github.mbdo.factoryscada.domains.multiprocessingstation.MultiProcessingStationMachine;
import io.github.mbdo.factoryscada.domains.punchingmachine.PunchingMachine;
import io.github.mbdo.factoryscada.domains.sortingline.SortingLineMachine;
import io.github.mbdo.factoryscada.domains.vacuumgripper.VacuumGripperMachine;
import io.github.mbdo.factoryscada.service.FactoryScada;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.regex.Pattern;

@Slf4j
@Service
public class MqttGatewayService {
  //PLC/<island>/<type>/<machineName>/measurements/input/<inputName>
  private static final Pattern TOPIC_PATTERN = Pattern.compile("PLC/([^/]+)/([^/]+)/([^/]+)/measurements/input/(.+)");

  private final MqttGateway mqttGateway;
  private final FactoryScada factoryScada;

  @Autowired
  public MqttGatewayService(MqttGateway mqttGateway, FactoryScada factoryScada) {
    this.mqttGateway = mqttGateway;
    this.factoryScada = factoryScada;
  }

  public void sendToMqtt(String payload, String topic) {
    mqttGateway.sendToMqtt(payload, topic);
  }

  public void onMessage(String topic, byte[] payload) {
    log.debug("Received MQTT message on topic {}", topic);
    updateMachineStateFromMessage(topic, payload);
  }

  /**
   * Attempts to update the state of a machine from an incoming mqtt message.
   *
   * <p>If an error occurs, it may produce a log message, but it will not throw an exception.
   *
   * @param topic   The topic on which the message arrived
   * @param payload The message payload
   */
  public void updateMachineStateFromMessage(String topic, byte[] payload) {
    var mapper = new ObjectMapper();
    JsonNode node;
    try {
      node = mapper.readTree(payload);
    } catch (IOException e) {
      return;
    }
    if (!node.isObject() || !node.has("value")) {
      return;
    }

    var value = node.get("value");
    var matcher = TOPIC_PATTERN.matcher(topic);
    if (!matcher.matches()) {
      return;
    }

    //group 1 is the island, we don't need that
    var machineType = matcher.group(2);
    var machineName = matcher.group(3);
    var inputName = matcher.group(4);

    try {
      updateMachineState(machineType, machineName, inputName, value);
    } catch (IllegalArgumentException e) {
      log.warn("Failed to update machine state", e);
    }
  }

  private void updateMachineState(String machineType, String machineName, String inputName, JsonNode value)
  throws IllegalArgumentException {
    var machine = factoryScada.getFactoryScadaInstance().machines().get(machineName);
    if (machine == null) {
      throw new IllegalArgumentException("Cannot find a machine with the name " + machineName);
    }

    switch (machine) {
      case ConveyorBeltMachine cb -> {
        switch (inputName) {
          case "conveyorSensFeed" -> cb.setTokenAtFeed(!value.asBoolean());
          case "conveyorSensSwap" -> cb.setTokenAtSwap(!value.asBoolean());
          default -> logIgnoredInput(machineName, inputName);
        }
      }
      case HighBayWarehouseMachine ignored -> logIgnoredInput(machineName, inputName);
      case IndexedLineMachine ignored -> logIgnoredInput(machineName, inputName);
      case MultiProcessingStationMachine mps -> {
        switch (inputName) {
          case "multiProcessingSensOven" -> mps.setTokenAtFeed(!value.asBoolean());
          case "multiProcessingSendEndConveyor" -> mps.setTokenAtSwap(!value.asBoolean());
          default -> logIgnoredInput(machineName, inputName);
        }
      }
      case PunchingMachine ignored -> logIgnoredInput(machineName, inputName);
      case SortingLineMachine sl -> {
        switch (inputName) {
          case "sortingLineSensInputLightBarrier" -> sl.setTokenAtFeed(!value.asBoolean());
          case "sortingLineSensWhiteLightBarrier" -> sl.setTokenAtWhite(!value.asBoolean());
          case "sortingLineSensRedLightBarrier" -> sl.setTokenAtRed(!value.asBoolean());
          case "sortingLineSensBlueLightBarrier" -> sl.setTokenAtBlue(!value.asBoolean());
          default -> logIgnoredInput(machineName, inputName);
        }
      }
      case VacuumGripperMachine ignored -> logIgnoredInput(machineName, inputName);
      default -> throw new IllegalArgumentException("Unsupported machine type: " + machineType);
    }
  }

  private void logIgnoredInput(String machineName, String inputName) {
    log.debug("Ignoring input {}/{}", machineName, inputName);
  }
}
