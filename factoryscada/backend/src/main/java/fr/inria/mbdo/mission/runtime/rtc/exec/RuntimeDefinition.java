package fr.inria.mbdo.mission.runtime.rtc.exec;

import java.util.Optional;

import fr.inria.mbdo.mission.runtime.rtc.def.RuntimeState;
import fr.inria.mbdo.mission.runtime.rtc.def.RuntimeTransition;
import fr.inria.mbdo.mission.runtime.rtc.event.Event;

public interface RuntimeDefinition {
    RuntimeTransition entryTransition();

    /** The transition fired in {@code from} by {@code event}, published by {@code source} (null if unknown). */
    Optional<RuntimeTransition> findTransition(RuntimeState from, Event event, Object source);

    default Optional<RuntimeTransition> findTransition(RuntimeState from, Event event) {
        return findTransition(from, event, null);
    }

    Optional<RuntimeTransition> findCompletion(RuntimeState from);
}
