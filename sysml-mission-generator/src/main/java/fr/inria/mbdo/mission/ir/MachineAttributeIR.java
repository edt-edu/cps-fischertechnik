package fr.inria.mbdo.mission.ir;

import lombok.Getter;
import org.eclipse.syson.sysml.Element;

@Getter
public class MachineAttributeIR extends ElementIR {
    private TypeRef type;

    public MachineAttributeIR(Element element, TypeRef type) {
        super(element);
        this.type = type;
    }
}
