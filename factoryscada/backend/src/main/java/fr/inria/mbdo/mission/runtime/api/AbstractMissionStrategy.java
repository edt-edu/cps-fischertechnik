package fr.inria.mbdo.mission.runtime.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.inria.mbdo.mission.runtime.rtc.def.RuntimeState;
import fr.inria.mbdo.mission.runtime.rtc.def.RuntimeTransition;
import fr.inria.mbdo.mission.runtime.rtc.event.Event;
import fr.inria.mbdo.mission.runtime.rtc.exec.RuntimeInstance;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
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

    /**
     * Serializes the missions' reactions to events with {@link #atomically}: a mission swap (stop one mission, start
     * another) runs between two events, so that no event (e.g. the success of a command in progress) is lost between
     * the stop and the start. Reentrant: a mission reacting to an event can publish events to other missions.
     */
    private static final ReentrantLock MISSIONS_LOCK = new ReentrantLock();

    /**
     * Runs {@code action} while no mission reacts to an event.
     */
    public static void atomically(Runnable action) {
        MISSIONS_LOCK.lock();
        try {
            action.run();
        } finally {
            MISSIONS_LOCK.unlock();
        }
    }

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
    public void startAt(String stateName) {
        logger.info("{} starting in state {}", getName(), stateName);
        log("started in " + stateName);
        executing(() -> runtime.startAt(stateName));
    }

    /**
     * Stops the state machine: it ignores the events it receives until it is started again. The commands already
     * sent to the machines are not interrupted (the PLCs finish them).
     */
    @Override
    public void stop() {
        logger.info("{} stopping", getName());
        atomically(runtime::stop);
        log("stopped");
    }

    @Override
    public void forceStop() {
        logger.info("{} force stopping", getName());
        atomically(runtime::stop);
        log("force stopped");
    }

    @Override
    public boolean isRunning() {
        return runtime.isRunning();
    }

    @Override
    public List<String> getStateNames() {
        return runtime.states().stream().map(RuntimeState::getName).toList();
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
        MISSIONS_LOCK.lock();
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
            MISSIONS_LOCK.unlock();
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

    /**
     * Whether the active state waits for the result of a command sent to a machine: one of its transitions is
     * triggered by a command success message (VGRCommandSuccessEventMessage, CBCommandSuccessEventMessage, ...). The
     * states of a zone, or waiting for another machine's message, never are.
     */
    public boolean isAwaitingCommandResult() {
        RuntimeState active = runtime.activeState();
        return active != null && active.getTransitions().stream()
            .map(RuntimeTransition::triggerType)
            .anyMatch(trigger -> trigger != null && trigger.getSimpleName().endsWith("CommandSuccessEventMessage"));
    }
}
