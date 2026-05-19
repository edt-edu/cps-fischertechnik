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
        logger.info("{} received event {}", getName(), event.getClass().getSimpleName());
        runtime.dispatch(event);
    }

    public String getActiveStateName() {
        return runtime.activeState().name();
    }

    protected static final class StateBox {
        private RuntimeState state;

        public StateBox() {
        }

        public void set(RuntimeState s) {
            this.state = s;
        }

        public RuntimeState get() {
            return state;
        }
    }

}
