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
import io.github.mbdo.factoryscada.mqtt.MqttPublisherService;
import io.github.mbdo.factoryscada.service.FactoryScada;
import io.github.mbdo.factoryscada.service.MachineNameMappingService;
import io.github.mbdo.factoryscada.utilities.DistinctDebugLogger;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Getter
public class Demo implements DynamicMission {
  private static final String BASE_LOGICALNAME = "DynamicMission/Demo/";
  private static final String SORTING_LINE_LOGICALNAME = BASE_LOGICALNAME+"SL";
  private static final String VGR2_LOGICALNAME = BASE_LOGICALNAME+"VGR2";
  private static final String CONVEYOR_LOGICALNAME = BASE_LOGICALNAME+"CB";
  private static final String VGR1_LOGICALNAME = BASE_LOGICALNAME+"VGR1";
  private static final String MPS_LOGICALNAME = BASE_LOGICALNAME+"MPS";
  private static final int POLL_DELAY_MILLIS = 100;

  private final FactoryScada factoryScada;
  private final MachineNameMappingService machineNameMapping;
  private volatile boolean active = false;
  @Setter
  private volatile boolean cbBroken = false;

  //machines
  private SortingLineMachine sortingLine;
  private VacuumGripperMachine vacuumGripper2;
  private ConveyorBeltMachine conveyorBelt;
  private VacuumGripperMachine vacuumGripper1;
  private MultiProcessingStationMachine multiProcessingStation;

  //locks
  private boolean cbFeedLocked;
  private boolean cbSwapLocked;
  private boolean mpsInputLocked;

  //activity
  private VGR2Activity vgr2Activity;
  private VGR1Activity vgr1Activity;
  private boolean cbActive;
  private boolean mpsActive;

  //smart loggers (one for each verbose activity)
  private final DistinctDebugLogger sortTokenLogger;
  private final DistinctDebugLogger moveFromSLtoCBLogger;
  private final DistinctDebugLogger moveFromFeedToSwapLogger;
  private final DistinctDebugLogger moveTokenToMpsLogger;
  private final DistinctDebugLogger processLogger;
  private final DistinctDebugLogger dumpStateLogger;

  private final MqttPublisherService mqttPublisher;

  private enum VGR2Activity {
    MOVE_FROM_SL_TO_CB,
    MOVE_FROM_FEED_TO_SWAP,
    RETRACT_FROM_FEED,
    RETRACT_FROM_SWAP,
    NONE,
  }

  private enum VGR1Activity {
    MOVE_FROM_CB_TO_MPS,
    MOVE_FROM_FEED_TO_SWAP,
    RETRACT_FROM_FEED,
    RETRACT_FROM_SWAP,
    RETRACT_FROM_MPS,
    NONE,
  }

  @Autowired
  public Demo(FactoryScada factoryScada, MachineNameMappingService machineNameMapping, MqttPublisherService mqttPublisher) {
      this.factoryScada = factoryScada;
      this.machineNameMapping = machineNameMapping;
      this.mqttPublisher = mqttPublisher;
      this.sortTokenLogger = new DistinctDebugLogger(log);
      this.moveFromSLtoCBLogger = new DistinctDebugLogger(log);
      this.moveTokenToMpsLogger = new DistinctDebugLogger(log);
      this.processLogger = new DistinctDebugLogger(log);
      this.moveFromFeedToSwapLogger = new DistinctDebugLogger(log);
      this.dumpStateLogger = new DistinctDebugLogger(log);


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
    List<String> logicalNames = List.of(SORTING_LINE_LOGICALNAME, VGR2_LOGICALNAME, CONVEYOR_LOGICALNAME, VGR1_LOGICALNAME, MPS_LOGICALNAME);
    List<String> machineNames = logicalNames.stream().map(machineNameMapping::getMachineForLogicalName).toList();

    log.info("Demo mission machine name mapping: \n   {}", logicalNames.stream().map(ln -> ln + "->"+machineNameMapping.getMachineForLogicalName(ln)).collect(Collectors.joining("\n   ")));
    return machineNames;
  }

  @Override
  public synchronized void start() {
    if (active) return;

    active = true;
    new Thread(this::run).start();
  }

  @Override
  public void stop() {
    if (active) {
      active = false;
      dumpState();
    }
  }

  private void setup() {
    //machines
    //TODO extract machine names into config
    this.sortingLine = getMachine(SortingLineMachine.class, machineNameMapping.getMachineForLogicalName(SORTING_LINE_LOGICALNAME));
    this.vacuumGripper2 = getMachine(VacuumGripperMachine.class, machineNameMapping.getMachineForLogicalName(VGR2_LOGICALNAME));
    this.conveyorBelt = getMachine(ConveyorBeltMachine.class, machineNameMapping.getMachineForLogicalName(CONVEYOR_LOGICALNAME));
    this.vacuumGripper1 = getMachine(VacuumGripperMachine.class, machineNameMapping.getMachineForLogicalName(VGR1_LOGICALNAME));
    this.multiProcessingStation = getMachine(MultiProcessingStationMachine.class, machineNameMapping.getMachineForLogicalName(MPS_LOGICALNAME));

    //locks
    cbFeedLocked = false;
    cbSwapLocked = false;
    mpsInputLocked = false;

    //tasks
    vgr1Activity = VGR1Activity.NONE;
    vgr2Activity = VGR2Activity.NONE;
    cbActive = false;
    mpsActive = false;
  }

  private void run() {
    log.info("Starting Demo");

    setup();

    while (active) {
      sortToken();
      moveFromSLtoCB();
      moveFromFeedToSwap();
      moveTokenToMps();
      process();
      dumpState();

      sleep(POLL_DELAY_MILLIS);
    }

    log.info("Demo stopped");
  }

  Map<String, Object> previousState = null;
  private void dumpState() {
    if (sortingLine.isIdle() && conveyorBelt.isIdle() && vacuumGripper1.isIdle() && vacuumGripper2.isIdle() && multiProcessingStation.isIdle()) {
      //locks
      if (cbFeedLocked || cbSwapLocked || mpsInputLocked) {
        log.warn("All machines are idle but some positions are still locked:");
        log.warn("CB feed locked: {}", cbFeedLocked);
        log.warn("CB swap locked: {}", cbSwapLocked);
        log.warn("MPS input locked: {}", mpsInputLocked);
      }

      //activity
      if (vgr1Activity != VGR1Activity.NONE || vgr2Activity != VGR2Activity.NONE || cbActive || mpsActive) {
        log.warn("All machines are idle but some activities are still not over:");
        log.warn("VGR1 activity: {}", vgr1Activity);
        log.warn("VGR2 activity: {}", vgr2Activity);
        log.warn("CB active: {}", cbActive);
        log.warn("MPS active: {}", mpsActive);
      }
    }
    Map<String, Object> newState = buildState();
    if(!newState.equals(previousState)) {
      previousState = newState;
      mqttPublisher.publish("internal/dynamicmission/Demo/state", newState);
    }
  }

  /**
   * build a serializable map with all attributes used by the various functions
   * @return a serializable map
   */
  private Map<String, Object> buildState() {
    Map<String, Object> state = new LinkedHashMap<>();
    state.put("active", active);
    state.put("slIsTokenAtFeed", sortingLine.isTokenAtFeed());
    state.put("slIsIdle", sortingLine.isIdle());
    state.put("slIsTokenAtWhite", sortingLine.isTokenAtWhite());
    state.put("slIsTokenAtBlue", sortingLine.isTokenAtBlue());
    state.put("slIsTokenAtRed", sortingLine.isTokenAtRed());
    state.put("cbActive", cbActive);
    state.put("cbBroken", cbBroken);
    state.put("cbIsIdle", conveyorBelt.isIdle());
    state.put("cbFeedLocked", cbFeedLocked);
    state.put("cbSwapLocked", cbSwapLocked);
    state.put("cbIsTokenAtSwap", conveyorBelt.isTokenAtSwap());
    state.put("cbIsTokenAtFeed", conveyorBelt.isTokenAtFeed());
    state.put("mpsActive", mpsActive);
    state.put("mpsIsIdle", multiProcessingStation.isIdle());
    state.put("mpsInputLocked", mpsInputLocked);
    state.put("mpsIsTokenAtFeed", multiProcessingStation.isTokenAtFeed());
    state.put("vgr1Activity", vgr1Activity);
    state.put("vgr1IsIdle",vacuumGripper1.isIdle());
    state.put("vgr2Activity", vgr2Activity);
    state.put("vgr2IsIdle",vacuumGripper2.isIdle());

    return state;
  }
  private void process() {
    if (multiProcessingStation.isTokenAtFeed() &&
        !mpsActive &&
        !mpsInputLocked) {
      processLogger.info("MPS Processing token");
      mpsActive = true;
      mpsInputLocked = true;
      multiProcessingStation.process(1, 1, MPSOutput.CONVEYOR);
    } else {
      if(log.isDebugEnabled()) {
        List<String> reasons = new ArrayList<>();
        if (!multiProcessingStation.isTokenAtFeed()) reasons.add("MPS feed empty");
        if (mpsActive) reasons.add("MPS active");
        if (mpsInputLocked) reasons.add("MPS input locked");
        processLogger.debug("MPS Not processing token: {}", String.join(", ", reasons));
      }
    }

    //cleanup
    if (mpsActive && multiProcessingStation.isIdle()) {
      processLogger.info("[MPS/process] Releasing mpsInputLock");
      mpsActive = false;
      mpsInputLocked = false;
    }
  }

  private void moveTokenToMps() {
    //move token from cb swap to mps
    if (conveyorBelt.isTokenAtSwap() &&
        !multiProcessingStation.isTokenAtFeed() &&
        vgr1Activity == VGR1Activity.NONE &&
        !mpsActive &&
        !mpsInputLocked &&
        !cbSwapLocked) {
      moveTokenToMpsLogger.info("Moving token from CB to MPS");
      mpsInputLocked = true;
      cbSwapLocked = true;
      vgr1Activity = VGR1Activity.MOVE_FROM_CB_TO_MPS;
      vacuumGripper1.move(new NamedPosition("CB"), new NamedPosition("MPS_INPUT"));
      multiProcessingStation.setup(); //ensure the mps is in a state where we can actually place the token
    } else {
      if(log.isDebugEnabled()) {
        List<String> reasons = new ArrayList<>();
        if (!conveyorBelt.isTokenAtSwap()) reasons.add("CB swap empty");
        if (vgr1Activity != VGR1Activity.NONE) reasons.add("VGR1 activity=" + vgr1Activity);
        if (mpsActive) reasons.add("MPS active");
        if (mpsInputLocked) reasons.add("MPS input locked");
        if (cbSwapLocked) reasons.add("CB swap locked");
        moveTokenToMpsLogger.debug("VGR1 Not moving from CB to MPS: {}", String.join(", ", reasons));
      }
    }

    //cleanup
    if (vgr1Activity == VGR1Activity.MOVE_FROM_CB_TO_MPS && vacuumGripper1.isIdle()) {
      moveTokenToMpsLogger.info("[VGR1/cb2mps] Releasing cbSwapLock");
      cbSwapLocked = false;
      vgr1Activity = VGR1Activity.RETRACT_FROM_MPS;
      vacuumGripper1.go_to_safe_position();
    } else if (vgr1Activity == VGR1Activity.RETRACT_FROM_MPS && vacuumGripper1.isIdle()) {
      moveTokenToMpsLogger.info("[VGR1/cb2mps] Releasing mpsInputLock");
      vgr1Activity = VGR1Activity.NONE;
      mpsInputLocked = false;
    }
  }

  private void moveFromFeedToSwap() {
    //move token to swap on cb
    if (!cbActive &&
        conveyorBelt.isTokenAtFeed() &&
        !conveyorBelt.isTokenAtSwap() &&
        !cbFeedLocked &&
        !cbSwapLocked) {
      if (!cbBroken) {
        moveFromFeedToSwapLogger.info("Moving token from feed to swap");
        cbFeedLocked = true;
        cbSwapLocked = true;
        cbActive = true;
        conveyorBelt.moveToSensor(DirectionKind.FORWARD);
      } else {
        log.debug("CB is broken");
        if (vgr1Activity == VGR1Activity.NONE) {
          moveFromFeedToSwapLogger.info("Moving token from feed to swap with VGR1");
          cbFeedLocked = true;
          cbSwapLocked = true;
          vgr1Activity = VGR1Activity.MOVE_FROM_FEED_TO_SWAP;
          vacuumGripper1.move(new NamedPosition("ALT_CB"), new NamedPosition("CB"));
        } else if (vgr2Activity == VGR2Activity.NONE) {
          moveFromFeedToSwapLogger.info("Moving token from feed to swap with VGR2");
          cbFeedLocked = true;
          cbSwapLocked = true;
          vgr2Activity = VGR2Activity.MOVE_FROM_FEED_TO_SWAP;
          vacuumGripper2.move(new NamedPosition("CB"), new NamedPosition("ALT_CB"));
        } else {
          moveFromFeedToSwapLogger.debug("Not moving from feed to swap: VGRs busy");
        }
      }
    } else {
      if(log.isDebugEnabled()) {
        List<String> reasons = new ArrayList<>();
        if (cbActive) reasons.add("CB active");
        if (!conveyorBelt.isTokenAtFeed()) reasons.add("CB feed empty");
        if (conveyorBelt.isTokenAtSwap()) reasons.add("CB swap occupied");
        if (cbFeedLocked) reasons.add("CB feed locked");
        if (cbSwapLocked) reasons.add("CB swap locked");
        moveFromFeedToSwapLogger.debug("CB Not moving from feed to swap: {}", String.join(", ", reasons));
      }
    }

    //cleanup
    if (cbActive && conveyorBelt.isIdle()) {
      moveFromFeedToSwapLogger.info("[CB/feed2swap] Releasing CB locks");
      cbActive = false;
      cbSwapLocked = false;
      cbFeedLocked = false;
    } else if (vgr1Activity == VGR1Activity.MOVE_FROM_FEED_TO_SWAP && vacuumGripper1.isIdle()) {
      moveFromFeedToSwapLogger.info("[VGR1/feed2swap] Releasing cbFeedLock");
      cbFeedLocked = false;
      vgr1Activity = VGR1Activity.RETRACT_FROM_SWAP;
      vacuumGripper1.retract_arm();
    } else if (vgr2Activity == VGR2Activity.MOVE_FROM_FEED_TO_SWAP && vacuumGripper2.isIdle()) {
      moveFromFeedToSwapLogger.info("[VGR2/feed2swap] Releasing cbFeedLock");
      cbFeedLocked = false;
      vgr2Activity = VGR2Activity.RETRACT_FROM_SWAP;
      vacuumGripper2.retract_arm();
    } else if (vgr1Activity == VGR1Activity.RETRACT_FROM_SWAP && vacuumGripper1.isIdle()) {
      moveFromFeedToSwapLogger.info("[VGR1/feed2swap] Releasing cbSwapLock");
      cbSwapLocked = false;
      vgr1Activity = VGR1Activity.NONE;
    } else if (vgr2Activity == VGR2Activity.RETRACT_FROM_SWAP && vacuumGripper2.isIdle()) {
      moveFromFeedToSwapLogger.info("[VGR2/feed2swap] Releasing cbSwapLock");
      cbSwapLocked = false;
      vgr2Activity = VGR2Activity.NONE;
    }
  }

  private void moveFromSLtoCB() {
    //move token from sl out to cb if there is room
    if (vgr2Activity == VGR2Activity.NONE && !cbActive && !conveyorBelt.isTokenAtFeed() && !cbFeedLocked) {
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
        moveFromSLtoCBLogger.info("Moving token from {} to CB", originName);
        cbFeedLocked = true;
        vgr2Activity = VGR2Activity.MOVE_FROM_SL_TO_CB;
        vacuumGripper2.move(new NamedPosition(originName), new NamedPosition("CB"));
      } else {
        moveFromSLtoCBLogger.debug("Not moving from SL to CB: No token at SL output");
      }
    } else {
      if(log.isDebugEnabled()) {
        List<String> reasons = new ArrayList<>();
        if (vgr2Activity != VGR2Activity.NONE) reasons.add("VGR2 activity="+ vgr2Activity);
        if (cbActive) reasons.add("CB active");
        if (conveyorBelt.isTokenAtFeed()) reasons.add("CB feed occupied");
        if (cbFeedLocked) reasons.add("CB feed locked");
        moveFromSLtoCBLogger.debug("VGR2 Not moving from SL to CB: {}", String.join(", ", reasons));
      }
    }

    //cleanup
    if (vgr2Activity == VGR2Activity.MOVE_FROM_SL_TO_CB && vacuumGripper2.isIdle()) {
      vgr2Activity = VGR2Activity.RETRACT_FROM_FEED;
      vacuumGripper2.retract_arm();
    } else if (vgr2Activity == VGR2Activity.RETRACT_FROM_FEED && vacuumGripper2.isIdle()) {
      moveFromSLtoCBLogger.info("[VGR2/sl2cb] Releasing cbFeedLock");
      vgr2Activity = VGR2Activity.NONE;
      cbFeedLocked = false;
    }
  }

  private void sortToken() {
    if (sortingLine.isTokenAtFeed() && sortingLine.isIdle()) {
      sortTokenLogger.info("Sorting token");
      sortingLine.eject(Color.AUTO);
    } else {
      if(log.isDebugEnabled()) {
        List<String> reasons = new ArrayList<>();
        if (!sortingLine.isTokenAtFeed()) reasons.add("No token at SL input");
        if (!sortingLine.isIdle()) reasons.add("SL busy");
        sortTokenLogger.debug("SL Not sorting token: {}", String.join(", ", reasons));
      }
    }
  }

  @SuppressWarnings("SameParameterValue") //needed since we always sleep for the same amount of millis
  private static void sleep(int millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      log.warn("Event loop was interrupted", e);
    }
  }
}
