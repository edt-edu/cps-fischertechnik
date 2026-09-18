package fr.inria.mbdo.mission.ir;

import lombok.Getter;

import java.util.List;

@Getter
public final class MachineIR extends ElementIR {
    private final List<Ref<MachineActionIR>> actions;
    private final List<MachineAttributeIR> attributes;
    private final List<Ref<MachineMessageIR>> messages;

    public MachineIR(IrMetadata metadata, List<Ref<MachineActionIR>> actions,
                     List<MachineAttributeIR> attributes, List<Ref<MachineMessageIR>> messages) {
        super(metadata);
        this.actions = actions;
        this.attributes = attributes;
        this.messages = messages;
    }
}
