package fr.inria.mbdo.mission.extensions.ren_mission_01_impl;

import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinesystem.sortingline.SortingLineMachine;
import fr.inria.mbdo.mission.runtime.api.AbstractAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stub adapter for the Sorting Line machine.
 *
 * <p>
 * Logs all commands and maintains local state. Connect to real hardware by
 * delegating to the legacy TCP socket layer and publishing events on feedback.
 */
public class SortingLineAdapterImpl extends AbstractAdapter implements SortingLineMachine {

    private static final Logger log = LoggerFactory.getLogger(SortingLineAdapterImpl.class);

    private volatile boolean sensor_SL_in;
    private volatile boolean sensor_SL_blue;
    private volatile boolean sensor_SL_white;
    private volatile boolean sensor_SL_red;

    public SortingLineAdapterImpl(String id) {
        super(id);
    }

    @Override
    public boolean getSensor_SL_in() {
        return sensor_SL_in;
    }

    @Override
    public void setSensor_SL_in(boolean sensor_SL_in) {
        this.sensor_SL_in = sensor_SL_in;
    }

    @Override
    public boolean getSensor_SL_blue() {
        return sensor_SL_blue;
    }

    @Override
    public void setSensor_SL_blue(boolean sensor_SL_blue) {
        this.sensor_SL_blue = sensor_SL_blue;
    }

    @Override
    public boolean getSensor_SL_white() {
        return sensor_SL_white;
    }

    @Override
    public void setSensor_SL_white(boolean sensor_SL_white) {
        this.sensor_SL_white = sensor_SL_white;
    }

    @Override
    public boolean getSensor_SL_red() {
        return sensor_SL_red;
    }

    @Override
    public void setSensor_SL_red(boolean sensor_SL_red) {
        this.sensor_SL_red = sensor_SL_red;
    }

    @Override
    public void eject() {
        log.info("[{}] eject()", id);
    }

    @Override
    public void stop() {
        log.info("[{}] stop()", id);
    }
}
