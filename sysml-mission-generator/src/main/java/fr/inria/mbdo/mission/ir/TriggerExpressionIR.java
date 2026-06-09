package fr.inria.mbdo.mission.ir;

import lombok.Getter;
import org.eclipse.syson.sysml.Element;

import java.util.List;

@Getter
public abstract class TriggerExpressionIR extends ElementIR {
    public TriggerExpressionIR(Element element) {
        super(element);
    }

    @Override
    public abstract String getName();

    @Override
    public abstract String toString();

    public abstract List<MachineRefIR> getMachines();
}
