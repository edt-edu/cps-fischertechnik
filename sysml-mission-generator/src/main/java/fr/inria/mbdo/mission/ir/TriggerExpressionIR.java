package fr.inria.mbdo.mission.ir;

import java.util.List;

/**
 * Base type for the expression tree embedded in an {@code accept when} trigger.
 * These nodes are structural tree nodes, not named SysML model elements,
 * so they do not extend {@link ElementIR}.
 */
public sealed abstract class TriggerExpressionIR
        permits TriggerLiteralExpressionIR, TriggerMachineAttributeExpressionIR, TriggerOperatorExpressionIR {
    public abstract String getName();

    public abstract List<MachineRefIR> getMachines();

    @Override
    public abstract String toString();
}
