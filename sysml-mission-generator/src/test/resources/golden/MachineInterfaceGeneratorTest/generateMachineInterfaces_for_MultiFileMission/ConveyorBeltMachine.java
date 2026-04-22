package fr.inria.factoryscada.sysmlbaseddomain.multifilemission_conveyorbelt;

import fr.inria.factoryscada.sysmlbaseddomain.multifilemission_conveyorbeltcommands.ConveyorCommandKind;
import fr.inria.factoryscada.sysmlbaseddomain.multifilemission_conveyorbeltcommands.DirectionKind;

/**
 * From MultiFileMission_ConveyorBelt::ConveyorBeltMachine
 * Definition of Conveyor Belt Machine
 */
public interface ConveyorBeltMachine {
  void setCurrentCommand(ConveyorCommandKind currentCommand);

  ConveyorCommandKind getCurrentCommand();

  void setDirection(DirectionKind direction);

  DirectionKind getDirection();

  void setCurrentStepCount(int currentStepCount);

  int getCurrentStepCount();

  void setTargetStepCount(int targetStepCount);

  int getTargetStepCount();

  void setConveyorSensFeed(boolean conveyorSensFeed);

  boolean getConveyorSensFeed();

  void setConveyorSensSwap(boolean conveyorSensSwap);

  boolean getConveyorSensSwap();

  void setConveyorSensImpulse(int conveyorSensImpulse);

  int getConveyorSensImpulse();

  /**
   * From MultiFileMission_ConveyorBeltCommands::MoveToSensor
   * Moves the conveyor in the given direction until the conveyed object reaches the sensor
   */
  void MoveToSensor(DirectionKind direction);

  /**
   * From MultiFileMission_ConveyorBeltCommands::MoveOut
   * Moves the conveyor in the given direction until the conveyed object reaches the sensor and is ejected of the Conveyor belt
   */
  void MoveOut(DirectionKind direction);

  /**
   * From MultiFileMission_ConveyorBeltCommands::MoveNbSteps
   */
  void MoveNbSteps(int steps, DirectionKind direction);

  /**
   * From MultiFileMission_ConveyorBeltCommands::Stop
   */
  void Stop();

  /**
   * From MultiFileMission_ConveyorBeltCommands::StatusRequest
   */
  void StatusRequest();
}
