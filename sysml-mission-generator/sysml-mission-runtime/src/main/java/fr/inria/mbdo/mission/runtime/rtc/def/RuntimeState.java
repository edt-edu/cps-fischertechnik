package fr.inria.mbdo.mission.runtime.rtc.def;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

@Getter
public class RuntimeState {
    private final String name;
    private final List<RuntimeTransition> transitions = new ArrayList<>();

    public RuntimeState(String name) {
        this.name = name;
    }

    public void addTransition(RuntimeTransition transition) {
        this.transitions.add(transition);
    }
}
