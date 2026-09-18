package fr.inria.mbdo.mission.ir;

import lombok.Getter;

@Getter
public final class TransitionTriggerSimpleIR extends TransitionTriggerIR {
    private final Ref<MachineMessageIR> eventMessage;

    public TransitionTriggerSimpleIR(IrMetadata metadata, Ref<MachineMessageIR> eventMessage) {
        super(metadata);
        this.eventMessage = eventMessage;
    }

    @Override
    public String getQualifiedName() {
        return String.format("%s::%s", super.getQualifiedName(), eventMessage.qName());
    }
}
