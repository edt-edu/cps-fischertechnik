package fr.inria.mbdo.mission.generators;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.TypeName;
import fr.inria.mbdo.mission.utils.SymlUtils;
import org.eclipse.syson.sysml.Element;
import org.eclipse.syson.sysml.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * class used to store the generator context i.e. element to be printed in the
 * end note: depending on the transformation/code gen it can be replaced by the
 * return type of the do switch
 */
public class TransformationContext {

    private final String packagePrefix;

    public TransformationContext(String packagePrefix) {
        this.packagePrefix = packagePrefix;
    }

    /**
     * map containing the sysml element as key and the created JavaFile
     */
    public Map<Element, JavaFile> elementToJavaFile = new HashMap<>();

    /**
     * list of indirect type used in the element that need to be generated too
     */
    public Map<String, TypeName> indirectTypesToGenerate = new HashMap<>();

    /**
     * add the given type to generate only if relevant and not already generated
     *
     * @param object
     */
    public void addIndirectTypeToGenerate(Type object) {
        if (!indirectTypesToGenerate.containsKey(object.getQualifiedName())) {
            indirectTypesToGenerate.put(object.getQualifiedName(), ClassName
                    .get(packagePrefix + "." + SymlUtils.getParentJavaPackageQualifiedName(object), object.getName()));
        }
    }

    /**
     * TODO
     *
     * @param type
     * @return
     */
    public TypeName resolveType(Type type) {

        switch (type.getQualifiedName()) {
            case "ScalarValues::Boolean":
                return TypeName.get(boolean.class);
            case "ScalarValues::Integer":
                return TypeName.get(int.class);
            case "ScalarValues::Real":
                return TypeName.get(float.class);
            default:
                if(indirectTypesToGenerate.containsKey(type.getQualifiedName())) {
                    return indirectTypesToGenerate.get(type.getQualifiedName());
                }
                String msg = "Type mapping not implemented for: " + type.getQualifiedName() + ". An example is "+type.getQualifiedName();
                throw new UnsupportedOperationException(msg);
        }
    }

    public String getPackagePrefix() {
        return packagePrefix;
    }

}