package fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgripper;

import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgrippercommands.ExecutionStatusKind;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgrippercommands.Position3D;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgrippercommands.VacuumGripperCommandKind;
import fr.inria.mbdo.mission.runtime.api.AbstractMachineAdapter;
import java.lang.Override;
import java.lang.String;

/**
 * From VacuumGripperSystem::VacuumGripper::VacuumGripperMachine
 */
public abstract class AbstractVacuumGripperMachineAdapter extends AbstractMachineAdapter implements VacuumGripperMachine {
  protected volatile VacuumGripperCommandKind currentCommand;

  protected volatile ExecutionStatusKind executionStatus;

  protected volatile float verticalEncoder;

  protected volatile float armEncoder;

  protected volatile float rotEncoder;

  protected volatile float expectedVerticalEncoderValue;

  protected volatile float expectedArmEncoderValue;

  protected volatile float expectedRotationEncoderValue;

  protected volatile boolean vacuumActCompressorOn;

  protected volatile boolean vacuumActValve;

  protected AbstractVacuumGripperMachineAdapter(String id) {
    super(id);
  }

  @Override
  public VacuumGripperCommandKind getCurrentCommand() {
    return this.currentCommand;
  }

  @Override
  public void setCurrentCommand(VacuumGripperCommandKind currentCommand) {
    this.currentCommand = currentCommand;
  }

  @Override
  public ExecutionStatusKind getExecutionStatus() {
    return this.executionStatus;
  }

  @Override
  public void setExecutionStatus(ExecutionStatusKind executionStatus) {
    this.executionStatus = executionStatus;
  }

  @Override
  public float getVerticalEncoder() {
    return this.verticalEncoder;
  }

  @Override
  public void setVerticalEncoder(float verticalEncoder) {
    this.verticalEncoder = verticalEncoder;
  }

  @Override
  public float getArmEncoder() {
    return this.armEncoder;
  }

  @Override
  public void setArmEncoder(float armEncoder) {
    this.armEncoder = armEncoder;
  }

  @Override
  public float getRotEncoder() {
    return this.rotEncoder;
  }

  @Override
  public void setRotEncoder(float rotEncoder) {
    this.rotEncoder = rotEncoder;
  }

  @Override
  public float getExpectedVerticalEncoderValue() {
    return this.expectedVerticalEncoderValue;
  }

  @Override
  public void setExpectedVerticalEncoderValue(float expectedVerticalEncoderValue) {
    this.expectedVerticalEncoderValue = expectedVerticalEncoderValue;
  }

  @Override
  public float getExpectedArmEncoderValue() {
    return this.expectedArmEncoderValue;
  }

  @Override
  public void setExpectedArmEncoderValue(float expectedArmEncoderValue) {
    this.expectedArmEncoderValue = expectedArmEncoderValue;
  }

  @Override
  public float getExpectedRotationEncoderValue() {
    return this.expectedRotationEncoderValue;
  }

  @Override
  public void setExpectedRotationEncoderValue(float expectedRotationEncoderValue) {
    this.expectedRotationEncoderValue = expectedRotationEncoderValue;
  }

  @Override
  public boolean getVacuumActCompressorOn() {
    return this.vacuumActCompressorOn;
  }

  @Override
  public void setVacuumActCompressorOn(boolean vacuumActCompressorOn) {
    this.vacuumActCompressorOn = vacuumActCompressorOn;
  }

  @Override
  public boolean getVacuumActValve() {
    return this.vacuumActValve;
  }

  @Override
  public void setVacuumActValve(boolean vacuumActValve) {
    this.vacuumActValve = vacuumActValve;
  }

  /**
   * From VacuumGripperSystem::VacuumGripper::VacuumGripperMachine::goToPosition
   */
  @Override
  public abstract void goToPosition(Position3D targetPosition);

  /**
   * From VacuumGripperSystem::VacuumGripper::VacuumGripperMachine::move
   */
  @Override
  public abstract void move(Position3D startPosition, Position3D endPosition);

  /**
   * From VacuumGripperSystem::VacuumGripper::VacuumGripperMachine::pick
   */
  @Override
  public abstract void pick(Position3D targetPosition);

  /**
   * From VacuumGripperSystem::VacuumGripper::VacuumGripperMachine::place
   */
  @Override
  public abstract void place(Position3D targetPosition);

  /**
   * From VacuumGripperSystem::VacuumGripper::VacuumGripperMachine::goToNamedPosition
   */
  @Override
  public abstract void goToNamedPosition(String positionName);

  /**
   * From VacuumGripperSystem::VacuumGripper::VacuumGripperMachine::pickNamed
   */
  @Override
  public abstract void pickNamed(String positionName);

  /**
   * From VacuumGripperSystem::VacuumGripper::VacuumGripperMachine::placeNamed
   */
  @Override
  public abstract void placeNamed(String positionName);

  /**
   * From VacuumGripperSystem::VacuumGripper::VacuumGripperMachine::setup
   */
  @Override
  public abstract void setup();

  /**
   * From VacuumGripperSystem::VacuumGripper::VacuumGripperMachine::statusRequest
   */
  @Override
  public abstract void statusRequest();

  /**
   * From VacuumGripperSystem::VacuumGripper::VacuumGripperMachine::grip
   */
  @Override
  public abstract void grip();

  /**
   * From VacuumGripperSystem::VacuumGripper::VacuumGripperMachine::release
   */
  @Override
  public abstract void release();

  /**
   * From VacuumGripperSystem::VacuumGripper::VacuumGripperMachine::stop
   */
  @Override
  public abstract void stop();

  /**
   * From VacuumGripperSystem::VacuumGripper::VacuumGripperMachine::moveToSafePosition
   */
  @Override
  public abstract void moveToSafePosition();

  /**
   * From VacuumGripperSystem::VacuumGripper::VacuumGripperMachine::retractArm
   */
  @Override
  public abstract void retractArm();
}
