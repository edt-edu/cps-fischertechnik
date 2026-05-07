package com.example.adapters;

import java.util.Objects;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.example.runtime.rtc.event.Event;
import com.example.runtime.rtc.time.RuntimeScheduler;
import com.example.runtime.rtc.event.TimerFiredEvent;
import java.util.function.Consumer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.example.generated.testmachinesystem.testmachine.TestMachineMachine;
import com.example.generated.testmachinesystem.testmachinecommands.TestCommandKind;
import com.example.generated.testmachinesystem.testmachinemessages.CommandSuccessEventMessage;
import com.example.generated.testmachinesystem.testmachinemessages.TestMachineAttAAbove10EventMessage;
import com.example.generated.testmachinesystem.testmachinemessages.TestMachineCommandChanged;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple adapter implementing TestMachineMachine used for local testing.
 * It publishes command changes and command completions to the provided event bus.
 */
public final class TestMachineAdapter implements TestMachineMachine {
  private final String id;
  private final Map<Class<?>, CopyOnWriteArrayList<Consumer<?>>> subscribers = new ConcurrentHashMap<>();
  private volatile TestCommandKind currentCommand;
  private volatile int attA;
  private volatile int attB;
  private final RuntimeScheduler scheduler;
  private static final Logger logger = LoggerFactory.getLogger(TestMachineAdapter.class);

  public TestMachineAdapter(String id, RuntimeScheduler scheduler) {
    this.id = Objects.requireNonNull(id);
    this.scheduler = Objects.requireNonNull(scheduler);
    this.currentCommand = null;
  }

  public String id() { return id; }

  @Override
  public TestCommandKind getCurrentCommand() {
    return currentCommand;
  }

  @Override
  public void setCurrentCommand(TestCommandKind currentCommand) {
    this.currentCommand = currentCommand;
    logger.info("{}: setCurrentCommand -> {}", id, currentCommand);
    // publish domain event representing command change
    publish(TestMachineCommandChanged.now(currentCommand));
  }

  @Override
  public int getAttA() {
    return attA;
  }

  @Override
  public void setAttA(int attA) {
    int previousAttA = this.attA;
    logger.info("{}: setAttA -> {}", id, attA);
    this.attA = attA;
    if (attA != previousAttA && attA > 10) {
      publish(TestMachineAttAAbove10EventMessage.now(attA));
    }
  }

  @Override
  public int getAttB() {
    return attB;
  }

  @Override
  public void setAttB(int attB) {
    logger.info("{}: setAttB -> {}", id, attB);
    this.attB = attB;
  }

  @Override
  public void actionA() {
    logger.info("{}: actionA() called !", id);
    setCurrentCommand(TestCommandKind.A);
  }

  @Override
  public void actionB() {
    logger.info("{}: actionB() called !", id);
    setCurrentCommand(TestCommandKind.B);
  }

  /**
   * Manually acknowledge a command success from an external tester/UI.
   */
  public void acknowledgeCommandSuccess() {
    logger.info("{}: manual acknowledge command success", id);
    this.setCurrentCommand(null);
    publish(CommandSuccessEventMessage.now());
  }

  public void shutdown() {
    logger.info("{}: manual shutdown", id);
  }

  public void publish(Event event) {
    logger.info("{}: publishing event {}", id, event.getClass().getSimpleName());
    for (Map.Entry<Class<?>, CopyOnWriteArrayList<Consumer<?>>> entry : subscribers.entrySet()) {
      if (!entry.getKey().isInstance(event)) continue;
      for (Consumer<?> handler : entry.getValue()) {
        try { ((Consumer) handler).accept(event); } catch (Throwable t) { logger.warn("handler threw", t); }
      }
    }
  }

  public <E extends Event> void subscribe(Class<E> eventType, Consumer<E> handler) {
    subscribers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(handler);
  }
}
