package fr.inria.mbdo.mission.ir;

public sealed abstract class TransitionTriggerIR extends ElementIR
        permits TransitionTriggerSimpleIR, TransitionTriggerWhenIR {
    public TransitionTriggerIR(IrMetadata metadata) {
        super(metadata);
    }
}
