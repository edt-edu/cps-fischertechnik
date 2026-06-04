package fr.inria.mbdo.mission.ir;

import lombok.Getter;
import org.eclipse.syson.sysml.Element;

@Getter
public abstract class TransitionTriggerIR extends ElementIR {

    public TransitionTriggerIR(Element element) {
        super(element);
    }
}
