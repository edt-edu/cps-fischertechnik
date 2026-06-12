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
  private static final int POLL_DELAY_MILLIS = 100;

  private final FactoryScada factoryScada;
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

  private void setup() {
    //machines
    //TODO extract machine names into config
    this.sortingLine = getMachine(SortingLineMachine.class, SORTING_LINE_TOPIC);
    this.vacuumGripper2 = getMachine(VacuumGripperMachine.class, VGR2_TOPIC);
    this.conveyorBelt = getMachine(ConveyorBeltMachine.class, CONVEYOR_TOPIC);
    this.vacuumGripper1 = getMachine(VacuumGripperMachine.class, VGR1_TOPIC);
    this.multiProcessingStation = getMachine(MultiProcessingStationMachine.class, MPS_TOPIC);

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
  }

  private void process() {
    if (multiProcessingStation.isTokenAtFeed() &&
        !mpsActive &&
        !mpsInputLocked) {
      log.info("Processing token");
      mpsActive = true;
      mpsInputLocked = true;
      multiProcessingStation.process(1, 1, MPSOutput.CONVEYOR);
    } else {
      if (!multiProcessingStation.isTokenAtFeed()) log.debug("Not processing token: MPS feed empty");
      if (mpsActive) log.debug("Not processing token: MPS active");
      if (mpsInputLocked) log.debug("Not processing token: MPS input locked");
    }

    //cleanup
    if (mpsActive && multiProcessingStation.isIdle()) {
      log.info("[MPS/process] Releasing mpsInputLock");
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
      log.info("Moving token from CB to MPS");
      mpsInputLocked = true;
      cbSwapLocked = true;
      vgr1Activity = VGR1Activity.MOVE_FROM_CB_TO_MPS;
      vacuumGripper1.move(new NamedPosition("CB"), new NamedPosition("MPS_INPUT"));
      multiProcessingStation.setup(); //ensure the mps is in a state where we can actually place the token
    } else {
      if (!conveyorBelt.isTokenAtSwap()) log.debug("Not moving from CB to MPS: CB swap empty");
      if (vgr1Activity != VGR1Activity.NONE) log.debug("Not moving from CB to MPS: VGR1 activity: {}", vgr1Activity);
      if (mpsActive) log.debug("Not moving from CB to MPS: MPS active");
      if (mpsInputLocked) log.debug("Not moving from CB to MPS: MPS input locked");
      if (cbSwapLocked) log.debug("Not moving from CB to MPS: CB swap locked");
    }

    //cleanup
    if (vgr1Activity == VGR1Activity.MOVE_FROM_CB_TO_MPS && vacuumGripper1.isIdle()) {
      log.info("[VGR1/cb2mps] Releasing cbSwapLock");
      cbSwapLocked = false;
      vgr1Activity = VGR1Activity.RETRACT_FROM_MPS;
      vacuumGripper1.go_to_safe_position();
    } else if (vgr1Activity == VGR1Activity.RETRACT_FROM_MPS && vacuumGripper1.isIdle()) {
      log.info("[VGR1/cb2mps] Releasing mpsInputLock");
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
        log.info("Moving token from feed to swap");
        cbFeedLocked = true;
        cbSwapLocked = true;
        cbActive = true;
        conveyorBelt.moveToSensor(DirectionKind.FORWARD);
      } else {
        log.debug("CB is broken");
        if (vgr1Activity == VGR1Activity.NONE) {
          log.info("Moving token from feed to swap with VGR1");
          cbFeedLocked = true;
          cbSwapLocked = true;
          vgr1Activity = VGR1Activity.MOVE_FROM_FEED_TO_SWAP;
          vacuumGripper1.move(new NamedPosition("ALT_CB"), new NamedPosition("CB"));
        } else if (vgr2Activity == VGR2Activity.NONE) {
          log.info("Moving token from feed to swap with VGR2");
          cbFeedLocked = true;
          cbSwapLocked = true;
          vgr2Activity = VGR2Activity.MOVE_FROM_FEED_TO_SWAP;
          vacuumGripper2.move(new NamedPosition("CB"), new NamedPosition("ALT_CB"));
        } else {
          log.debug("Not moving from feed to swap: VGRs busy");
        }
      }
    } else {
      if (cbActive) log.debug("Not moving from feed to swap: CB active");
      if (!conveyorBelt.isTokenAtFeed()) log.debug("Not moving from feed to swap: CB feed empty");
      if (conveyorBelt.isTokenAtSwap()) log.debug("Not moving from feed to swap: CB swap occupied");
      if (cbFeedLocked) log.debug("Not moving from feed to swap: CB feed locked");
      if (cbSwapLocked) log.debug("Not moving from feed to swap: CB swap locked");
    }

    //cleanup
    if (cbActive && conveyorBelt.isIdle()) {
      log.info("[CB/feed2swap] Releasing CB locks");
      cbActive = false;
      cbSwapLocked = false;
      cbFeedLocked = false;
    } else if (vgr1Activity == VGR1Activity.MOVE_FROM_FEED_TO_SWAP && vacuumGripper1.isIdle()) {
      log.info("[VGR1/feed2swap] Releasing cbFeedLock");
      cbFeedLocked = false;
      vgr1Activity = VGR1Activity.RETRACT_FROM_SWAP;
      vacuumGripper1.retract_arm();
    } else if (vgr2Activity == VGR2Activity.MOVE_FROM_FEED_TO_SWAP && vacuumGripper2.isIdle()) {
      log.info("[VGR2/feed2swap] Releasing cbFeedLock");
      cbFeedLocked = false;
      vgr2Activity = VGR2Activity.RETRACT_FROM_SWAP;
      vacuumGripper2.retract_arm();
    } else if (vgr1Activity == VGR1Activity.RETRACT_FROM_SWAP && vacuumGripper1.isIdle()) {
      log.info("[VGR1/feed2swap] Releasing cbSwapLock");
      cbSwapLocked = false;
      vgr1Activity = VGR1Activity.NONE;
    } else if (vgr2Activity == VGR2Activity.RETRACT_FROM_SWAP && vacuumGripper2.isIdle()) {
      log.info("[VGR2/feed2swap] Releasing cbSwapLock");
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
        log.info("Moving token from {} to CB", originName);
        cbFeedLocked = true;
        vgr2Activity = VGR2Activity.MOVE_FROM_SL_TO_CB;
        vacuumGripper2.move(new NamedPosition(originName), new NamedPosition("CB"));
      } else {
        log.debug("Not moving from SL to CB: No token at SL output");
      }
    } else {
      if (vgr2Activity != VGR2Activity.NONE) log.debug("Not moving from SL to CB: VGR2 activity: {}", vgr2Activity);
      if (cbActive) log.debug("Not moving from SL to CB: CB active");
      if (conveyorBelt.isTokenAtFeed()) log.debug("Not moving from SL to CB: CB feed occupied");
      if (cbFeedLocked) log.debug("Not moving from SL to CB: CB feed locked");
    }

    //cleanup
    if (vgr2Activity == VGR2Activity.MOVE_FROM_SL_TO_CB && vacuumGripper2.isIdle()) {
      vgr2Activity = VGR2Activity.RETRACT_FROM_FEED;
      vacuumGripper2.retract_arm();
    } else if (vgr2Activity == VGR2Activity.RETRACT_FROM_FEED && vacuumGripper2.isIdle()) {
      log.info("[VGR2/sl2cb] Releasing cbFeedLock");
      vgr2Activity = VGR2Activity.NONE;
      cbFeedLocked = false;
    }
  }

  private void sortToken() {
    if (sortingLine.isTokenAtFeed() && sortingLine.isIdle()) {
      log.info("Sorting token");
      sortingLine.eject(Color.AUTO);
    } else {
      if (!sortingLine.isTokenAtFeed()) log.debug("Not sorting token: No token at SL input");
      if (!sortingLine.isIdle()) log.debug("Not sorting token: SL busy");
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
