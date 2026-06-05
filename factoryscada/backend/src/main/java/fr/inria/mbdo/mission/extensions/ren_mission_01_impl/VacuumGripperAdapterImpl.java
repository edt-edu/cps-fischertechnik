package fr.inria.mbdo.mission.extensions.ren_mission_01_impl;

import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgripper.VacuumGripperMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgrippercommands.ExecutionStatusKind;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgrippercommands.VacuumGripperCommandKind;
import fr.inria.mbdo.mission.runtime.api.AbstractAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stub adapter for a Vacuum Gripper machine (VacuumGripper1 or VacuumGripper2).
 *
 * <p>
 * Logs all commands and maintains local state. Connect to real hardware by
 * delegating to the legacy TCP socket layer and publishing events on feedback.
 */
public class VacuumGripperAdapterImpl extends AbstractAdapter implements VacuumGripperMachine {

    private static final Logger log = LoggerFactory.getLogger(VacuumGripperAdapterImpl.class);

    private volatile VacuumGripperCommandKind currentCommand;
    private volatile ExecutionStatusKind executionStatus;
    private volatile float verticalEncoder;
    private volatile float armEncoder;
    private volatile float rotEncoder;
    private volatile float expectedVerticalEncoderValue;
    private volatile float expectedArmEncoderValue;
    private volatile float expectedRotationEncoderValue;
    private volatile boolean vacuumActCompressorOn;
    private volatile boolean vacuumActValve;

    public VacuumGripperAdapterImpl(String id) {
        super(id);
    }

    @Override
    public VacuumGripperCommandKind getCurrentCommand() {
        return currentCommand;
    }

    @Override
    public void setCurrentCommand(VacuumGripperCommandKind currentCommand) {
        this.currentCommand = currentCommand;
        log.info("[{}] setCurrentCommand({})", id, currentCommand);
    }

    @Override
    public ExecutionStatusKind getExecutionStatus() {
        return executionStatus;
    }

    @Override
    public void setExecutionStatus(ExecutionStatusKind executionStatus) {
        this.executionStatus = executionStatus;
    }

    @Override
    public float getVerticalEncoder() {
        return verticalEncoder;
    }

    @Override
    public void setVerticalEncoder(float verticalEncoder) {
        this.verticalEncoder = verticalEncoder;
    }

    @Override
    public float getArmEncoder() {
        return armEncoder;
    }

    @Override
    public void setArmEncoder(float armEncoder) {
        this.armEncoder = armEncoder;
    }

    @Override
    public float getRotEncoder() {
        return rotEncoder;
    }

    @Override
    public void setRotEncoder(float rotEncoder) {
        this.rotEncoder = rotEncoder;
    }

    @Override
    public float getExpectedVerticalEncoderValue() {
        return expectedVerticalEncoderValue;
    }

    @Override
    public void setExpectedVerticalEncoderValue(float v) {
        this.expectedVerticalEncoderValue = v;
    }

    @Override
    public float getExpectedArmEncoderValue() {
        return expectedArmEncoderValue;
    }

    @Override
    public void setExpectedArmEncoderValue(float v) {
        this.expectedArmEncoderValue = v;
    }

    @Override
    public float getExpectedRotationEncoderValue() {
        return expectedRotationEncoderValue;
    }

    @Override
    public void setExpectedRotationEncoderValue(float v) {
        this.expectedRotationEncoderValue = v;
    }

    @Override
    public boolean getVacuumActCompressorOn() {
        return vacuumActCompressorOn;
    }

    @Override
    public void setVacuumActCompressorOn(boolean v) {
        this.vacuumActCompressorOn = v;
    }

    @Override
    public boolean getVacuumActValve() {
        return vacuumActValve;
    }

    @Override
    public void setVacuumActValve(boolean v) {
        this.vacuumActValve = v;
    }

    @Override
    public void release() {
        log.info("[{}] release()", id);
    }

    @Override
    public void statusRequest() {
        log.info("[{}] statusRequest()", id);
    }

    @Override
    public void pick() {
        log.info("[{}] pick()", id);
    }

    @Override
    public void move() {
        log.info("[{}] move()", id);
    }

    @Override
    public void grip() {
        log.info("[{}] grip()", id);
    }

    @Override
    public void goToPosition() {
        log.info("[{}] goToPosition()", id);
    }

    @Override
    public void place() {
        log.info("[{}] place()", id);
    }

    @Override
    public void moveToSafePosition() {
        log.info("[{}] moveToSafePosition()", id);
    }

    @Override
    public void stop() {
        log.info("[{}] stop()", id);
    }

    @Override
    public void setup() {
        log.info("[{}] setup()", id);
    }

    @Override
    public void retractArm() {
        log.info("[{}] retractArm()", id);
    }
}
