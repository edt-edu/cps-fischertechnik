package com.example.generated.testmachinemissions;

import java.util.List;

import com.example.generated.testmachinesystem.testmachine.TestMachineMachine;
import com.example.generated.testmachinesystem.testmachinemessages.EventMessage2;
import com.example.generated.testmachinesystem.testmachinemessages.CommandSuccessEventMessage;
import com.example.generated.testmachinesystem.testmachinemessages.EventMessage1;
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
 * From TestMachineMissions::TestMissionBeta
 */
public class TestMissionBeta implements MachineMissionStrategy {
  private final TestMachineMachine testMachine1;
  private final TestMachineMachine testMachine2;
  private final RuntimeInstance runtime;
  private static final Logger logger = LoggerFactory.getLogger(TestMissionBeta.class);

  public TestMissionBeta(TestMachineMachine testMachine1, TestMachineMachine testMachine2) {
    this.testMachine1 = testMachine1;
    this.testMachine2 = testMachine2;

    // Actions
    RuntimeAction testAction = evt -> testMachine1.actionB();
    RuntimeAction sendEvent2 = evt -> testMachine1.publish(EventMessage2.now());

    // State refs
    StateBox standbyRef = new StateBox();
    StateBox waitingRef = new StateBox();

    RuntimeTransition initToStandby = new RuntimeTransition(
      CompletionEvent.class, RuntimeGuards.always(), RuntimeActions.noop(), () -> standbyRef.get());

    RuntimeTransition standbyToWaiting = new RuntimeTransition(
        EventMessage1.class, RuntimeGuards.always(), testAction, () -> waitingRef.get());

    RuntimeTransition waitingToStandby = new RuntimeTransition(
      CommandSuccessEventMessage.class, RuntimeGuards.always(), sendEvent2, () -> standbyRef.get());

    RuntimeState init = new RuntimeState("Init", List.of(initToStandby), List.of(), List.of(), false);
    RuntimeState standby = new RuntimeState("Standby", List.of(standbyToWaiting), List.of(), List.of(), false);
    RuntimeState waiting = new RuntimeState("WaitingForA", List.of(waitingToStandby), List.of(), List.of(), false);

    standbyRef.set(standby);
    waitingRef.set(waiting);

    this.runtime = new RuntimeInstance(new SimpleRuntimeDefinition(init));

    testMachine1.subscribe(EventMessage1.class, this::onEvent);
    testMachine1.subscribe(CommandSuccessEventMessage.class, this::onEvent);
    logger.info("TestMissionBeta initialized and subscribed to events");
  }

  public void start() {
    logger.info("TestMissionBeta starting");
    runtime.start();
  }

  public void onEvent(Event event) {
    logger.info("TestMissionBeta received event {}", event.getClass().getSimpleName());
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
