package io.github.mbdo.factoryscada.service.dynamicMission;

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

  private final FactoryScada factoryScada;
  private final SimpMessagingTemplate template;
  private volatile boolean active = false;

  @Autowired
  public Demo(FactoryScada factoryScada, SimpMessagingTemplate template) {
    this.factoryScada = factoryScada;
    this.template = template;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void initAfterStartup() {
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
      var slStatus = factoryScada.getMachineLastMachineStatusMap().get("I1SortingLine");
      if (slStatus != null) {
        log.info("SL status: {}", slStatus);
        //TODO evaluate machine status of SL
        //TODO send sort command to SL
      }

      try {
        Thread.sleep(3000);
      } catch (InterruptedException e) {
        log.warn("Thread was interrupted during sleep.");
      }
    }
  }
}
