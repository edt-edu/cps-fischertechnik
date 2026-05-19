package fr.inria.mbdo.mission.runtime.rtc.def;

import java.util.function.Supplier;
import fr.inria.mbdo.mission.runtime.rtc.event.Event;
import fr.inria.mbdo.mission.runtime.rtc.exec.RuntimeAction;
import fr.inria.mbdo.mission.runtime.rtc.exec.RuntimeGuard;

public record RuntimeTransition(
        Class<? extends Event> triggerType,
        RuntimeGuard guard,
        RuntimeAction effect,
        Supplier<RuntimeState> target) {
    public RuntimeState targetState() {
        return target.get();
    }
}
