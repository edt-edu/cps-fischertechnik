package fr.inria.mbdo.mission.ir;

import lombok.Getter;
import org.eclipse.syson.sysml.Element;

@Getter
public class AcceptEventExprIR extends AcceptExprIR {
    private final TypeRef typeRef;

    public AcceptEventExprIR(Element element, TypeRef typeRef) {
        super(element);
        this.typeRef = typeRef;
    }

    public AcceptEventExprIR(Element element, TypeRef typeRef, String name, String qualifiedName) {
        super(element, name, qualifiedName);
        this.typeRef = typeRef;
    }
}
