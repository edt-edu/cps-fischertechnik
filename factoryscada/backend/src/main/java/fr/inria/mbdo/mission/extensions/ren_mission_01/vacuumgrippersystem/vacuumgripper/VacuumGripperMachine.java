package fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgripper;

import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgrippercommands.ExecutionStatusKind;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgrippercommands.Position3D;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgrippercommands.VacuumGripperCommandKind;
import fr.inria.mbdo.mission.runtime.api.MachineAdapter;

/**
 * From VacuumGripperSystem::VacuumGripper::VacuumGripperMachine
 */
public interface VacuumGripperMachine extends MachineAdapter {
  VacuumGripperCommandKind getCurrentCommand();

  void setCurrentCommand(VacuumGripperCommandKind currentCommand);

  ExecutionStatusKind getExecutionStatus();

  void setExecutionStatus(ExecutionStatusKind executionStatus);

  float getVerticalEncoder();

  void setVerticalEncoder(float verticalEncoder);

  float getArmEncoder();

  void setArmEncoder(float armEncoder);

  float getRotEncoder();

  void setRotEncoder(float rotEncoder);

  float getExpectedVerticalEncoderValue();

  void setExpectedVerticalEncoderValue(float expectedVerticalEncoderValue);

  float getExpectedArmEncoderValue();

  void setExpectedArmEncoderValue(float expectedArmEncoderValue);

  float getExpectedRotationEncoderValue();

  void setExpectedRotationEncoderValue(float expectedRotationEncoderValue);

  boolean getVacuumActCompressorOn();

  void setVacuumActCompressorOn(boolean vacuumActCompressorOn);

  boolean getVacuumActValve();

  void setVacuumActValve(boolean vacuumActValve);

  /**
   * From VacuumGripperSystem::VacuumGripper::VacuumGripperMachine::goToPosition
   */
  void goToPosition(Position3D targetPosition);

  /**
   * From VacuumGripperSystem::VacuumGripper::VacuumGripperMachine::move
   */
  void move(Position3D startPosition, Position3D endPosition);

  /**
   * From VacuumGripperSystem::VacuumGripper::VacuumGripperMachine::pick
   */
  void pick(Position3D targetPosition);

  /**
   * From VacuumGripperSystem::VacuumGripper::VacuumGripperMachine::place
   */
  void place(Position3D targetPosition);

  /**
   * From VacuumGripperSystem::VacuumGripper::VacuumGripperMachine::setup
   */
  void setup();

  /**
   * From VacuumGripperSystem::VacuumGripper::VacuumGripperMachine::statusRequest
   */
  void statusRequest();

  /**
   * From VacuumGripperSystem::VacuumGripper::VacuumGripperMachine::grip
   */
  void grip();

  /**
   * From VacuumGripperSystem::VacuumGripper::VacuumGripperMachine::release
   */
  void release();

  /**
   * From VacuumGripperSystem::VacuumGripper::VacuumGripperMachine::stop
   */
  void stop();

  /**
   * From VacuumGripperSystem::VacuumGripper::VacuumGripperMachine::moveToSafePosition
   */
  void moveToSafePosition();

  /**
   * From VacuumGripperSystem::VacuumGripper::VacuumGripperMachine::retractArm
   */
  void retractArm();
}
