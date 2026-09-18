package fr.inria.mbdo.mission.ir;

import lombok.Getter;

import java.util.List;

@Getter
public final class CustomTypeIR extends ElementIR {
    private final List<ParameterIR> parameters;

    public CustomTypeIR(IrMetadata metadata, List<ParameterIR> parameters) {
        super(metadata);
        this.parameters = parameters;
    }
}
