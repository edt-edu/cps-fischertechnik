package fr.inria.mbdo.mission.runtime.rtc.exec;

import fr.inria.mbdo.mission.runtime.rtc.def.RuntimeState;
import fr.inria.mbdo.mission.runtime.rtc.def.RuntimeTransition;
import fr.inria.mbdo.mission.runtime.rtc.event.CompletionEvent;
import fr.inria.mbdo.mission.runtime.rtc.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Minimal runtime instance for generated state machines.
 * Supports start(), dispatch(event) and activeState().
 *
 * <p>Run-to-completion: an event received while this instance is executing a transition (e.g. the synchronous
 * answer to a message its transition effect just sent to another mission) is queued and processed once that
 * transition has completed, i.e. in the transition's target state.
 */
public final class RuntimeInstance {
    private RuntimeDefinition def;
    private RuntimeState active;
    private final Deque<PendingEvent> pendingEvents = new ArrayDeque<>();
    private boolean processing;
    private static final Logger logger = LoggerFactory.getLogger(RuntimeInstance.class);
    private Consumer<String> stateChangeListener = s -> {};

    public void setStateChangeListener(Consumer<String> listener) {
        this.stateChangeListener = listener;
    }

    public synchronized void start() {
        if (def == null) {
            throw new IllegalStateException("Initial state not set");
        }
        processing = true;
        try {
            RuntimeTransition tr = def.entryTransition();
            try {
                tr.effect().execute(new CompletionEvent());
            } catch (Exception ex) {
                logger.warn("Transition effect threw", ex);
            }
            active = tr.targetState();
            logger.info("RuntimeInstance started in state {}", active.getName());
            stateChangeListener.accept(active.getName());
            processCompletions();
        } finally {
            processing = false;
        }
        processPendingEvents();
    }

    /**
     * Starts the state machine directly in {@code stateName}, without running the entry transition's effect: used to
     * migrate a machine from another mission, once its state has been reconciled with that state. Completion
     * transitions leaving {@code stateName} are run, as if the state had been entered by a transition.
     *
     * @throws IllegalArgumentException if the state machine has no state named {@code stateName}
     */
    public synchronized void startAt(String stateName) {
        if (def == null) {
            throw new IllegalStateException("Initial state not set");
        }
        RuntimeState state = findState(stateName)
                .orElseThrow(() -> new IllegalArgumentException("Unknown state: " + stateName));
        pendingEvents.clear();
        processing = true;
        try {
            active = state;
            logger.info("RuntimeInstance started in state {} (migration)", active.getName());
            stateChangeListener.accept(active.getName());
            processCompletions();
        } finally {
            processing = false;
        }
        processPendingEvents();
    }

    /**
     * Stops the state machine: it leaves its active state and drops the events it receives until it is started
     * again. The commands already sent to the machines are not interrupted.
     */
    public synchronized void stop() {
        pendingEvents.clear();
        active = null;
    }

    public synchronized boolean isRunning() {
        return active != null;
    }

    /** The states reachable from the entry transition, in discovery order. */
    public List<RuntimeState> states() {
        if (def == null) {
            return List.of();
        }
        Map<String, RuntimeState> states = new LinkedHashMap<>();
        Deque<RuntimeState> toVisit = new ArrayDeque<>();
        toVisit.add(def.entryTransition().targetState());
        while (!toVisit.isEmpty()) {
            RuntimeState state = toVisit.poll();
            if (states.putIfAbsent(state.getName(), state) == null) {
                state.getTransitions().forEach(t -> toVisit.add(t.targetState()));
            }
        }
        return List.copyOf(states.values());
    }

    public Optional<RuntimeState> findState(String stateName) {
        return states().stream().filter(s -> s.getName().equals(stateName)).findFirst();
    }

    /** An event and the machine that published it (null if unknown). */
    private record PendingEvent(Event event, Object source) {
    }

    public void dispatch(Event event) {
        dispatch(event, null);
    }

    /**
     * Dispatches an event published by {@code source}: a transition with a source only fires on the events of that
     * source.
     */
    public synchronized void dispatch(Event event, Object source) {
        if (active == null) {
            logger.debug("Not started (or stopped); dropping event {}", event.getClass().getSimpleName());
            return;
        }
        pendingEvents.add(new PendingEvent(event, source));
        if (processing) {
            logger.info("Queued event {} until the transition in progress from state {} completes",
                    event.getClass().getSimpleName(), active.getName());
            return;
        }
        processPendingEvents();
    }

    private void processPendingEvents() {
        processing = true;
        try {
            PendingEvent next;
            while (active != null && (next = pendingEvents.poll()) != null) {
                processEvent(next.event(), next.source());
            }
        } finally {
            processing = false;
        }
    }

    private void processEvent(Event event, Object source) {
        logger.info("Dispatching event {} in state {}", event.getClass().getSimpleName(), active.getName());
        Optional<RuntimeTransition> t = def.findTransition(active, event, source);
        if (t.isPresent()) {
            RuntimeTransition tr = t.get();
            try {
                tr.effect().execute(event);
            } catch (Exception ex) {
                logger.warn("Transition effect threw", ex);
            }
            active = tr.targetState();
            logger.info("Transitioned to {}", active.getName());
            stateChangeListener.accept(active.getName());
            processCompletions();
        } else {
            logger.debug("No transition for event {} in state {}", event.getClass().getSimpleName(), active.getName());
        }
    }

    private void processCompletions() {
        while (true) {
            Optional<RuntimeTransition> c = def.findCompletion(active);
            if (c.isEmpty())
                break;
            RuntimeTransition tr = c.get();
            try {
                tr.effect().execute(new CompletionEvent());
            } catch (Exception ex) {
                logger.warn("Completion effect threw", ex);
            }
            active = tr.targetState();
            logger.info("Completion transitioned to {}", active.getName());
            stateChangeListener.accept(active.getName());
        }
    }

    public synchronized RuntimeState activeState() {
        return active;
    }

    /**
     * Returns the initial state (target of the entry transition) without starting
     * the machine.
     * Used for UI introspection of the state graph.
     */
    public RuntimeState initialState() {
        return def != null ? def.entryTransition().targetState() : null;
    }

    public void setEntryTransition(RuntimeTransition entryTransition) {
        this.def = new SimpleRuntimeDefinition(entryTransition);
    }
}
