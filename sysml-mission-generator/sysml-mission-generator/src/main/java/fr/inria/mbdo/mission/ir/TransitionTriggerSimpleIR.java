package fr.inria.mbdo.mission.ir;

import lombok.Getter;

@Getter
public final class TransitionTriggerSimpleIR extends TransitionTriggerIR {
    private final Ref<MachineMessageIR> eventMessage;
    /** Name of the machine given by {@code accept ... via <machine>}, null when the accept has no via. */
    private final String viaMachineName;

    public TransitionTriggerSimpleIR(IrMetadata metadata, Ref<MachineMessageIR> eventMessage) {
        this(metadata, eventMessage, null);
    }

    public TransitionTriggerSimpleIR(IrMetadata metadata, Ref<MachineMessageIR> eventMessage, String viaMachineName) {
        super(metadata);
        this.eventMessage = eventMessage;
        this.viaMachineName = viaMachineName;
    }

    @Override
    public String getQualifiedName() {
        // the via is part of the key: two unnamed transitions accepting the same message via different machines
        // must not share their trigger
        String key = String.format("%s::%s", super.getQualifiedName(), eventMessage.qName());
        return viaMachineName == null ? key : key + " via " + viaMachineName;
    }
}
