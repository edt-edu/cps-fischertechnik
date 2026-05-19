package fr.inria.mbdo.mission.ir;

import lombok.Getter;
import org.eclipse.syson.sysml.Element;

import java.util.List;

@Getter
public class ActionIR extends ElementIR {
    private final List<ParameterIR> parameters;
    private final List<String> bodyStatements;

    public ActionIR(Element element, List<ParameterIR> parameters, List<String> bodyStatements) {
        super(element);
        this.parameters = parameters;
        this.bodyStatements = bodyStatements;
    }
}
