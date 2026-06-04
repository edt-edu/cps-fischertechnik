package fr.inria.mbdo.mission.runtime.rtc.def;

import java.util.List;

import fr.inria.mbdo.mission.runtime.rtc.exec.RuntimeAction;
import lombok.Getter;

@Getter
public class RuntimeState {
    private final String name;
    private List<RuntimeTransition> transitions;

    public RuntimeState(String name) {
        this.name = name;
    }

    public void addTransition(RuntimeTransition transition) {
        this.transitions.add(transition);
    }
}
