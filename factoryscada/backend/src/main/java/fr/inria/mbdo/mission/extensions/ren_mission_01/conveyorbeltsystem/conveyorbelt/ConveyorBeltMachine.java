package fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltsystem.conveyorbelt;

import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltsystem.conveyorbeltcommands.ConveyorCommandKind;
import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltsystem.conveyorbeltcommands.DirectionKind;
import fr.inria.mbdo.mission.runtime.api.MachineAdapter;

/**
 * From ConveyorBeltSystem::ConveyorBelt::ConveyorBeltMachine
 */
public interface ConveyorBeltMachine extends MachineAdapter {
  ConveyorCommandKind getCurrentCommand();

  void setCurrentCommand(ConveyorCommandKind currentCommand);

  DirectionKind getDirection();

  void setDirection(DirectionKind direction);

  int getCurrentStepCount();

  void setCurrentStepCount(int currentStepCount);

  int getTargetStepCount();

  void setTargetStepCount(int targetStepCount);

  boolean getConveyorSensFeed();

  void setConveyorSensFeed(boolean conveyorSensFeed);

  boolean getConveyorSensSwap();

  void setConveyorSensSwap(boolean conveyorSensSwap);

  int getConveyorSensImpulse();

  void setConveyorSensImpulse(int conveyorSensImpulse);

  /**
   * From ConveyorBeltSystem::ConveyorBelt::ConveyorBeltMachine::stop
   */
  void stop();

  /**
   * From ConveyorBeltSystem::ConveyorBelt::ConveyorBeltMachine::moveNbSteps
   */
  void moveNbSteps();

  /**
   * From ConveyorBeltSystem::ConveyorBelt::ConveyorBeltMachine::moveToSensor
   */
  void moveToSensor();

  /**
   * From ConveyorBeltSystem::ConveyorBelt::ConveyorBeltMachine::moveOut
   */
  void moveOut();

  /**
   * From ConveyorBeltSystem::ConveyorBelt::ConveyorBeltMachine::statusRequest
   */
  void statusRequest();
}
