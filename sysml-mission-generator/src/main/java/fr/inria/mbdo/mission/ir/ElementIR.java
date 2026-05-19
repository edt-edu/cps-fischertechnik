package fr.inria.mbdo.mission.ir;

import lombok.Getter;
import org.eclipse.syson.sysml.Documentation;
import org.eclipse.syson.sysml.Element;

import java.util.stream.Collectors;

@Getter
public class ElementIR {
    private String name;
    private String qualifiedName;
    private String documentation;
    private String javaPackage;
    private String sourceUri;

    public ElementIR(Element source) {
        this.name = resolveName(source);
        this.qualifiedName = resolveQualifiedName(source);
        this.documentation = source.getDocumentation().stream().map(Documentation::getBody)
                .collect(Collectors.joining("\n"));
        this.javaPackage = resolveJavaPackage(source);
        this.sourceUri = source.eResource() != null ? source.eResource().getURI().toString() : null;
    }

    public ElementIR(Element source, String name, String qualifiedName, String documentation, String javaPackage) {
        this.name = name;
        this.qualifiedName = qualifiedName;
        this.documentation = documentation;
        this.javaPackage = javaPackage;
        this.sourceUri = source != null && source.eResource() != null ? source.eResource().getURI().toString() : null;
    }

    private static String resolveName(Element source) {
        String name = source.getName();
        if (name != null && !name.isBlank()) {
            return name;
        }
        return source.eClass().getName();
    }

    private static String resolveQualifiedName(Element source) {
        String qualifiedName = source.getQualifiedName();
        if (qualifiedName != null && !qualifiedName.isBlank()) {
            return qualifiedName;
        }
        String owner = source.getOwner() != null ? source.getOwner().getQualifiedName() : "";
        String name = resolveName(source);
        return owner == null || owner.isBlank() ? name : owner + "::" + name;
    }

    private static String resolveJavaPackage(Element source) {
        String packageSource = null;
        if (source.getOwner() != null) {
            packageSource = resolveQualifiedName(source.getOwner());
        }
        if (packageSource == null || packageSource.isBlank()) {
            packageSource = resolveQualifiedName(source);
        }

        if (packageSource == null || packageSource.isBlank()) {
            return "";
        }

        return packageSource.replace("::", ".").toLowerCase();
    }
}
