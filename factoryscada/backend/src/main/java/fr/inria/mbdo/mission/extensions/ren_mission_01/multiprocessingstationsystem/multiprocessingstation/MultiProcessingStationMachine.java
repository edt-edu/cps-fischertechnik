package fr.inria.mbdo.mission.extensions.ren_mission_01.multiprocessingstationsystem.multiprocessingstation;

import fr.inria.mbdo.mission.extensions.ren_mission_01.multiprocessingstationsystem.multiprocessingstationcommands.MultiProcessingStationCommandKind;
import fr.inria.mbdo.mission.runtime.api.MachineAdapter;

/**
 * From MultiProcessingStationSystem::MultiProcessingStation::MultiProcessingStationMachine
 */
public interface MultiProcessingStationMachine extends MachineAdapter {
  MultiProcessingStationCommandKind getCurrentCommand();

  void setCurrentCommand(MultiProcessingStationCommandKind currentCommand);

  boolean getSensor_MPS_in();

  void setSensor_MPS_in(boolean sensor_MPS_in);

  boolean getSensor_MPS_out();

  void setSensor_MPS_out(boolean sensor_MPS_out);

  /**
   * From MultiProcessingStationSystem::MultiProcessingStation::MultiProcessingStationMachine::setup
   */
  void setup();

  /**
   * From MultiProcessingStationSystem::MultiProcessingStation::MultiProcessingStationMachine::stop
   */
  void stop();

  /**
   * From MultiProcessingStationSystem::MultiProcessingStation::MultiProcessingStationMachine::process1
   */
  void process1();

  /**
   * From MultiProcessingStationSystem::MultiProcessingStation::MultiProcessingStationMachine::process
   */
  void process();

  /**
   * From MultiProcessingStationSystem::MultiProcessingStation::MultiProcessingStationMachine::moveToSafePosition
   */
  void moveToSafePosition();

  /**
   * From MultiProcessingStationSystem::MultiProcessingStation::MultiProcessingStationMachine::ovenLoad
   */
  void ovenLoad();

  /**
   * From MultiProcessingStationSystem::MultiProcessingStation::MultiProcessingStationMachine::ovenUnload
   */
  void ovenUnload();

  /**
   * From MultiProcessingStationSystem::MultiProcessingStation::MultiProcessingStationMachine::ovenHeat
   */
  void ovenHeat();

  /**
   * From MultiProcessingStationSystem::MultiProcessingStation::MultiProcessingStationMachine::ovenProcess
   */
  void ovenProcess();

  /**
   * From MultiProcessingStationSystem::MultiProcessingStation::MultiProcessingStationMachine::armMove
   */
  void armMove();

  /**
   * From MultiProcessingStationSystem::MultiProcessingStation::MultiProcessingStationMachine::armPick
   */
  void armPick();

  /**
   * From MultiProcessingStationSystem::MultiProcessingStation::MultiProcessingStationMachine::armPlace
   */
  void armPlace();

  /**
   * From MultiProcessingStationSystem::MultiProcessingStation::MultiProcessingStationMachine::turntableRotate
   */
  void turntableRotate();

  /**
   * From MultiProcessingStationSystem::MultiProcessingStation::MultiProcessingStationMachine::turntableEject
   */
  void turntableEject();

  /**
   * From MultiProcessingStationSystem::MultiProcessingStation::MultiProcessingStationMachine::conveyorMoveToSensor
   */
  void conveyorMoveToSensor();

  /**
   * From MultiProcessingStationSystem::MultiProcessingStation::MultiProcessingStationMachine::conveyorMoveOut
   */
  void conveyorMoveOut();

  /**
   * From MultiProcessingStationSystem::MultiProcessingStation::MultiProcessingStationMachine::sawCut
   */
  void sawCut();
}
