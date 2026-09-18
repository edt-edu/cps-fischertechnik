package fr.inria.mbdo.mission.ir;

import fr.inria.mbdo.mission.utils.StringUtils;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public final class TriggerOperatorExpressionIR extends TriggerExpressionIR {
    private final String operator;
    private final TriggerExpressionIR leftPart;
    private final TriggerExpressionIR rightPart;

    public TriggerOperatorExpressionIR(String operator, TriggerExpressionIR leftPart,
                                       TriggerExpressionIR rightPart) {
        this.operator = operator;
        this.leftPart = leftPart;
        this.rightPart = rightPart;
    }

    @Override
    public String getName() {
        String operatorStr = switch (operator) {
            case "==" -> "Equals";
            default -> StringUtils.toUpperFirst(operator);
        };
        return leftPart.getName() + operatorStr + rightPart.getName();
    }

    @Override
    public String toString() {
        return leftPart + " " + operator + " " + rightPart;
    }

    @Override
    public List<MachineRefIR> getMachines() {
        ArrayList<MachineRefIR> machines = new ArrayList<>();
        machines.addAll(leftPart.getMachines());
        machines.addAll(rightPart.getMachines());
        return machines;
    }
}
