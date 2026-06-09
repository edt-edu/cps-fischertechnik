package fr.inria.mbdo.mission.ir;

import lombok.Getter;
import org.eclipse.syson.sysml.Documentation;
import org.eclipse.syson.sysml.Element;

import java.util.stream.Collectors;

@Getter
public class ElementIR {
    private final String namespace;
    private final String name;
    private final String qualifiedName;
    private final String documentation;
    private final String sourceUri;

    public ElementIR(Element source) {
        this.namespace = source.getOwningNamespace().getQualifiedName();
        this.name = source.getName();
        this.qualifiedName = this.namespace + "::" + this.name;
        this.documentation = source.getDocumentation().stream().map(Documentation::getBody)
                .collect(Collectors.joining("\n"));
        this.sourceUri = source.eResource() != null ? source.eResource().getURI().toString() : null;
    }
}
