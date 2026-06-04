package fr.inria.mbdo.mission.ir;

import lombok.Getter;
import org.eclipse.syson.sysml.Element;

@Getter
public class TransitionTriggerWhenIR extends TransitionTriggerIR {

    private final String acceptEventName;
    private final String acceptEventQualifiedName;
    private final String machineRefName;
    private final String machineTypeQualifiedName;
    private final String conditionMethodName;
    private final String triggerMethodName;
    private final String sysmlSource;

    public TransitionTriggerWhenIR(Element element, String acceptEventName, String acceptEventQualifiedName, String machineRefName, String machineTypeQualifiedName, String conditionMethodName, String triggerMethodName, String sysmlSource) {
        super(element);
        this.acceptEventName = acceptEventName;
        this.acceptEventQualifiedName = acceptEventQualifiedName;
        this.machineRefName = machineRefName;
        this.machineTypeQualifiedName = machineTypeQualifiedName;
        this.conditionMethodName = conditionMethodName;
        this.triggerMethodName = triggerMethodName;
        this.sysmlSource = sysmlSource;
    }

    @Override
    public String getQualifiedName() {
        return String.format("%s::%s", super.getQualifiedName(), acceptEventQualifiedName);
    }
}
