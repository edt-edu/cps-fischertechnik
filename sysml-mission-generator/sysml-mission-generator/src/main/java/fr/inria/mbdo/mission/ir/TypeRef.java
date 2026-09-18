package fr.inria.mbdo.mission.ir;

public record TypeRef(TypeKind kind, String qualifiedName, ScalarType scalarType) {
    public enum TypeKind {
        SCALAR,
        USER,
        UNKNOWN
    }

    public static TypeRef scalar(ScalarType scalarType) {
        return new TypeRef(TypeKind.SCALAR, null, scalarType);
    }

    public static TypeRef user(String qualifiedName) {
        return new TypeRef(TypeKind.USER, qualifiedName, null);
    }

    public static TypeRef unknown(String qualifiedName) {
        return new TypeRef(TypeKind.UNKNOWN, qualifiedName, null);
    }
}
