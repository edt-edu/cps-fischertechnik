package fr.inria.mbdo.mission.ir;

import lombok.Getter;
import org.eclipse.syson.sysml.Element;

import java.util.List;

@Getter
public class TransitionActionMachineIR extends TransitionActionIR {
    private final MachineRefIR machineRef;

    public TransitionActionMachineIR(Element element, List<ParameterIR> parameters, MachineRefIR machineRef) {
        super(element, parameters);
        this.machineRef = machineRef;
    }
}
