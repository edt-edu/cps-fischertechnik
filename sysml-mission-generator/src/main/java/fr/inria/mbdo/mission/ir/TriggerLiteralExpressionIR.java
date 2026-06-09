package fr.inria.mbdo.mission.ir;

import lombok.Getter;
import org.eclipse.syson.sysml.Element;

import java.util.List;

@Getter
public class TriggerLiteralExpressionIR extends TriggerExpressionIR {
    private final TypeRef type;
    private final String value;

    public TriggerLiteralExpressionIR(Element element, TypeRef type, String value) {
        super(element);
        this.type = type;
        this.value = value;
    }

    @Override
    public String getName() {
        if (type.scalarType().equals(ScalarType.INTEGER) || type.scalarType().equals(ScalarType.REAL)) {
            return "NumericalValue";
        }
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public List<MachineRefIR> getMachines() {
        return List.of();
    }
}
