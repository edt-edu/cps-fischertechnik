package com.example.generated.testmachinemissions;

import java.util.List;

import com.example.generated.testmachinesystem.testmachine.TestMachineMachine;
import com.example.generated.testmachinesystem.testmachinemessages.TestMachineAttAAbove10EventMessage;
import com.example.generated.testmachinesystem.testmachinemessages.CommandSuccessEventMessage;
import com.example.generated.testmachinesystem.testmachinemessages.EventMessage1;
import com.example.generated.testmachinesystem.testmachinemessages.EventMessage2;
import com.example.runtime.rtc.def.RuntimeState;
import com.example.runtime.rtc.def.RuntimeTransition;
import com.example.runtime.rtc.event.CompletionEvent;
import com.example.runtime.rtc.event.Event;
import com.example.runtime.rtc.exec.*;
// runtime scheduler not required for missions after adapter-queue refactor
import fr.inria.mbdo.mission.runtime.api.MachineMissionStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * From TestMachineMissions::TestMissionAlpha
 */
public class TestMissionAlpha implements MachineMissionStrategy {
  private final TestMachineMachine testMachine1;
  private final TestMachineMachine testMachine2;
  private final RuntimeInstance runtime;
  private static final Logger logger = LoggerFactory.getLogger(TestMissionAlpha.class);

  public TestMissionAlpha(TestMachineMachine testMachine1, TestMachineMachine testMachine2) {
    this.testMachine1 = testMachine1;
    this.testMachine2 = testMachine2;

    // Actions
    RuntimeAction entryInit = evt -> testMachine1.actionA();
    RuntimeAction sendEventMessage1 = evt -> testMachine2.publish(EventMessage1.now());

    // Guards
    // State refs
    StateBox setupRef = new StateBox();
    StateBox idleRef = new StateBox();
    StateBox waitingRef = new StateBox();

    RuntimeTransition initToSetup = new RuntimeTransition(
      CompletionEvent.class,
      RuntimeGuards.always(),
      entryInit,
      () -> setupRef.get());

    RuntimeTransition setupToIdle = new RuntimeTransition(
      CommandSuccessEventMessage.class,
      RuntimeGuards.always(),
      RuntimeActions.noop(),
      () -> idleRef.get());

    RuntimeTransition idleToWaiting = new RuntimeTransition(
      TestMachineAttAAbove10EventMessage.class,
      RuntimeGuards.always(),
      sendEventMessage1,
      () -> waitingRef.get());

    RuntimeTransition waitingToIdle = new RuntimeTransition(
      EventMessage2.class,
      RuntimeGuards.always(),
      RuntimeActions.noop(),
      () -> idleRef.get());

    RuntimeState init = new RuntimeState("Init", List.of(initToSetup), List.of(), List.of(), false);
    RuntimeState setup = new RuntimeState("Setup", List.of(setupToIdle), List.of(), List.of(), false);
    RuntimeState idle = new RuntimeState("Idle", List.of(idleToWaiting), List.of(), List.of(), false);
    RuntimeState waiting = new RuntimeState("WaitingForB", List.of(waitingToIdle), List.of(), List.of(), false);

    setupRef.set(setup);
    idleRef.set(idle);
    waitingRef.set(waiting);

    this.runtime = new RuntimeInstance(new SimpleRuntimeDefinition(init));
    testMachine1.subscribe(CommandSuccessEventMessage.class, this::onEvent);
    testMachine1.subscribe(TestMachineAttAAbove10EventMessage.class, this::onEvent);
    testMachine2.subscribe(EventMessage2.class, this::onEvent);
    logger.info("TestMissionAlpha initialized and subscribed to events");
  }

  public void start() {
    logger.info("TestMissionAlpha starting");
    runtime.start();
  }

  public void onEvent(Event event) {
    logger.info("TestMissionAlpha received event {}", event.getClass().getSimpleName());
    runtime.dispatch(event);
  }

  public String getActiveStateName() {
    return runtime.activeState().name();
  }

  private static final class StateBox {
    private RuntimeState state;
    void set(RuntimeState s) { this.state = s; }
    RuntimeState get() { return state; }
  }
}
