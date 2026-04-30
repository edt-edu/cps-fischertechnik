package fr.inria.mbdo.mission.ir;

import lombok.Getter;
import org.eclipse.syson.sysml.Element;

import java.util.List;

@Getter
public class StateIR extends ElementIR {
    private final List<Ref<TransitionIR>> transitions;

    public StateIR(Element element, List<Ref<TransitionIR>> transitions) {
        super(element);
        this.transitions = transitions;
    }
}
