package fr.inria.mbdo.mission.ir;

import lombok.Getter;
import org.eclipse.syson.sysml.Element;

@Getter
public class ParameterIR extends ElementIR {
    private final TypeRef type;

    public ParameterIR(Element element, TypeRef type) {
        super(element);
        this.type = type;
    }
}
