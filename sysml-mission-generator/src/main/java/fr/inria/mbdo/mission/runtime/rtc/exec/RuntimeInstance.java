package fr.inria.mbdo.mission.runtime.rtc.exec;

import fr.inria.mbdo.mission.runtime.rtc.def.RuntimeState;
import fr.inria.mbdo.mission.runtime.rtc.def.RuntimeTransition;
import fr.inria.mbdo.mission.runtime.rtc.event.CompletionEvent;
import fr.inria.mbdo.mission.runtime.rtc.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Minimal runtime instance for generated state machines.
 * Supports start(), dispatch(event) and activeState().
 */
public final class RuntimeInstance {
    private RuntimeDefinition def;
    private RuntimeState active;
    private static final Logger logger = LoggerFactory.getLogger(RuntimeInstance.class);

    public synchronized void start() {
        if (def == null) {
            throw new IllegalStateException("Initial state not set");
        }
        RuntimeTransition tr = def.entryTransition();
        try {
            tr.effect().execute(new CompletionEvent());
        } catch (Exception ex) {
            logger.warn("Transition effect threw", ex);
        }
        active = tr.targetState();
        logger.info("RuntimeInstance started in state {}", active.getName());
        // run completion transitions if any
        processCompletions();
    }

    public synchronized void dispatch(Event event) {
        if (active == null) {
            logger.warn("Dispatch called before start(); dropping event {}", event.getClass().getSimpleName());
            return;
        }
        logger.info("Dispatching event {} in state {}", event.getClass().getSimpleName(), active.getName());
        Optional<RuntimeTransition> t = def.findTransition(active, event);
        if (t.isPresent()) {
            RuntimeTransition tr = t.get();
            try {
                tr.effect().execute(event);
            } catch (Exception ex) {
                logger.warn("Transition effect threw", ex);
            }
            active = tr.targetState();
            logger.info("Transitioned to {}", active.getName());
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
        }
    }

    public synchronized RuntimeState activeState() {
        return active;
    }

    public void setEntryTransition(RuntimeTransition entryTransition) {
        this.def = new SimpleRuntimeDefinition(entryTransition);
    }
}
