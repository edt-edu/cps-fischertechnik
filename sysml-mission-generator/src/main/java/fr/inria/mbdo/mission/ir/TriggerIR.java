package fr.inria.mbdo.mission.ir;

import lombok.Getter;
import org.eclipse.syson.sysml.Element;

@Getter
public class TriggerIR extends ElementIR {
    private final String expr; // TODO

    public TriggerIR(Element element, String expr) {
        super(element);
        this.expr = expr;
    }
}
