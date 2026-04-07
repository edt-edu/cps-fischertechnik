package io.github.mbdo.factoryscada.service.dynamicMission;

import io.github.mbdo.factoryscada.domains.sortingline.SortingLineMachine;
import io.github.mbdo.factoryscada.service.FactoryScada;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Getter
public class Demo {

  public static final String SORTING_LINE_TOPIC = "I1SortingLine01";
  private final FactoryScada factoryScada;
  private final SimpMessagingTemplate template;
  private volatile boolean active = false;
  private SortingLineMachine sortingLine;

  @Autowired
  public Demo(FactoryScada factoryScada, SimpMessagingTemplate template) {
    this.factoryScada = factoryScada;
    this.template = template;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void initAfterStartup() {
    if (factoryScada.getFactoryScadaInstance().machines().get(SORTING_LINE_TOPIC) instanceof SortingLineMachine sl) {
      this.sortingLine = sl;
    } else {
      throw new IllegalStateException("Sorting line machine not found");
    }

    //TODO add a button in the frontend to start and stop this service
    start();
  }

  public synchronized void start() {
    if (active) return;

    active = true;
    new Thread(this::run).start();
  }

  public void stop() {
    active = false;
  }

  private void run() {
    while (active) {
      if (!getInputStatus(SORTING_LINE_TOPIC, "sortingLineSensInputLightBarrier") && isMachineIdle(SORTING_LINE_TOPIC)) {
        //TODO sort token to red, later sort it to random

      }
    }
  }

  @SuppressWarnings("SameParameterValue")
  private boolean getInputStatus(String machineName, String inputName) {
    //TODO this method currently mocks the input status which is published in mqtt
    //noinspection SwitchStatementWithTooFewBranches
    return switch (machineName) {
      case "I1SortingLine01" -> switch (inputName) {
        case "sortingLineSensInputLightBarrier", "sortingLineSensRedLightBarrier" -> false;
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
