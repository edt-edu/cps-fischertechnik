package com.example.runtime.rtc.def;

import lombok.Getter;
import java.util.List;
import com.example.runtime.rtc.exec.RuntimeAction;

@Getter
public class RuntimeState {
    private final String name;
    private List<RuntimeTransition> transitions;
    private final boolean isFinal;

    public RuntimeState( String name) {
        this.name = name;
    }

    public void addTransition(RuntimeTransition transition) {
        this.transitions.add(transition);
    }
}
