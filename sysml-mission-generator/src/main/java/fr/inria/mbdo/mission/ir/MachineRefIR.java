package fr.inria.mbdo.mission.ir;

public record MachineRefIR(String name, String qualifiedName, TypeRef type, boolean attached) {
    public MachineRefIR(String name, String qualifiedName, TypeRef type) {
        this(name, qualifiedName, type, false);
    }
}
