package fr.inria.mbdo.mission.ir;

import lombok.Getter;

import java.util.List;

@Getter
public final class TransitionActionMachineIR extends TransitionActionIR {
    private final MachineRefIR machineRef;

    public TransitionActionMachineIR(IrMetadata metadata, List<ParameterIR> parameters, MachineRefIR machineRef) {
        super(metadata, parameters);
        this.machineRef = machineRef;
    }
}
