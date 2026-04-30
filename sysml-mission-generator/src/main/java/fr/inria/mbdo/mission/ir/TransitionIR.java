package fr.inria.mbdo.mission.ir;

import lombok.Getter;
import org.eclipse.syson.sysml.Element;

@Getter
public class TransitionIR extends ElementIR {
    private final Ref<StateIR> to;
    private final Ref<TriggerIR> trigger;
    private final Ref<ActionIR> action;

    public TransitionIR(Element element, Ref<StateIR> to, Ref<TriggerIR> trigger, Ref<ActionIR> action) {
        super(element);
        this.to = to;
        this.trigger = trigger;
        this.action = action;
    }
}
