package com.example.generated.testmachinesystem.testmachine;

import com.example.generated.testmachinesystem.testmachinecommands.TestCommandKind;
import com.example.runtime.rtc.event.Event;
import java.util.function.Consumer;

/**
 * From TestMachineSystem::TestMachine::TestMachineMachine
 */
public interface TestMachineMachine {
  TestCommandKind getCurrentCommand();

  void setCurrentCommand(TestCommandKind currentCommand);

  int getAttA();

  void setAttA(int attA);

  int getAttB();

  void setAttB(int attB);

  /**
   * From TestMachineSystem::TestMachine::TestMachineMachine::actionA
   */
  void actionA();

  /**
   * From TestMachineSystem::TestMachine::TestMachineMachine::actionB
   */
  void actionB();

  void publish(Event event);

  <E extends Event> void subscribe(Class<E> eventType, Consumer<E> handler);
}
