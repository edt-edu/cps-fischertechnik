package fr.inria.mbdo.mission.ir;

import fr.inria.mbdo.mission.utils.StringUtils;
import lombok.Getter;
import org.eclipse.syson.sysml.Element;

import java.util.List;

@Getter
public class TriggerMachineAttributeExpressionIR extends TriggerExpressionIR {
    private final MachineRefIR machineRef;
    private final MachineAttributeRefIR attributeRef;

    public TriggerMachineAttributeExpressionIR(Element element, MachineRefIR machineRef, MachineAttributeRefIR attributeRef) {
        super(element);
        this.machineRef = machineRef;
        this.attributeRef = attributeRef;
    }

    @Override
    public String getName() {
        return StringUtils.toUpperFirst(machineRef.name()) + StringUtils.toUpperFirst(attributeRef.name());
    }

    @Override
    public String toString() {
        return machineRef.name() + "." + attributeRef.name();
    }

    @Override
    public List<MachineRefIR> getMachines() {
        return List.of(machineRef);
    }
}
