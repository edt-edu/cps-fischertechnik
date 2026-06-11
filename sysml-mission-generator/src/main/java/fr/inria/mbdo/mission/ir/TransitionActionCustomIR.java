package fr.inria.mbdo.mission.ir;

import lombok.Getter;

import java.util.List;

@Getter
public final class TransitionActionCustomIR extends TransitionActionIR {
    private final List<String> bodyStatements;

    public TransitionActionCustomIR(IrMetadata metadata, List<ParameterIR> parameters, List<String> bodyStatements) {
        super(metadata, parameters);
        this.bodyStatements = bodyStatements;
    }
}
