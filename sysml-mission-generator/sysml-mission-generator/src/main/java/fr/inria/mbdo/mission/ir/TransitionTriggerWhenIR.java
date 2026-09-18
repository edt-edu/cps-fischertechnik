package fr.inria.mbdo.mission.ir;

import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

@Getter
public final class TransitionTriggerWhenIR extends TransitionTriggerIR {
    private final TriggerExpressionIR expression;

    public TransitionTriggerWhenIR(IrMetadata metadata, TriggerExpressionIR expression) {
        super(metadata);
        this.expression = expression;
    }

    @Override
    public String getName() {
        return "AcceptWhen" + expression.getName() + "Event";
    }

    @Override
    public String getQualifiedName() {
        return getNamespace() + "::" + getName();
    }

    @Override
    public String toString() {
        return expression.toString();
    }

    public MachineRefIR getAssociatedMachine() {
        Set<MachineRefIR> machines = new HashSet<>(expression.getMachines());
        if (machines.size() == 1) {
            return machines.iterator().next();
        } else if (machines.size() > 1) {
            throw new IllegalStateException("Multiple machines associated to same 'accept-when' expression.");
        }
        return null;
    }
}
