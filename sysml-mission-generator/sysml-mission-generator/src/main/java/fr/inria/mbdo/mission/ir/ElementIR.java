package fr.inria.mbdo.mission.ir;

import lombok.Getter;

@Getter
public sealed class ElementIR
        permits MachineMissionIR, MachineIR, EnumerationIR, CustomTypeIR, MachineActionIR, MachineMessageIR,
        StateIR, TransitionGuardIR, TransitionIR, TransitionTriggerIR, TransitionActionIR {

    private final String namespace;
    private final String name;
    private final String documentation;

    public ElementIR(IrMetadata metadata) {
        this.namespace = metadata.namespace();
        this.name = metadata.name();
        this.documentation = metadata.documentation();
    }

    public String getQualifiedName() {
        return namespace + "::" + name;
    }
}
