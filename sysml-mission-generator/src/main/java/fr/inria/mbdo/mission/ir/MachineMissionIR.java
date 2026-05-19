package fr.inria.mbdo.mission.ir;

import lombok.Getter;
import org.eclipse.syson.sysml.Element;

import java.util.List;

@Getter
public class MachineMissionIR extends ElementIR {
    private final Ref<TransitionIR> defaultTransition;
    private final List<Ref<StateIR>> states;
    private final List<MachineRefIR> machinesRefs;
    private final List<Ref<ActionIR>> customActions;

    public MachineMissionIR(Element element, Ref<TransitionIR> defaultTransition, List<Ref<StateIR>> states,
            List<MachineRefIR> machinesRefs, List<Ref<ActionIR>> customActions) {
        super(element);
        this.defaultTransition = defaultTransition;
        this.states = states;
        this.machinesRefs = machinesRefs;
        this.customActions = customActions;
    }
}
