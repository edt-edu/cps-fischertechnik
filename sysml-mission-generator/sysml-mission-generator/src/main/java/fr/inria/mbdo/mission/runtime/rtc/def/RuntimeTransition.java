package fr.inria.mbdo.mission.runtime.rtc.def;

import fr.inria.mbdo.mission.runtime.rtc.event.Event;
import fr.inria.mbdo.mission.runtime.rtc.exec.RuntimeAction;
import fr.inria.mbdo.mission.runtime.rtc.exec.RuntimeGuard;

/**
 * Runtime transition holding trigger type, guard, effect and the target state.
 *
 * <p>{@code source} is the machine the trigger must be published by ("accept &lt;message&gt; via &lt;machine&gt;"), or
 * null when it may come from any machine.
 */
public record RuntimeTransition(
        Class<? extends Event> triggerType,
        RuntimeGuard guard,
        RuntimeAction effect,
        RuntimeState target,
        Object source) {
    public RuntimeTransition(Class<? extends Event> triggerType, RuntimeGuard guard, RuntimeAction effect,
                             RuntimeState target) {
        this(triggerType, guard, effect, target, null);
    }

    public RuntimeState targetState() {
        return target;
    }
}
