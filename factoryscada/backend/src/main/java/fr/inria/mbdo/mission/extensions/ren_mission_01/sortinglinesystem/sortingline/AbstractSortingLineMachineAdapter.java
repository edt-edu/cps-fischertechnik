package fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinesystem.sortingline;

import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinemissions.sortinglinenominalmission.customevents.AcceptWhenSortingLineSensorSLblueEqualsfalseEvent;
import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinemissions.sortinglinenominalmission.customevents.AcceptWhenSortingLineSensorSLinEqualsfalseEvent;
import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinemissions.sortinglinenominalmission.customevents.AcceptWhenSortingLineSensorSLredEqualsfalseEvent;
import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinemissions.sortinglinenominalmission.customevents.AcceptWhenSortingLineSensorSLwhiteEqualsfalseEvent;
import fr.inria.mbdo.mission.runtime.api.AbstractMachineAdapter;
import java.lang.Override;
import java.lang.String;

/**
 * From SortingLineSystem::SortingLine::SortingLineMachine
 */
public abstract class AbstractSortingLineMachineAdapter extends AbstractMachineAdapter implements SortingLineMachine {
  protected volatile boolean sensor_SL_in;

  protected volatile boolean sensor_SL_blue;

  protected volatile boolean sensor_SL_white;

  protected volatile boolean sensor_SL_red;

  protected AbstractSortingLineMachineAdapter(String id) {
    super(id);
  }

  @Override
  public boolean getSensor_SL_in() {
    return this.sensor_SL_in;
  }

  @Override
  public void setSensor_SL_in(boolean sensor_SL_in) {
    this.sensor_SL_in = sensor_SL_in;
    checkAndFireAcceptWhenEvents();
  }

  @Override
  public boolean getSensor_SL_blue() {
    return this.sensor_SL_blue;
  }

  @Override
  public void setSensor_SL_blue(boolean sensor_SL_blue) {
    this.sensor_SL_blue = sensor_SL_blue;
    checkAndFireAcceptWhenEvents();
  }

  @Override
  public boolean getSensor_SL_white() {
    return this.sensor_SL_white;
  }

  @Override
  public void setSensor_SL_white(boolean sensor_SL_white) {
    this.sensor_SL_white = sensor_SL_white;
    checkAndFireAcceptWhenEvents();
  }

  @Override
  public boolean getSensor_SL_red() {
    return this.sensor_SL_red;
  }

  @Override
  public void setSensor_SL_red(boolean sensor_SL_red) {
    this.sensor_SL_red = sensor_SL_red;
    checkAndFireAcceptWhenEvents();
  }

  /**
   * From SortingLineSystem::SortingLine::SortingLineMachine::eject
   */
  @Override
  public abstract void eject();

  /**
   * From SortingLineSystem::SortingLine::SortingLineMachine::stop
   */
  @Override
  public abstract void stop();

  private void checkAndFireAcceptWhenEvents() {
    if ((this.sensor_SL_blue == false)) {
      publish(new AcceptWhenSortingLineSensorSLblueEqualsfalseEvent());
    }
    if ((this.sensor_SL_white == false)) {
      publish(new AcceptWhenSortingLineSensorSLwhiteEqualsfalseEvent());
    }
    if ((this.sensor_SL_red == false)) {
      publish(new AcceptWhenSortingLineSensorSLredEqualsfalseEvent());
    }
    if ((this.sensor_SL_in == false)) {
      publish(new AcceptWhenSortingLineSensorSLinEqualsfalseEvent());
    }
  }
}
