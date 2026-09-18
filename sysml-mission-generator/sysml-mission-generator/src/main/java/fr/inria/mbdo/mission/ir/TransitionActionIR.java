package fr.inria.mbdo.mission.ir;

import lombok.Getter;

import java.util.List;

@Getter
public sealed abstract class TransitionActionIR extends ElementIR
        permits TransitionActionCustomIR, TransitionActionMachineIR, TransitionActionSendToIR {
    private final List<ParameterIR> parameters;

    public TransitionActionIR(IrMetadata metadata, List<ParameterIR> parameters) {
        super(metadata);
        this.parameters = parameters;
    }

    @Override
    public String getQualifiedName() {
        return getClass().getName() + ":" + super.getQualifiedName();
    }
}
