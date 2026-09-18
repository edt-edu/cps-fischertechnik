package fr.inria.mbdo.mission.ir;

import lombok.Getter;

import java.util.List;

@Getter
public final class StateIR extends ElementIR {
    private final List<Ref<TransitionIR>> transitions;

    public StateIR(IrMetadata metadata, List<Ref<TransitionIR>> transitions) {
        super(metadata);
        this.transitions = transitions;
    }
}
