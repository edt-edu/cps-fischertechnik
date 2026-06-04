package fr.inria.mbdo.mission.ir;

import lombok.Getter;
import org.eclipse.syson.sysml.Element;

import java.util.List;

@Getter
public class TransitionActionCustomIR extends TransitionActionIR {
    private final List<String> bodyStatements;

    public TransitionActionCustomIR(Element element, List<ParameterIR> parameters, List<String> bodyStatements) {
        super(element, parameters);
        this.bodyStatements = bodyStatements;
    }
}
