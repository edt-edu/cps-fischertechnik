package fr.inria.mbdo.mission.ir;

import lombok.Getter;

import java.util.List;

@Getter
public final class MachineActionIR extends ElementIR {
    private final List<ParameterIR> parameters;

    public MachineActionIR(IrMetadata metadata, List<ParameterIR> parameters) {
        super(metadata);
        this.parameters = parameters;
    }
}
