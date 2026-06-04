package fr.inria.mbdo.mission.ir;

import lombok.Getter;
import org.eclipse.syson.sysml.Element;

@Getter
public class TransitionGuardIR extends ElementIR{

    public TransitionGuardIR(Element element) {
        super(element);
    }
}
