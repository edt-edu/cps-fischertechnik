package fr.inria.mbdo.mission.runtime.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.inria.mbdo.mission.runtime.rtc.def.RuntimeState;
import fr.inria.mbdo.mission.runtime.rtc.event.Event;
import fr.inria.mbdo.mission.runtime.rtc.exec.RuntimeInstance;

import java.time.LocalDateTime;
import java.util.function.Consumer;

public abstract class AbstractMissionStrategy implements MachineMissionStrategy {

    protected final RuntimeInstance runtime;
    private static final Logger logger = LoggerFactory.getLogger(AbstractMissionStrategy.class);
    private Consumer<String> logListener = s -> {};

    protected AbstractMissionStrategy() {
        this.runtime = new RuntimeInstance();
    }

    public void setLogListener(Consumer<String> listener) {
        this.logListener = listener;
        runtime.setStateChangeListener(state -> log("→ " + state));
    }

    private void log(String message) {
        String entry = LocalDateTime.now() + " : [" + getName() + "] " + message;
        logListener.accept(entry);
    }

    @Override
    public void start() {
        logger.info("{} starting", getName());
        log("started");
        runtime.start();
    }

    @Override
    public void stop() {
        logger.info("{} stopping", getName());
        log("stopped");
    }

    @Override
    public void forceStop() {
        logger.info("{} force stopping", getName());
        log("force stopped");
    }

    @Override
    public void onEvent(Event event) {
        logger.info("{} received event {}", getName(), event.getClass().getSimpleName());
        log("event: " + event.getClass().getSimpleName());
        runtime.dispatch(event);
    }

    public String getActiveStateName() {
        RuntimeState active = runtime.activeState();
        return active != null ? active.getName() : null;
    }
}
