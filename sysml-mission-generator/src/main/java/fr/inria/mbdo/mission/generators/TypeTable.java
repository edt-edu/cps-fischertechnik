package fr.inria.mbdo.mission.generators;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import fr.inria.mbdo.mission.ir.ScalarType;
import fr.inria.mbdo.mission.ir.TypeRef;

import java.util.HashMap;
import java.util.Map;

public class TypeTable {
    private final Map<String, ClassName> userTypes = new HashMap<>();

    public void register(String qualifiedName, ClassName className) {
        userTypes.put(qualifiedName, className);
    }

    public TypeName resolve(TypeRef typeRef) {
        if (typeRef == null) {
            return TypeName.get(Object.class);
        }

        return switch (typeRef.kind()) {
            case SCALAR -> resolveScalar(typeRef.scalarType());
            case USER -> userTypes.getOrDefault(typeRef.qualifiedName(), ClassName.get(Object.class));
            case UNKNOWN -> TypeName.get(Object.class);
        };
    }

    private TypeName resolveScalar(ScalarType scalarType) {
        if (scalarType == null) {
            return TypeName.get(Object.class);
        }
        return switch (scalarType) {
            case BOOLEAN -> TypeName.get(boolean.class);
            case INTEGER -> TypeName.get(int.class);
            case REAL -> TypeName.get(float.class);
        };
    }
}
