package fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltsystem.conveyorbelt;

import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltmissions.conveyorbeltnominalmission.customevents.AcceptWhenConveyorBeltConveyorSensFeedEqualstrueAndConveyorBeltConveyorSensSwapEqualsfalseEvent;
import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltsystem.conveyorbeltcommands.ConveyorCommandKind;
import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltsystem.conveyorbeltcommands.DirectionKind;
import fr.inria.mbdo.mission.runtime.api.AbstractMachineAdapter;
import java.lang.Override;
import java.lang.String;

/**
 * From ConveyorBeltSystem::ConveyorBelt::ConveyorBeltMachine
 */
public abstract class AbstractConveyorBeltMachineAdapter extends AbstractMachineAdapter implements ConveyorBeltMachine {
  protected volatile ConveyorCommandKind currentCommand;

  protected volatile DirectionKind direction;

  protected volatile int currentStepCount;

  protected volatile int targetStepCount;

  protected volatile boolean conveyorSensFeed;

  protected volatile boolean conveyorSensSwap;

  protected volatile int conveyorSensImpulse;

  protected AbstractConveyorBeltMachineAdapter(String id) {
    super(id);
  }

  @Override
  public ConveyorCommandKind getCurrentCommand() {
    return this.currentCommand;
  }

  @Override
  public void setCurrentCommand(ConveyorCommandKind currentCommand) {
    this.currentCommand = currentCommand;
  }

  @Override
  public DirectionKind getDirection() {
    return this.direction;
  }

  @Override
  public void setDirection(DirectionKind direction) {
    this.direction = direction;
  }

  @Override
  public int getCurrentStepCount() {
    return this.currentStepCount;
  }

  @Override
  public void setCurrentStepCount(int currentStepCount) {
    this.currentStepCount = currentStepCount;
  }

  @Override
  public int getTargetStepCount() {
    return this.targetStepCount;
  }

  @Override
  public void setTargetStepCount(int targetStepCount) {
    this.targetStepCount = targetStepCount;
  }

  @Override
  public boolean getConveyorSensFeed() {
    return this.conveyorSensFeed;
  }

  @Override
  public void setConveyorSensFeed(boolean conveyorSensFeed) {
    this.conveyorSensFeed = conveyorSensFeed;
    checkAndFireAcceptWhenEvents();
  }

  @Override
  public boolean getConveyorSensSwap() {
    return this.conveyorSensSwap;
  }

  @Override
  public void setConveyorSensSwap(boolean conveyorSensSwap) {
    this.conveyorSensSwap = conveyorSensSwap;
    checkAndFireAcceptWhenEvents();
  }

  @Override
  public int getConveyorSensImpulse() {
    return this.conveyorSensImpulse;
  }

  @Override
  public void setConveyorSensImpulse(int conveyorSensImpulse) {
    this.conveyorSensImpulse = conveyorSensImpulse;
  }

  /**
   * From ConveyorBeltSystem::ConveyorBelt::ConveyorBeltMachine::moveToSensor
   */
  @Override
  public abstract void moveToSensor();

  /**
   * From ConveyorBeltSystem::ConveyorBelt::ConveyorBeltMachine::moveOut
   */
  @Override
  public abstract void moveOut();

  /**
   * From ConveyorBeltSystem::ConveyorBelt::ConveyorBeltMachine::moveNbSteps
   */
  @Override
  public abstract void moveNbSteps();

  /**
   * From ConveyorBeltSystem::ConveyorBelt::ConveyorBeltMachine::stop
   */
  @Override
  public abstract void stop();

  /**
   * From ConveyorBeltSystem::ConveyorBelt::ConveyorBeltMachine::statusRequest
   */
  @Override
  public abstract void statusRequest();

  private void checkAndFireAcceptWhenEvents() {
    if (((this.conveyorSensFeed == true) && (this.conveyorSensSwap == false))) {
      publish(new AcceptWhenConveyorBeltConveyorSensFeedEqualstrueAndConveyorBeltConveyorSensSwapEqualsfalseEvent());
    }
  }
}
