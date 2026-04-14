package io.github.mbdo.factoryscada.service.dynamicMission;

import io.github.mbdo.factoryscada.domains.sortingline.SortingLineMachine;
import io.github.mbdo.factoryscada.service.FactoryScada;
import io.github.mbdo.factoryscada.service.dynamicMission.machine_state.Island1MqttGateway;
import io.github.mbdo.factoryscada.service.dynamicMission.machine_state.Island1State;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Random;

@Slf4j
@Service
@Getter
public class Demo {

  public static final String SORTING_LINE_TOPIC = "I1SortingLine01";
  private final FactoryScada factoryScada;
  private final SimpMessagingTemplate template;
  private volatile boolean active = false;
  private SortingLineMachine sortingLine;

  private Island1State island1State;
  private MqttClient mqttClient;
  private Island1MqttGateway island1MqttGateway;

  @Autowired
  public Demo(FactoryScada factoryScada, SimpMessagingTemplate template) {
    this.factoryScada = factoryScada;
    this.template = template;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void initAfterStartup() throws IOException {
    if (factoryScada.getFactoryScadaInstance().machines().get(SORTING_LINE_TOPIC) instanceof SortingLineMachine sl) {
      this.sortingLine = sl;
    } else {
      throw new IllegalStateException("Sorting line machine not found");
    }

    //TODO add a button in the frontend to start and stop this service
    start();
  }

  public synchronized void start() throws IOException {
    if (active) return;

    active = true;
    island1State = new Island1State();
    try {
      // TODO: configurable
      mqttClient = new MqttClient("tcp://localhost:1883", "client" + new Random().nextInt());
    } catch (MqttException e) {
      throw new IOException(e);
    }
    island1MqttGateway = new Island1MqttGateway(island1State, mqttClient);
    island1MqttGateway.start();

    new Thread(this::run).start();
  }

  public void stop() throws IOException {
    active = false;
    island1MqttGateway.stop();
  }

  private void run() {
    while (active) {
      if (!island1State.getSortingLine01().isInputLightBarrier() && isMachineIdle(SORTING_LINE_TOPIC)) {
        //TODO sort token to red, later sort it to random

      }
    }
  }

  // TODO: not typesafe enough, please instead add new fields to Island1State, SortingLineState, ...
  @SuppressWarnings("SameParameterValue")
  private boolean getInputStatus(String machineName, String inputName) {
    //TODO this method currently mocks the input status which is published in mqtt
    //noinspection SwitchStatementWithTooFewBranches
    return switch (machineName) {
      case "I1SortingLine01" -> switch (inputName) {
        case "sortingLineSensRedLightBarrier" -> false;
        case "sortingLineSensWhiteLightBarrier", "sortingLineSensBlueLightBarrier" -> true;
        default -> throw new IllegalArgumentException("Unknown input: " + inputName);
      };
      default -> throw new IllegalArgumentException("Unknown machine: " + machineName);
    };
  }

  @SuppressWarnings("SameParameterValue")
  private boolean isMachineIdle(String machineName) {
    var status = factoryScada.getMachineLastMachineStatusMap().get(machineName);
    if (status == null) return true;

    return status.getMachineFeedbackStatus().contains("IDLE");
  }
}
