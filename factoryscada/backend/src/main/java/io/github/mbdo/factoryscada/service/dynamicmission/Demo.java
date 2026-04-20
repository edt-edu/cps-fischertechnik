package io.github.mbdo.factoryscada.service.dynamicmission;

import io.github.mbdo.factoryscada.core.enums.Color;
import io.github.mbdo.factoryscada.domains.sortingline.SortingLineMachine;
import io.github.mbdo.factoryscada.service.FactoryScada;
import io.github.mbdo.factoryscada.service.dynamicmission.machinestate.Island1MqttGateway;
import io.github.mbdo.factoryscada.service.dynamicmission.machinestate.Island1State;
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

  private static final String SORTING_LINE_TOPIC = "I1SortingLine01";

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
    new Thread(this::run).start();
  }

  private void startMQTTGateway() {
    try {
      // TODO: configurable
      mqttClient = new MqttClient("tcp://localhost:1883", "client" + new Random().nextInt());
    } catch (MqttException e) {
      log.error("Failed to create MQTT client", e);
      return;
    }
    island1MqttGateway = new Island1MqttGateway(island1State, mqttClient);
    try {
      island1MqttGateway.start();
    } catch (IOException e) {
      log.error("Failed to start MQTT gateway", e);
    }
  }

  public void stop() throws IOException {
    active = false;
  }

  private void run() {
    log.info("Demo started");

    island1State = new Island1State(); //reset island state
    startMQTTGateway();
    var slState = island1State.getSortingLine01();

    while (active) {
      if (!slState.isInputLightBarrier() && sortingLine.isIdle()) {
        sortingLine.eject(Color.AUTO);
      }

      sleep(5);
    }

    log.info("Demo stopped");
    stopMQTTGateway();
  }

  @SuppressWarnings("SameParameterValue")
  private static void sleep(int millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      log.warn("Event loop was interrupted");
    }
  }

  private void stopMQTTGateway() {
    try {
      island1MqttGateway.stop();
    } catch (IOException e) {
      log.error("Failed to stop MQTT gateway", e);
    }
  }
}
