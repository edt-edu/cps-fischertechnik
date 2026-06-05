package fr.inria.mbdo.mission.extensions.ren_mission_01_impl;

import fr.inria.mbdo.mission.extensions.ren_mission_01.multiprocessingstationsystem.multiprocessingstation.MultiProcessingStationMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.multiprocessingstationsystem.multiprocessingstationcommands.MultiProcessingStationCommandKind;
import fr.inria.mbdo.mission.runtime.api.AbstractAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stub adapter for the Multi-Processing Station machine.
 *
 * <p>
 * Logs all commands and maintains local state. Connect to real hardware by
 * delegating to the legacy TCP socket layer and publishing events on feedback.
 */
public class MultiProcessingStationAdapterImpl extends AbstractAdapter implements MultiProcessingStationMachine {

    private static final Logger log = LoggerFactory.getLogger(MultiProcessingStationAdapterImpl.class);

    private volatile MultiProcessingStationCommandKind currentCommand;
    private volatile boolean sensor_MPS_in;
    private volatile boolean sensor_MPS_out;

    public MultiProcessingStationAdapterImpl(String id) {
        super(id);
    }

    @Override
    public MultiProcessingStationCommandKind getCurrentCommand() {
        return currentCommand;
    }

    @Override
    public void setCurrentCommand(MultiProcessingStationCommandKind currentCommand) {
        this.currentCommand = currentCommand;
        log.info("[{}] setCurrentCommand({})", id, currentCommand);
    }

    @Override
    public boolean getSensor_MPS_in() {
        return sensor_MPS_in;
    }

    @Override
    public void setSensor_MPS_in(boolean sensor_MPS_in) {
        this.sensor_MPS_in = sensor_MPS_in;
    }

    @Override
    public boolean getSensor_MPS_out() {
        return sensor_MPS_out;
    }

    @Override
    public void setSensor_MPS_out(boolean sensor_MPS_out) {
        this.sensor_MPS_out = sensor_MPS_out;
    }

    @Override
    public void armMove() {
        log.info("[{}] armMove()", id);
    }

    @Override
    public void ovenLoad() {
        log.info("[{}] ovenLoad()", id);
    }

    @Override
    public void turntableEject() {
        log.info("[{}] turntableEject()", id);
    }

    @Override
    public void turntableRotate() {
        log.info("[{}] turntableRotate()", id);
    }

    @Override
    public void armPick() {
        log.info("[{}] armPick()", id);
    }

    @Override
    public void stop() {
        log.info("[{}] stop()", id);
    }

    @Override
    public void ovenHeat() {
        log.info("[{}] ovenHeat()", id);
    }

    @Override
    public void conveyorMoveOut() {
        log.info("[{}] conveyorMoveOut()", id);
    }

    @Override
    public void ovenUnload() {
        log.info("[{}] ovenUnload()", id);
    }

    @Override
    public void ovenProcess() {
        log.info("[{}] ovenProcess()", id);
    }

    @Override
    public void conveyorMoveToSensor() {
        log.info("[{}] conveyorMoveToSensor()", id);
    }

    @Override
    public void process1() {
        log.info("[{}] process1()", id);
    }

    @Override
    public void sawCut() {
        log.info("[{}] sawCut()", id);
    }

    @Override
    public void armPlace() {
        log.info("[{}] armPlace()", id);
    }

    @Override
    public void setup() {
        log.info("[{}] setup()", id);
    }

    @Override
    public void process() {
        log.info("[{}] process()", id);
    }

    @Override
    public void moveToSafePosition() {
        log.info("[{}] moveToSafePosition()", id);
    }
}
