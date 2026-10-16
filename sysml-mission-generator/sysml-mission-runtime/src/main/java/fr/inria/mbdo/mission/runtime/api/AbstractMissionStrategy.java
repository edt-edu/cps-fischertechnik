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

    /**
     * Mission whose state machine is executing on this thread (starting, or handling an event). Machine commands
     * are sent synchronously from its transition effects, so they are logged in that mission.
     */
    private static final ThreadLocal<AbstractMissionStrategy> EXECUTING_MISSION = new ThreadLocal<>();

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
        executing(runtime::start);
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
        onEvent(null, event);
    }

    /**
     * Handles an event published by {@code source}: a transition triggered "via" a machine only fires on the events
     * that machine publishes.
     */
    public void onEvent(MachineAdapter source, Event event) {
        logger.info("{} received event {}", getName(), event.getClass().getSimpleName());
        log("event: " + event.getClass().getSimpleName());
        executing(() -> runtime.dispatch(event, source));
    }

    private void executing(Runnable execution) {
        AbstractMissionStrategy previous = EXECUTING_MISSION.get();
        EXECUTING_MISSION.set(this);
        try {
            execution.run();
        } finally {
            if (previous == null) {
                EXECUTING_MISSION.remove();
            } else {
                EXECUTING_MISSION.set(previous);
            }
        }
    }

    /**
     * Logs a command sent to a machine in the mission that sent it, if any (see {@link AbstractMachineAdapter#commandSent}).
     */
    static void logCommandOfExecutingMission(String command) {
        AbstractMissionStrategy mission = EXECUTING_MISSION.get();
        if (mission != null) {
            mission.log("command: " + command);
        }
    }

    public String getActiveStateName() {
        RuntimeState active = runtime.activeState();
        return active != null ? active.getName() : null;
    }
}
