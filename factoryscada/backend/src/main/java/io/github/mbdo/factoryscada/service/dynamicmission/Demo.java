package io.github.mbdo.factoryscada.service.dynamicmission;

import io.github.mbdo.factoryscada.core.enums.Color;
import io.github.mbdo.factoryscada.core.enums.DirectionKind;
import io.github.mbdo.factoryscada.core.enums.MPSOutput;
import io.github.mbdo.factoryscada.core.passable.NamedPosition;
import io.github.mbdo.factoryscada.domains.conveyorbelt.ConveyorBeltMachine;
import io.github.mbdo.factoryscada.domains.multiprocessingstation.MultiProcessingStationMachine;
import io.github.mbdo.factoryscada.domains.sortingline.SortingLineMachine;
import io.github.mbdo.factoryscada.domains.vacuumgripper.VacuumGripperMachine;
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
  private static final String VGR2_TOPIC = "I1VacuumGripper02";
  private static final String CONVEYOR_TOPIC = "I1ConveyorBelt01";
  private static final String VGR1_TOPIC = "I1VacuumGripper01";
  private static final String MPS_TOPIC = "I1MultiProcessing01";

  private final FactoryScada factoryScada;
  private final SimpMessagingTemplate template;
  private volatile boolean active = false;

  private SortingLineMachine sortingLine;
  private VacuumGripperMachine vacuumGripper2;
  private ConveyorBeltMachine conveyorBelt;
  private VacuumGripperMachine vacuumGripper1;
  private MultiProcessingStationMachine multiProcessingStation;

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
    this.sortingLine = getMachine(SortingLineMachine.class, SORTING_LINE_TOPIC);
    this.vacuumGripper2 = getMachine(VacuumGripperMachine.class, VGR2_TOPIC);
    this.conveyorBelt = getMachine(ConveyorBeltMachine.class, CONVEYOR_TOPIC);
    this.vacuumGripper1 = getMachine(VacuumGripperMachine.class, VGR1_TOPIC);
    this.multiProcessingStation = getMachine(MultiProcessingStationMachine.class, MPS_TOPIC);

    //TODO add a button in the frontend to start and stop this service
    start();
  }

  private <T> T getMachine(Class<T> machineClass, String machineName) throws IllegalArgumentException {
    var machine = getFactoryScada().getFactoryScadaInstance().machines().get(machineName);
    //isInstance check is null-safe
    if (!machineClass.isInstance(machine)) {
      throw new IllegalArgumentException("Cannot find machine " + machineName);
    }

    return machineClass.cast(machine);
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
    var cbState = island1State.getConveyorBelt01();
    var mpsState = island1State.getMultiProcessing01();
    Object conveyorBeltOwner = null;
    var processingState = ProcessingState.IDLE;

    while (active) {
      //sort token if one is present at sl input

      if (!slState.isInputLightBarrier() && sortingLine.isIdle()) {
        log.info("Sorting token");
        sortingLine.eject(Color.AUTO);
      }

      //move token from sl out to cb if there is room
      if (vacuumGripper2.isIdle() &&
          conveyorBelt.isIdle() &&
          cbState.isFeedLightBarrier() &&
          conveyorBeltOwner == null) {
        String originName;
        if (!slState.isOutputWhiteLightBarrier()) {
          originName = "SL_OUTPUT_WHITE";
        } else if (!slState.isOutputRedLightBarrier()) {
          originName = "SL_OUTPUT_RED";
        } else if (!slState.isOutputBlueLightBarrier()) {
          originName = "SL_OUTPUT_BLUE";
        } else {
          originName = null;
        }
        if (originName != null) {
          log.info("Moving token from {} to CB", originName);
          conveyorBeltOwner = vacuumGripper2;
          vacuumGripper2.move(new NamedPosition(originName), new NamedPosition("CB"));
        }
      }

      //release conveyor belt lock
      if (vacuumGripper2.isIdle() && conveyorBeltOwner == vacuumGripper2) {
        log.info("Releasing CB lock");
        conveyorBeltOwner = null;
        //vacuumGripper2.setup();
      }

      //move token to swap on cb
      if (conveyorBelt.isIdle() &&
          !cbState.isFeedLightBarrier() &&
          cbState.isSwapLightBarrier() &&
          conveyorBeltOwner == null) {
        conveyorBelt.moveToSensor(DirectionKind.FORWARD);
      }

      //move token to mps
      if (!cbState.isSwapLightBarrier() &&
          vacuumGripper1.isIdle() &&
          multiProcessingStation.isIdle() &&
          processingState == ProcessingState.IDLE) {
        vacuumGripper1.move(new NamedPosition("CB"), new NamedPosition("MPS_INPUT"));
        processingState = ProcessingState.DELIVERING_TOKEN;
      }

      //move vgr1 out of the way
      if (vacuumGripper1.isIdle() && processingState == ProcessingState.DELIVERING_TOKEN) {
        vacuumGripper1.move_to_safe_position();
        processingState = ProcessingState.GOTO_SAFETY;
      }

      //this may happen in parallel with the vgr going to safety
      if (!mpsState.isInputLightBarrier() && multiProcessingStation.isIdle() && processingState == ProcessingState.GOTO_SAFETY) {
        multiProcessingStation.process(2, 2, MPSOutput.CONVEYOR);
        processingState = ProcessingState.PROCESSING;
      }

      if (multiProcessingStation.isIdle() && processingState == ProcessingState.PROCESSING) {
        processingState = ProcessingState.IDLE;
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

  enum ProcessingState {
    IDLE,
    DELIVERING_TOKEN,
    GOTO_SAFETY,
    PROCESSING,
  }
}
