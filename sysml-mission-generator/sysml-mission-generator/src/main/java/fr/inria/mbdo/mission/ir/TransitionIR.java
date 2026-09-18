package fr.inria.mbdo.mission.ir;

import lombok.Getter;

@Getter
public final class TransitionIR extends ElementIR {
    private final Ref<StateIR> fromState;
    private final Ref<StateIR> toState;
    private final Ref<TransitionTriggerIR> trigger;
    private final Ref<TransitionGuardIR> guard;
    private final Ref<TransitionActionIR> action;

    public TransitionIR(IrMetadata metadata, Ref<StateIR> from, Ref<StateIR> to,
                        Ref<TransitionTriggerIR> trigger, Ref<TransitionGuardIR> guard,
                        Ref<TransitionActionIR> action) {
        super(metadata);
        this.fromState = from;
        this.toState = to;
        this.trigger = trigger;
        this.guard = guard;
        this.action = action;
    }

    @Override
    public String getQualifiedName() {
        String baseKey = super.getQualifiedName();
        String trigger = getTrigger() != null ? getTrigger().qName() : "implicit";
        String source = getFromState() != null ? getFromState().qName() : "unknown";
        String target = getToState() != null ? getToState().qName() : "unknown";
        return baseKey + "@" + trigger + "|" + source + "|" + target;
    }
}
