package fr.inria.mbdo.mission.ir;

import lombok.Getter;
import org.eclipse.syson.sysml.Element;

import java.util.List;

@Getter
public class MachineMissionIR extends ElementIR {
    private final Ref<StateIR> defaultState;
    private final List<Ref<StateIR>> states;
    private final List<MachineRefIR> machinesRefs;
    private final List<Ref<ActionIR>> customActions;

    public MachineMissionIR(Element element, Ref<StateIR> defaultState, List<Ref<StateIR>> states,
            List<MachineRefIR> machinesRefs, List<Ref<ActionIR>> customActions) {
        super(element);
        this.defaultState = defaultState;
        this.states = states;
        this.machinesRefs = machinesRefs;
        this.customActions = customActions;
    }
}
