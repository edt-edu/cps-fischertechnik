package fr.inria.mbdo.mission.extensions.ren_mission_01.multiprocessingstationsystem.multiprocessingstation;

import fr.inria.mbdo.mission.extensions.ren_mission_01.multiprocessingstationmissions.multiprocessingstationnominalmission.customevents.AcceptWhenMultiProcessingStationSensorMPSinEqualstrueAndMultiProcessingStationSensorMPSoutEqualsfalseEvent;
import fr.inria.mbdo.mission.extensions.ren_mission_01.multiprocessingstationsystem.multiprocessingstationcommands.MultiProcessingStationCommandKind;
import fr.inria.mbdo.mission.runtime.api.AbstractAdapter;
import java.lang.Override;
import java.lang.String;

/**
 * From MultiProcessingStationSystem::MultiProcessingStation::MultiProcessingStationMachine
 */
public abstract class AbstractMultiProcessingStationMachineAdapter extends AbstractAdapter implements MultiProcessingStationMachine {
  protected volatile MultiProcessingStationCommandKind currentCommand;

  protected volatile boolean sensor_MPS_in;

  protected volatile boolean sensor_MPS_out;

  protected AbstractMultiProcessingStationMachineAdapter(String id) {
    super(id);
  }

  @Override
  public MultiProcessingStationCommandKind getCurrentCommand() {
    return this.currentCommand;
  }

  @Override
  public void setCurrentCommand(MultiProcessingStationCommandKind currentCommand) {
    this.currentCommand = currentCommand;
  }

  @Override
  public boolean getSensor_MPS_in() {
    return this.sensor_MPS_in;
  }

  @Override
  public void setSensor_MPS_in(boolean sensor_MPS_in) {
    this.sensor_MPS_in = sensor_MPS_in;
    checkAndFireAcceptWhenEvents();
  }

  @Override
  public boolean getSensor_MPS_out() {
    return this.sensor_MPS_out;
  }

  @Override
  public void setSensor_MPS_out(boolean sensor_MPS_out) {
    this.sensor_MPS_out = sensor_MPS_out;
    checkAndFireAcceptWhenEvents();
  }

  /**
   * From MultiProcessingStationSystem::MultiProcessingStation::MultiProcessingStationMachine::setup
   */
  @Override
  public abstract void setup();

  /**
   * From MultiProcessingStationSystem::MultiProcessingStation::MultiProcessingStationMachine::stop
   */
  @Override
  public abstract void stop();

  /**
   * From MultiProcessingStationSystem::MultiProcessingStation::MultiProcessingStationMachine::process1
   */
  @Override
  public abstract void process1();

  /**
   * From MultiProcessingStationSystem::MultiProcessingStation::MultiProcessingStationMachine::process
   */
  @Override
  public abstract void process();

  /**
   * From MultiProcessingStationSystem::MultiProcessingStation::MultiProcessingStationMachine::moveToSafePosition
   */
  @Override
  public abstract void moveToSafePosition();

  /**
   * From MultiProcessingStationSystem::MultiProcessingStation::MultiProcessingStationMachine::ovenLoad
   */
  @Override
  public abstract void ovenLoad();

  /**
   * From MultiProcessingStationSystem::MultiProcessingStation::MultiProcessingStationMachine::ovenUnload
   */
  @Override
  public abstract void ovenUnload();

  /**
   * From MultiProcessingStationSystem::MultiProcessingStation::MultiProcessingStationMachine::ovenHeat
   */
  @Override
  public abstract void ovenHeat();

  /**
   * From MultiProcessingStationSystem::MultiProcessingStation::MultiProcessingStationMachine::ovenProcess
   */
  @Override
  public abstract void ovenProcess();

  /**
   * From MultiProcessingStationSystem::MultiProcessingStation::MultiProcessingStationMachine::armMove
   */
  @Override
  public abstract void armMove();

  /**
   * From MultiProcessingStationSystem::MultiProcessingStation::MultiProcessingStationMachine::armPick
   */
  @Override
  public abstract void armPick();

  /**
   * From MultiProcessingStationSystem::MultiProcessingStation::MultiProcessingStationMachine::armPlace
   */
  @Override
  public abstract void armPlace();

  /**
   * From MultiProcessingStationSystem::MultiProcessingStation::MultiProcessingStationMachine::turntableRotate
   */
  @Override
  public abstract void turntableRotate();

  /**
   * From MultiProcessingStationSystem::MultiProcessingStation::MultiProcessingStationMachine::turntableEject
   */
  @Override
  public abstract void turntableEject();

  /**
   * From MultiProcessingStationSystem::MultiProcessingStation::MultiProcessingStationMachine::conveyorMoveToSensor
   */
  @Override
  public abstract void conveyorMoveToSensor();

  /**
   * From MultiProcessingStationSystem::MultiProcessingStation::MultiProcessingStationMachine::conveyorMoveOut
   */
  @Override
  public abstract void conveyorMoveOut();

  /**
   * From MultiProcessingStationSystem::MultiProcessingStation::MultiProcessingStationMachine::sawCut
   */
  @Override
  public abstract void sawCut();

  private void checkAndFireAcceptWhenEvents() {
    if (((this.sensor_MPS_in == true) && (this.sensor_MPS_out == false))) {
      publish(new AcceptWhenMultiProcessingStationSensorMPSinEqualstrueAndMultiProcessingStationSensorMPSoutEqualsfalseEvent());
    }
  }
}
