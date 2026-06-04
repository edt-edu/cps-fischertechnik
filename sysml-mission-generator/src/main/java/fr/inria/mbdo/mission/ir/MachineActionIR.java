package fr.inria.mbdo.mission.ir;

import lombok.Getter;
import org.eclipse.syson.sysml.Element;

import java.util.List;

@Getter
public class MachineActionIR extends ElementIR {
    private final List<ParameterIR> parameters;

    public MachineActionIR(Element element, List<ParameterIR> parameters) {
        super(element);
        this.parameters = parameters;
    }
}
