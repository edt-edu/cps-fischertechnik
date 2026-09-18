package fr.inria.mbdo.mission.runtime.rtc.def;

import fr.inria.mbdo.mission.runtime.rtc.event.Event;
import fr.inria.mbdo.mission.runtime.rtc.exec.RuntimeAction;
import fr.inria.mbdo.mission.runtime.rtc.exec.RuntimeGuard;

/**
 * Runtime transition holding trigger type, guard, effect and the target state.
 */
public record RuntimeTransition(
        Class<? extends Event> triggerType,
        RuntimeGuard guard,
        RuntimeAction effect,
        RuntimeState target) {
    public RuntimeState targetState() {
        return target;
    }
}
