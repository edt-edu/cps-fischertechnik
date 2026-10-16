package fr.inria.mbdo.mission.runtime.rtc.exec;

import java.util.Optional;

import fr.inria.mbdo.mission.runtime.rtc.def.RuntimeState;
import fr.inria.mbdo.mission.runtime.rtc.def.RuntimeTransition;
import fr.inria.mbdo.mission.runtime.rtc.event.CompletionEvent;
import fr.inria.mbdo.mission.runtime.rtc.event.Event;

public final class SimpleRuntimeDefinition implements RuntimeDefinition {
    private final RuntimeTransition entryTransition;

    public SimpleRuntimeDefinition(RuntimeTransition entryTransition) {
        this.entryTransition = entryTransition;
    }

    @Override
    public RuntimeTransition entryTransition() {
        return entryTransition;
    }

    @Override
    public Optional<RuntimeTransition> findTransition(RuntimeState from, Event event, Object source) {
        return from.getTransitions().stream()
                .filter(t -> t.triggerType().isInstance(event) && t.guard().test(event))
                .filter(t -> t.source() == null || t.source() == source)
                .findFirst();
    }

    @Override
    public Optional<RuntimeTransition> findCompletion(RuntimeState from) {
        return from.getTransitions().stream()
                .filter(t -> t.triggerType().equals(CompletionEvent.class))
                .findFirst();
    }
}
