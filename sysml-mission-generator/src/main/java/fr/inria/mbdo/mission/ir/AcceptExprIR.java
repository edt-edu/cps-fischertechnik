package fr.inria.mbdo.mission.ir;

import lombok.Getter;
import org.eclipse.syson.sysml.Element;

@Getter
public abstract class AcceptExprIR extends ElementIR{
    protected AcceptExprIR(Element element) {
        super(element);
    }

    protected AcceptExprIR(Element element, String name, String qualifiedName) {
        super(element, name, qualifiedName,
                element != null ? element.getDocumentation().stream().map(org.eclipse.syson.sysml.Documentation::getBody)
                        .collect(java.util.stream.Collectors.joining("\n")) : "",
                element != null && element.getOwner() != null && element.getOwner().getQualifiedName() != null
                        ? element.getOwner().getQualifiedName().replace("::", ".").toLowerCase()
                        : "");
    }
}
