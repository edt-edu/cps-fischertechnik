package fr.inria.mbdo.mission.ir;

import lombok.Getter;
import org.eclipse.syson.sysml.Element;

import java.util.List;

@Getter
public abstract class TransitionActionIR extends ElementIR {
    private final List<ParameterIR> parameters;

    public TransitionActionIR(Element element, List<ParameterIR> parameters) {
        super(element);
        this.parameters = parameters;
    }

    @Override
    public String getQualifiedName() {
        return getClass().getName() + ":" + super.getQualifiedName();
    }
}
