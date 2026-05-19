package fr.inria.mbdo.mission.ir;

import lombok.Getter;
import org.eclipse.syson.sysml.Element;

@Getter
public class TriggerIR extends ElementIR {
    private final Ref<AcceptExprIR> expr;

    public TriggerIR(Element element, Ref<AcceptExprIR> expr) {
        super(element);
        this.expr = expr;
    }
}
