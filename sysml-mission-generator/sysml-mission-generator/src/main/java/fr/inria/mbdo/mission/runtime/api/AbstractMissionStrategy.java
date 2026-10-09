package fr.inria.mbdo.mission.runtime.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.inria.mbdo.mission.runtime.rtc.def.RuntimeState;
import fr.inria.mbdo.mission.runtime.rtc.event.Event;
import fr.inria.mbdo.mission.runtime.rtc.exec.RuntimeInstance;

public abstract class AbstractMissionStrategy implements MachineMissionStrategy {

    protected final RuntimeInstance runtime;
    private static final Logger logger = LoggerFactory.getLogger(AbstractMissionStrategy.class);

    protected AbstractMissionStrategy() {
        this.runtime = new RuntimeInstance();
    }

    @Override
    public void start() {
        logger.info("{} starting", getName());
        runtime.start();
    }

    @Override
    public void stop() {
        logger.info("{} stopping", getName());
        // no specific stop logic for now, just log
    }

    @Override
    public void forceStop() {
        logger.info("{} force stopping", getName());
        // no specific force stop logic for now, just log
    }

    @Override
    public void onEvent(Event event) {
        onEvent(null, event);
    }

    /**
     * Handles an event published by {@code source}: a transition triggered "via" a machine only fires on the events
     * that machine publishes.
     */
    public void onEvent(MachineAdapter source, Event event) {
        logger.info("{} received event {}", getName(), event.getClass().getSimpleName());
        runtime.dispatch(event, source);
    }

    @Override
    public String getActiveStateName() {
        return runtime.activeState().getName();
    }
}
