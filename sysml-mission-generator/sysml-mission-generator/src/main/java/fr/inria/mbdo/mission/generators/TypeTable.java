package fr.inria.mbdo.mission.generators;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import fr.inria.mbdo.mission.ir.Ref;
import fr.inria.mbdo.mission.ir.ScalarType;
import fr.inria.mbdo.mission.ir.TypeRef;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TypeTable {
    private final Map<String, ClassName> userTypes = new HashMap<>();

    public void register(String qualifiedName, ClassName className) {
        userTypes.put(qualifiedName, className);
    }

    /**
     * Resolve a TypeRef to a Java TypeName.
     * Handles scalars, user-defined types (by qualified name lookup), and unknowns.
     */
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

    /**
     * Resolve a Ref (qualified-name reference) to a Java TypeName.
     * This is used when you have a Ref&lt;T&gt; pointing at any registered IR
     * element.
     */
    public TypeName resolve(Ref<?> ref) {
        if (ref == null || ref.qName() == null || ref.qName().isBlank()) {
            return TypeName.get(Object.class);
        }
        return userTypes.getOrDefault(ref.qName(), ClassName.get(Object.class));
    }

    /**
     * Resolve a qualified name to its registered ClassName.
     * Returns empty if not registered.
     */
    public Optional<ClassName> resolveClassName(String qualifiedName) {
        if (qualifiedName == null || qualifiedName.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(userTypes.get(qualifiedName));
    }

    /**
     * Get the Java class for a registered qualified name, throwing if not found.
     */
    public ClassName resolveClassNameOrThrow(String qualifiedName) {
        return resolveClassName(qualifiedName)
                .orElseThrow(() -> new IllegalStateException(
                        "No registered Java class for qualified name: " + qualifiedName));
    }

    /**
     * Get the Java package for a registered qualified name.
     * Returns empty if the element is not registered.
     */
    public Optional<String> resolvePackage(String qualifiedName) {
        if (qualifiedName == null || qualifiedName.isBlank()) {
            return Optional.empty();
        }
        ClassName className = userTypes.get(qualifiedName);
        if (className == null) {
            return Optional.empty();
        }
        return Optional.of(className.packageName());
    }

    /**
     * Get the Java package for a registered qualified name, throwing if not found.
     */
    public String resolvePackageOrThrow(String qualifiedName) {
        return resolvePackage(qualifiedName)
                .orElseThrow(() -> new IllegalStateException(
                        "No registered Java package for qualified name: " + qualifiedName));
    }

    /**
     * Check whether a type is registered by its qualified name.
     */
    public boolean isRegistered(String qualifiedName) {
        return qualifiedName != null && userTypes.containsKey(qualifiedName);
    }

    private TypeName resolveScalar(ScalarType scalarType) {
        if (scalarType == null) {
            return TypeName.get(Object.class);
        }
        return switch (scalarType) {
            case BOOLEAN -> TypeName.get(boolean.class);
            case INTEGER -> TypeName.get(int.class);
            case REAL -> TypeName.get(float.class);
            case STRING -> TypeName.get(String.class);
        };
    }
}
