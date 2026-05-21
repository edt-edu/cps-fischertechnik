package io.github.mbdo.factoryscada.service.dynamicmission;

import io.github.mbdo.factoryscada.core.enums.Color;
import io.github.mbdo.factoryscada.core.enums.DirectionKind;
import io.github.mbdo.factoryscada.core.enums.MPSOutput;
import io.github.mbdo.factoryscada.core.passable.NamedPosition;
import io.github.mbdo.factoryscada.domains.conveyorbelt.ConveyorBeltMachine;
import io.github.mbdo.factoryscada.domains.factoryscada.dtos.FactoryScadaInstance;
import io.github.mbdo.factoryscada.domains.multiprocessingstation.MultiProcessingStationMachine;
import io.github.mbdo.factoryscada.domains.sortingline.SortingLineMachine;
import io.github.mbdo.factoryscada.domains.vacuumgripper.VacuumGripperMachine;
import io.github.mbdo.factoryscada.service.FactoryScada;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DemoTest {

  private static final String SORTING_LINE_TOPIC = "I1SortingLine01";
  private static final String VGR2_TOPIC = "I1VacuumGripper02";
  private static final String CONVEYOR_TOPIC = "I1ConveyorBelt01";
  private static final String VGR1_TOPIC = "I1VacuumGripper01";
  private static final String MPS_TOPIC = "I1MultiProcessing01";

  private enum ProductionState {
    TOKEN_AT_SORTING_LINE_FEED,
    TOKEN_AT_SORTING_LINE_OUTPUT,
    VGR2_MOVING_TO_CONVEYOR_FEED,
    VGR2_RETRACTING_FROM_CONVEYOR_FEED,
    CONVEYOR_MOVING_TO_SWAP,
    VGR1_MOVING_FROM_FEED_TO_SWAP,
    VGR1_RETRACTING_FROM_SWAP,
    CONVEYOR_STUCK_AT_FEED,
    FALLBACK_MOVE_STUCK_AT_SWAP,
    VGR1_MOVING_TO_MPS,
    VGR1_RETRACTING_FROM_MPS,
    MPS_PROCESSING
  }

  @Test
  void simpleProductionRunExecutesEveryElementaryAction() throws InterruptedException {
    DemoFixture fixture = createDemoFixture();
    configureDefaultConveyorTransfer(fixture);

    // Start with a token at the sorting line feed.
    fixture.startDemoAndWaitForProcessing();

    // The token is sorted, transferred to the conveyor, buffered, moved to MPS, then processed.
    verifyCommonProductionRun(fixture);
    verify(fixture.conveyorBelt, atLeastOnce()).moveToSensor(DirectionKind.FORWARD);
  }

  @Test
  void simpleProductionRunWithBrokenConveyorUsesVacuumGripperForBufferTransfer() throws InterruptedException {
    DemoFixture fixture = createDemoFixture();
    fixture.demo.setCbBroken(true);
    configureBrokenConveyorTransfer(fixture);

    // Start with a token at the sorting line feed while the conveyor belt movement is unavailable.
    fixture.startDemoAndWaitForProcessing();

    // The token is sorted, transferred to the conveyor feed, carried to swap by VGR1, then processed by MPS.
    verifyCommonProductionRun(fixture);
    verify(fixture.vacuumGripper1, atLeastOnce()).retract_arm();
    verify(fixture.conveyorBelt, never()).moveToSensor(DirectionKind.FORWARD);
  }

  @Test
  void brokenConveyorMissionGetsStuckWhenFallbackFeedToSwapNeverFinishes() throws InterruptedException {
    DemoFixture fixture = createDemoFixture();
    configureDefaultConveyorTransfer(fixture);

    // First, start the normal demo through the service and run one token through the conveyor path.
    fixture.startMissionAndWaitForProcessing("Demo");
    verifyCommonProductionRun(fixture);
    verify(fixture.conveyorBelt, atLeastOnce()).moveToSensor(DirectionKind.FORWARD);
    fixture.clearMachineInvocations();

    fixture.prepareForNextToken();
    configureConveyorBreaksDuringFeedToSwap(fixture);

    // Next, start another token; it reaches the conveyor feed and the conveyor command never finishes.
    fixture.startMissionAndWaitForConveyorToGetStuck("Demo");

    configureBrokenConveyorTransferAfterStuckCommandDoesNotFinish(fixture);

    // Then stop the normal demo and start the broken-conveyor mission while the feed sensor is still blocked.
    fixture.service.stopActiveMission();
    fixture.service.startMissionByName("Broken CB Demo");

    // The broken-conveyor mission starts the fallback transfer from feed to swap.
    verify(fixture.vacuumGripper1, timeout(500).atLeastOnce())
        .move(eq(new NamedPosition("ALT_CB")), eq(new NamedPosition("CB")));

    // The fallback transfer never finishes, leaving VGR1 busy and preventing the final CB-to-MPS transfer.
    assertFalse(fixture.processingStarted.await(500, TimeUnit.MILLISECONDS), "The last token should remain stuck before MPS");
    verify(fixture.vacuumGripper1, never()).retract_arm();
    verify(fixture.vacuumGripper1, never())
        .move(eq(new NamedPosition("CB")), eq(new NamedPosition("MPS_INPUT")));
    verify(fixture.multiProcessingStation, never()).process(1, 1, MPSOutput.CONVEYOR);
    fixture.service.stopActiveMission();
  }

  private static DemoFixture createDemoFixture() {
    FactoryScada factoryScada = mock(FactoryScada.class);
    SortingLineMachine sortingLine = mock(SortingLineMachine.class);
    VacuumGripperMachine vacuumGripper2 = mock(VacuumGripperMachine.class);
    ConveyorBeltMachine conveyorBelt = mock(ConveyorBeltMachine.class);
    VacuumGripperMachine vacuumGripper1 = mock(VacuumGripperMachine.class);
    MultiProcessingStationMachine multiProcessingStation = mock(MultiProcessingStationMachine.class);
    Demo demo = new Demo(factoryScada);
    BrokenCBDemo brokenCBDemo = new BrokenCBDemo(demo);
    DynamicMissionService service = new DynamicMissionService(factoryScada, demo, brokenCBDemo);
    DemoFixture fixture = new DemoFixture(
        service,
        demo,
        sortingLine,
        vacuumGripper2,
        conveyorBelt,
        vacuumGripper1,
        multiProcessingStation
    );

    when(factoryScada.getFactoryScadaInstance()).thenReturn(new FactoryScadaInstance(
        Map.of(),
        Map.of(
            SORTING_LINE_TOPIC, sortingLine,
            VGR2_TOPIC, vacuumGripper2,
            CONVEYOR_TOPIC, conveyorBelt,
            VGR1_TOPIC, vacuumGripper1,
            MPS_TOPIC, multiProcessingStation
        )
    ));

    configureCommonProductionRun(fixture);
    return fixture;
  }

  private static void configureCommonProductionRun(DemoFixture fixture) {
    when(fixture.sortingLine.isTokenAtFeed()).thenAnswer(invocation ->
        fixture.state.get() == ProductionState.TOKEN_AT_SORTING_LINE_FEED
    );
    when(fixture.sortingLine.isTokenAtWhite()).thenAnswer(invocation ->
        fixture.state.get() == ProductionState.TOKEN_AT_SORTING_LINE_OUTPUT
    );
    when(fixture.sortingLine.isTokenAtRed()).thenReturn(false);
    when(fixture.sortingLine.isTokenAtBlue()).thenReturn(false);
    when(fixture.sortingLine.isIdle()).thenReturn(true);
    when(fixture.conveyorBelt.isTokenAtFeed()).thenAnswer(invocation ->
        fixture.state.get() == ProductionState.VGR2_RETRACTING_FROM_CONVEYOR_FEED
            || fixture.state.get() == ProductionState.CONVEYOR_STUCK_AT_FEED
    );
    when(fixture.conveyorBelt.isTokenAtSwap()).thenAnswer(invocation ->
        false
    );
    when(fixture.vacuumGripper2.isIdle()).thenAnswer(invocation ->
        fixture.state.get() != ProductionState.VGR2_MOVING_TO_CONVEYOR_FEED
            || fixture.vgr2MoveIdleChecks.getAndIncrement() > 0
    );
    when(fixture.vacuumGripper1.isIdle()).thenAnswer(invocation ->
        fixture.state.get() != ProductionState.VGR1_MOVING_TO_MPS
            || fixture.vgr1MoveToMpsIdleChecks.getAndIncrement() > 0
    );
    when(fixture.multiProcessingStation.isTokenAtFeed()).thenAnswer(invocation ->
        fixture.state.get() == ProductionState.VGR1_RETRACTING_FROM_MPS
    );
    when(fixture.multiProcessingStation.isIdle()).thenAnswer(invocation ->
        fixture.state.get() != ProductionState.MPS_PROCESSING
    );

    doAnswer(invocation -> {
      fixture.state.set(ProductionState.TOKEN_AT_SORTING_LINE_OUTPUT);
      return null;
    }).when(fixture.sortingLine).eject(Color.AUTO);
    doAnswer(invocation -> {
      fixture.state.set(ProductionState.VGR2_MOVING_TO_CONVEYOR_FEED);
      return null;
    }).when(fixture.vacuumGripper2).move(any(NamedPosition.class), any(NamedPosition.class));
    doAnswer(invocation -> {
      fixture.state.set(ProductionState.VGR2_RETRACTING_FROM_CONVEYOR_FEED);
      return null;
    }).when(fixture.vacuumGripper2).retract_arm();
    doAnswer(invocation -> {
      fixture.state.set(ProductionState.VGR1_MOVING_TO_MPS);
      return null;
    }).when(fixture.vacuumGripper1).move(any(NamedPosition.class), any(NamedPosition.class));
    doAnswer(invocation -> {
      fixture.state.set(ProductionState.VGR1_RETRACTING_FROM_MPS);
      return null;
    }).when(fixture.vacuumGripper1).go_to_safe_position();
    doAnswer(invocation -> {
      fixture.state.set(ProductionState.MPS_PROCESSING);
      fixture.processingStarted.countDown();
      fixture.demo.stop();
      return null;
    }).when(fixture.multiProcessingStation).process(1, 1, MPSOutput.CONVEYOR);
  }

  private static void configureDefaultConveyorTransfer(DemoFixture fixture) {
    when(fixture.conveyorBelt.isIdle()).thenAnswer(invocation ->
        fixture.state.get() != ProductionState.CONVEYOR_MOVING_TO_SWAP
            || fixture.conveyorMoveIdleChecks.getAndIncrement() > 0
    );
    doAnswer(invocation -> {
      fixture.state.set(ProductionState.CONVEYOR_MOVING_TO_SWAP);
      return null;
    }).when(fixture.conveyorBelt).moveToSensor(DirectionKind.FORWARD);
    when(fixture.conveyorBelt.isTokenAtSwap()).thenAnswer(invocation ->
        fixture.state.get() == ProductionState.CONVEYOR_MOVING_TO_SWAP
            && fixture.conveyorMoveIdleChecks.get() > 0
    );
  }

  private static void configureBrokenConveyorTransfer(DemoFixture fixture) {
    when(fixture.vacuumGripper1.isIdle()).thenAnswer(invocation -> switch (fixture.state.get()) {
      case VGR1_MOVING_FROM_FEED_TO_SWAP -> fixture.vgr1FeedToSwapIdleChecks.getAndIncrement() > 0;
      case VGR1_MOVING_TO_MPS -> fixture.vgr1MoveToMpsIdleChecks.getAndIncrement() > 0;
      default -> true;
    });
    doAnswer(invocation -> {
      if (fixture.state.get() == ProductionState.VGR2_RETRACTING_FROM_CONVEYOR_FEED) {
        fixture.state.set(ProductionState.VGR1_MOVING_FROM_FEED_TO_SWAP);
      } else {
        fixture.state.set(ProductionState.VGR1_MOVING_TO_MPS);
      }
      return null;
    }).when(fixture.vacuumGripper1).move(any(NamedPosition.class), any(NamedPosition.class));
    doAnswer(invocation -> {
      fixture.state.set(ProductionState.VGR1_RETRACTING_FROM_SWAP);
      return null;
    }).when(fixture.vacuumGripper1).retract_arm();
    when(fixture.conveyorBelt.isTokenAtSwap()).thenAnswer(invocation ->
        fixture.state.get() == ProductionState.VGR1_RETRACTING_FROM_SWAP
            && fixture.vgr1FeedToSwapIdleChecks.get() > 0
    );
  }

  private static void configureConveyorBreaksDuringFeedToSwap(DemoFixture fixture) {
    when(fixture.conveyorBelt.isIdle()).thenAnswer(invocation ->
        fixture.state.get() != ProductionState.CONVEYOR_STUCK_AT_FEED
    );
    doAnswer(invocation -> {
      fixture.state.set(ProductionState.CONVEYOR_STUCK_AT_FEED);
      fixture.conveyorStuck.countDown();
      return null;
    }).when(fixture.conveyorBelt).moveToSensor(DirectionKind.FORWARD);
    when(fixture.conveyorBelt.isTokenAtSwap()).thenAnswer(invocation ->
        false
    );
  }

  private static void configureBrokenConveyorTransferAfterStuckCommandDoesNotFinish(DemoFixture fixture) {
    when(fixture.vacuumGripper1.isIdle()).thenAnswer(invocation -> switch (fixture.state.get()) {
      case FALLBACK_MOVE_STUCK_AT_SWAP -> false;
      case VGR1_MOVING_TO_MPS -> fixture.vgr1MoveToMpsIdleChecks.getAndIncrement() > 0;
      default -> true;
    });
    doAnswer(invocation -> {
      if (fixture.state.get() == ProductionState.CONVEYOR_STUCK_AT_FEED) {
        fixture.state.set(ProductionState.FALLBACK_MOVE_STUCK_AT_SWAP);
      } else {
        fixture.state.set(ProductionState.VGR1_MOVING_TO_MPS);
      }
      return null;
    }).when(fixture.vacuumGripper1).move(any(NamedPosition.class), any(NamedPosition.class));
    doAnswer(invocation -> {
      fixture.state.set(ProductionState.VGR1_RETRACTING_FROM_SWAP);
      return null;
    }).when(fixture.vacuumGripper1).retract_arm();
    when(fixture.conveyorBelt.isTokenAtSwap()).thenAnswer(invocation ->
        fixture.state.get() == ProductionState.FALLBACK_MOVE_STUCK_AT_SWAP
            || fixture.state.get() == ProductionState.VGR1_RETRACTING_FROM_SWAP
            && fixture.vgr1FeedToSwapIdleChecks.get() > 0
    );
  }

  private static void verifyCommonProductionRun(DemoFixture fixture) {
    verify(fixture.sortingLine, atLeastOnce()).eject(Color.AUTO);
    verify(fixture.vacuumGripper2, atLeastOnce()).move(any(NamedPosition.class), any(NamedPosition.class));
    verify(fixture.vacuumGripper2, atLeastOnce()).retract_arm();
    verify(fixture.vacuumGripper1, atLeastOnce()).move(any(NamedPosition.class), any(NamedPosition.class));
    verify(fixture.multiProcessingStation, atLeastOnce()).setup();
    verify(fixture.vacuumGripper1, atLeastOnce()).go_to_safe_position();
    verify(fixture.multiProcessingStation, atLeastOnce()).process(1, 1, MPSOutput.CONVEYOR);
  }

  private static class DemoFixture {
    private final DynamicMissionService service;
    private final Demo demo;
    private final SortingLineMachine sortingLine;
    private final VacuumGripperMachine vacuumGripper2;
    private final ConveyorBeltMachine conveyorBelt;
    private final VacuumGripperMachine vacuumGripper1;
    private final MultiProcessingStationMachine multiProcessingStation;
    private final AtomicReference<ProductionState> state =
        new AtomicReference<>(ProductionState.TOKEN_AT_SORTING_LINE_FEED);
    private final AtomicInteger vgr2MoveIdleChecks = new AtomicInteger();
    private final AtomicInteger conveyorMoveIdleChecks = new AtomicInteger();
    private final AtomicInteger vgr1FeedToSwapIdleChecks = new AtomicInteger();
    private final AtomicInteger vgr1MoveToMpsIdleChecks = new AtomicInteger();
    private CountDownLatch processingStarted = new CountDownLatch(1);
    private final CountDownLatch conveyorStuck = new CountDownLatch(1);

    private DemoFixture(
        DynamicMissionService service,
        Demo demo,
        SortingLineMachine sortingLine,
        VacuumGripperMachine vacuumGripper2,
        ConveyorBeltMachine conveyorBelt,
        VacuumGripperMachine vacuumGripper1,
        MultiProcessingStationMachine multiProcessingStation
    ) {
      this.service = service;
      this.demo = demo;
      this.sortingLine = sortingLine;
      this.vacuumGripper2 = vacuumGripper2;
      this.conveyorBelt = conveyorBelt;
      this.vacuumGripper1 = vacuumGripper1;
      this.multiProcessingStation = multiProcessingStation;
    }

    private void startDemoAndWaitForProcessing() throws InterruptedException {
      demo.start();

      assertTrue(processingStarted.await(2, TimeUnit.SECONDS), "The demo should process one token");
      demo.stop();
    }

    private void startDemoAndWaitForConveyorToGetStuck() throws InterruptedException {
      demo.start();

      assertTrue(conveyorStuck.await(2, TimeUnit.SECONDS), "The conveyor should start a command that never finishes");
      demo.stop();
    }

    private void startMissionAndWaitForProcessing(String missionName) throws InterruptedException {
      service.startMissionByName(missionName);

      assertTrue(processingStarted.await(2, TimeUnit.SECONDS), "The mission should process one token");
      service.stopActiveMission();
    }

    private void startMissionAndWaitForConveyorToGetStuck(String missionName) throws InterruptedException {
      service.startMissionByName(missionName);

      assertTrue(conveyorStuck.await(2, TimeUnit.SECONDS), "The conveyor should start a command that never finishes");
    }

    private void prepareForNextToken() {
      state.set(ProductionState.TOKEN_AT_SORTING_LINE_FEED);
      vgr2MoveIdleChecks.set(0);
      conveyorMoveIdleChecks.set(0);
      vgr1FeedToSwapIdleChecks.set(0);
      vgr1MoveToMpsIdleChecks.set(0);
      processingStarted = new CountDownLatch(1);
    }

    private void clearMachineInvocations() {
      clearInvocations(
          sortingLine,
          vacuumGripper2,
          conveyorBelt,
          vacuumGripper1,
          multiProcessingStation
      );
    }
  }
}
