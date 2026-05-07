package io.github.mbdo.factoryscada.service.dynamicmission;

import io.github.mbdo.factoryscada.core.enums.Color;
import io.github.mbdo.factoryscada.core.enums.DirectionKind;
import io.github.mbdo.factoryscada.core.enums.MPSOutput;
import io.github.mbdo.factoryscada.core.passable.NamedPosition;
import io.github.mbdo.factoryscada.domains.conveyorbelt.ConveyorBeltMachine;
import io.github.mbdo.factoryscada.domains.dynamicmission.DynamicMission;
import io.github.mbdo.factoryscada.domains.multiprocessingstation.MultiProcessingStationMachine;
import io.github.mbdo.factoryscada.domains.sortingline.SortingLineMachine;
import io.github.mbdo.factoryscada.domains.vacuumgripper.VacuumGripperMachine;
import io.github.mbdo.factoryscada.service.FactoryScada;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Slf4j
@Service
@Getter
public class Demo implements DynamicMission {

  private static final String SORTING_LINE_TOPIC = "I1SortingLine01";
  private static final String VGR2_TOPIC = "I1VacuumGripper02";
  private static final String CONVEYOR_TOPIC = "I1ConveyorBelt01";
  private static final String VGR1_TOPIC = "I1VacuumGripper01";
  private static final String MPS_TOPIC = "I1MultiProcessing01";

  private final FactoryScada factoryScada;
  private volatile boolean active = false;
  @Setter
  private volatile boolean cbBroken = false; //TODO add a button in the frontend for this

  private SortingLineMachine sortingLine;
  private VacuumGripperMachine vacuumGripper2;
  private ConveyorBeltMachine conveyorBelt;
  private VacuumGripperMachine vacuumGripper1;
  private MultiProcessingStationMachine multiProcessingStation;

  @Autowired
  public Demo(FactoryScada factoryScada) {
    this.factoryScada = factoryScada;
  }

  @Override
  public String getName() {
    return "Demo";
  }

  @Override
  public String getDescription() {
    return """
        A simple demo that sorts tokens at the sorting line as soon as they are present
        and processes them at the multi-processing-station, using the conveyor belt as buffer.
        """;
  }

  private <T> T getMachine(Class<T> machineClass, String machineName) throws IllegalArgumentException {
    var machine = getFactoryScada().getFactoryScadaInstance().machines().get(machineName);
    //isInstance check is null-safe
    if (!machineClass.isInstance(machine)) {
      throw new IllegalArgumentException("Cannot find machine " + machineName);
    }

    return machineClass.cast(machine);
  }

  @Override
  public Collection<String> getInvolvedMachineNames() {
    return List.of(SORTING_LINE_TOPIC, VGR2_TOPIC, CONVEYOR_TOPIC, VGR1_TOPIC, MPS_TOPIC);
  }

  @Override
  public synchronized void start() {
    if (active) return;

    active = true;
    new Thread(this::run).start();
  }

  @Override
  public void stop() {
    active = false;
  }

  private void run() {
    log.info("Starting Demo");

    //TODO extract machine names into config
    this.sortingLine = getMachine(SortingLineMachine.class, SORTING_LINE_TOPIC);
    this.vacuumGripper2 = getMachine(VacuumGripperMachine.class, VGR2_TOPIC);
    this.conveyorBelt = getMachine(ConveyorBeltMachine.class, CONVEYOR_TOPIC);
    this.vacuumGripper1 = getMachine(VacuumGripperMachine.class, VGR1_TOPIC);
    this.multiProcessingStation = getMachine(MultiProcessingStationMachine.class, MPS_TOPIC);

    var processingState = ProcessingState.IDLE;

    while (active) {
      //sort token if one is present at sl input
      if (sortingLine.isTokenAtFeed() && sortingLine.isIdle()) {
        log.info("Sorting token");
        sortingLine.eject(Color.AUTO);
      } else {
        log.debug("Not sorting token");
        if (!sortingLine.isTokenAtFeed()) log.debug("No token at SL input");
        if (!sortingLine.isIdle()) log.debug("SL busy");
      }

      //move token from sl out to cb if there is room
      if (vacuumGripper2.isIdle() && conveyorBelt.isIdle() && !conveyorBelt.isTokenAtFeed()) {
        String originName;
        if (sortingLine.isTokenAtWhite()) {
          originName = "SL_OUTPUT_WHITE";
        } else if (sortingLine.isTokenAtRed()) {
          originName = "SL_OUTPUT_RED";
        } else if (sortingLine.isTokenAtBlue()) {
          originName = "SL_OUTPUT_BLUE";
        } else {
          originName = null;
        }
        if (originName != null) {
          log.info("Moving token from {} to CB", originName);
          vacuumGripper2.move(new NamedPosition(originName), new NamedPosition("CB"));
        } else {
          log.debug("Not moving from SL to CB: No token at SL output");
        }
      } else {
        if (!vacuumGripper2.isIdle()) log.debug("Not moving from SL to CB: VGR2 busy");
        if (!conveyorBelt.isIdle()) log.debug("Not moving from SL to CB: CB busy");
        if (conveyorBelt.isTokenAtFeed()) log.debug("Not moving from SL to CB: CB feed occupied");
      }

      //move token to swap on cb
      if (conveyorBelt.isIdle() && conveyorBelt.isTokenAtFeed() && !conveyorBelt.isTokenAtSwap()) {
        if (!cbBroken) {
          log.info("Moving token from feed to swap");
          conveyorBelt.moveToSensor(DirectionKind.FORWARD);
        } else {
          log.debug("CB is broken");
          if (vacuumGripper1.isIdle() && processingState != ProcessingState.DELIVERING_TOKEN) {
            log.info("Moving token from feed to swap with VGR1");
            vacuumGripper1.move(new NamedPosition("ALT_CB"), new NamedPosition("CB"));
          } else {
            if (!vacuumGripper1.isIdle()) log.debug("Not moving from feed to swap: VGR1 busy");
            if (processingState == ProcessingState.DELIVERING_TOKEN) log.debug("Not moving from feed to swap: processingState: {}", processingState);
          }
        }
      } else {
        if (!conveyorBelt.isIdle()) log.debug("Not moving from feed to swap: CB busy");
        if (!conveyorBelt.isTokenAtFeed()) log.debug("Not moving from feed to swap: CB feed empty");
        if (conveyorBelt.isTokenAtSwap()) log.debug("Not moving from feed to swap: CB swap occupied");
      }

      //move token to mps
      if (conveyorBelt.isTokenAtSwap() &&
          vacuumGripper1.isIdle() &&
          multiProcessingStation.isIdle() &&
          processingState == ProcessingState.IDLE) {
        log.info("Moving token from CB to MPS");
        vacuumGripper1.move(new NamedPosition("CB"), new NamedPosition("MPS_INPUT"));
        multiProcessingStation.setup(); //ensure the mps is in a state where we can actually place the token
        processingState = ProcessingState.DELIVERING_TOKEN;
      } else {
        if (!conveyorBelt.isTokenAtSwap()) log.debug("Not moving from CB to MPS: CB swap empty");
        if (!vacuumGripper1.isIdle()) log.debug("Not moving from CB to MPS: VGR1 busy");
        if (!multiProcessingStation.isIdle()) log.debug("Not moving from CB to MPS: MPS busy");
        if (processingState != ProcessingState.IDLE) log.debug("Not moving from CB to MPS: processingState: {}", processingState);
      }

      //move vgr1 out of the way
      if (vacuumGripper1.isIdle() && processingState == ProcessingState.DELIVERING_TOKEN) {
        log.info("Going to safety");
        vacuumGripper1.go_to_safe_position();
        processingState = ProcessingState.GOTO_SAFETY;
      } else {
        if (!vacuumGripper1.isIdle()) log.debug("Not going to safety: VGR1 busy");
        if (processingState != ProcessingState.DELIVERING_TOKEN) log.debug("Not going to safety: processingState: {}", processingState);
      }

      //this may happen in parallel with the vgr going to safety
      if (multiProcessingStation.isTokenAtFeed() &&
          multiProcessingStation.isIdle() &&
          processingState == ProcessingState.GOTO_SAFETY) {
        log.info("Processing token");
        multiProcessingStation.process(2, 2, MPSOutput.CONVEYOR);
        processingState = ProcessingState.PROCESSING;
      } else {
        if (!multiProcessingStation.isTokenAtFeed()) log.debug("Not processing token: MPS feed empty");
        if (!multiProcessingStation.isIdle()) log.debug("Not processing token: MPS busy");
        if (processingState != ProcessingState.GOTO_SAFETY) log.debug("Not processing token: processingState: {}", processingState);
      }

      if (multiProcessingStation.isIdle() && processingState == ProcessingState.PROCESSING) {
        log.info("Done processing, setting state to idle");
        processingState = ProcessingState.IDLE;
      }

      //retract vgr arms if one of them is idle!
      if (vacuumGripper1.isIdle()) {
        vacuumGripper1.retract_arm();
      }
      if (vacuumGripper2.isIdle()) {
        vacuumGripper2.retract_arm();
      }

      sleep(5);
    }

    log.info("Demo stopped");
  }

  @SuppressWarnings("SameParameterValue")
  private static void sleep(int millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      log.warn("Event loop was interrupted");
    }
  }

  enum ProcessingState {
    IDLE,
    DELIVERING_TOKEN,
    GOTO_SAFETY,
    PROCESSING,
  }
}
