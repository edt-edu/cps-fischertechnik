package fr.inria.mbdo.mission.ir;

import lombok.Getter;

import java.util.List;

@Getter
public final class MachineMissionIR extends ElementIR {
    private final Ref<TransitionIR> defaultTransition;
    private final List<Ref<StateIR>> states;
    private final List<MachineRefIR> machinesRefs;

    public MachineMissionIR(IrMetadata metadata, Ref<TransitionIR> defaultTransition,
                            List<Ref<StateIR>> states, List<MachineRefIR> machinesRefs) {
        super(metadata);
        this.defaultTransition = defaultTransition;
        this.states = states;
        this.machinesRefs = machinesRefs;
    }
}
