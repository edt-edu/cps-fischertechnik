package fr.inria.mbdo.mission.runtime.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.inria.mbdo.mission.runtime.rtc.def.RuntimeState;
import fr.inria.mbdo.mission.runtime.rtc.event.Event;
import fr.inria.mbdo.mission.runtime.rtc.exec.RuntimeInstance;

public class AbstractMissionStrategy implements MachineMissionStrategy {

    protected final RuntimeInstance runtime;
    private static final Logger logger = LoggerFactory.getLogger(AbstractMissionStrategy.class);

    public AbstractMissionStrategy() {
        this.runtime = new RuntimeInstance();
    }

    @Override
    public void start() {
        logger.info("TestMissionAlpha starting");
        runtime.start();
    }

    @Override
    public void stop() {
        logger.info("TestMissionAlpha stopping");
        // no specific stop logic for now, just log
    }

    @Override
    public void forceStop() {
        logger.info("TestMissionAlpha force stopping");
        // no specific force stop logic for now, just log
    }

    @Override
    public String getName() {
        return "TestMissionAlpha";
    }

    @Override
    public void onEvent(Event event) {
        logger.info("TestMissionBeta received event {}", event.getClass().getSimpleName());
        runtime.dispatch(event);
    }

    public String getActiveStateName() {
        RuntimeState active = runtime.activeState();
        return active == null ? null : active.name();
    }

    public RuntimeInstance getRuntime() {
        return runtime;
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
