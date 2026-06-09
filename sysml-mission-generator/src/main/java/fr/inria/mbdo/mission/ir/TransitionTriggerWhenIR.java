package fr.inria.mbdo.mission.ir;

import lombok.Getter;
import org.eclipse.syson.sysml.Element;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
public class TransitionTriggerWhenIR extends TransitionTriggerIR {

    private final TriggerExpressionIR expression;
    private final String triggerEventName;

    public TransitionTriggerWhenIR(Element element, String triggerEventName, TriggerExpressionIR expression) {
        super(element);
        this.triggerEventName = triggerEventName;
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
