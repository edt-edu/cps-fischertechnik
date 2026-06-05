package fr.inria.mbdo.mission.extensions.ren_mission_01_impl;

import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltsystem.conveyorbelt.ConveyorBeltMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltsystem.conveyorbeltcommands.ConveyorCommandKind;
import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltsystem.conveyorbeltcommands.DirectionKind;
import fr.inria.mbdo.mission.runtime.api.AbstractAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stub adapter for the Conveyor Belt machine.
 *
 * <p>
 * Logs all commands and maintains local state. To connect to real hardware,
 * inject the legacy {@code FactoryScada} service and delegate command calls
 * to the TCP socket layer, then call {@link #publish} when hardware feedback
 * arrives (e.g. {@code CBCommandSuccessEventMessage}).
 */
public class ConveyorBeltAdapterImpl extends AbstractAdapter implements ConveyorBeltMachine {

    private static final Logger log = LoggerFactory.getLogger(ConveyorBeltAdapterImpl.class);

    private volatile ConveyorCommandKind currentCommand;
    private volatile DirectionKind direction;
    private volatile int currentStepCount;
    private volatile int targetStepCount;
    private volatile boolean conveyorSensFeed;
    private volatile boolean conveyorSensSwap;
    private volatile int conveyorSensImpulse;

    public ConveyorBeltAdapterImpl(String id) {
        super(id);
    }

    @Override
    public ConveyorCommandKind getCurrentCommand() {
        return currentCommand;
    }

    @Override
    public void setCurrentCommand(ConveyorCommandKind currentCommand) {
        this.currentCommand = currentCommand;
        log.info("[{}] setCurrentCommand({})", id, currentCommand);
    }

    @Override
    public DirectionKind getDirection() {
        return direction;
    }

    @Override
    public void setDirection(DirectionKind direction) {
        this.direction = direction;
    }

    @Override
    public int getCurrentStepCount() {
        return currentStepCount;
    }

    @Override
    public void setCurrentStepCount(int currentStepCount) {
        this.currentStepCount = currentStepCount;
    }

    @Override
    public int getTargetStepCount() {
        return targetStepCount;
    }

    @Override
    public void setTargetStepCount(int targetStepCount) {
        this.targetStepCount = targetStepCount;
    }

    @Override
    public boolean getConveyorSensFeed() {
        return conveyorSensFeed;
    }

    @Override
    public void setConveyorSensFeed(boolean conveyorSensFeed) {
        this.conveyorSensFeed = conveyorSensFeed;
    }

    @Override
    public boolean getConveyorSensSwap() {
        return conveyorSensSwap;
    }

    @Override
    public void setConveyorSensSwap(boolean conveyorSensSwap) {
        this.conveyorSensSwap = conveyorSensSwap;
    }

    @Override
    public int getConveyorSensImpulse() {
        return conveyorSensImpulse;
    }

    @Override
    public void setConveyorSensImpulse(int conveyorSensImpulse) {
        this.conveyorSensImpulse = conveyorSensImpulse;
    }

    @Override
    public void stop() {
        log.info("[{}] stop()", id);
    }

    @Override
    public void moveNbSteps() {
        log.info("[{}] moveNbSteps()", id);
    }

    @Override
    public void moveToSensor() {
        log.info("[{}] moveToSensor()", id);
    }

    @Override
    public void moveOut() {
        log.info("[{}] moveOut()", id);
    }

    @Override
    public void statusRequest() {
        log.info("[{}] statusRequest()", id);
    }
}
