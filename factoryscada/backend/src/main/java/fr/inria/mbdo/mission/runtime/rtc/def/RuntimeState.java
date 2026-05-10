package fr.inria.mbdo.mission.runtime.rtc.def;

import java.util.List;
import fr.inria.mbdo.mission.runtime.rtc.exec.RuntimeAction;

public record RuntimeState(
        String name,
        List<RuntimeTransition> transitions,
        List<RuntimeAction> entryActions,
        List<RuntimeAction> exitActions,
        boolean isFinal) {
}
