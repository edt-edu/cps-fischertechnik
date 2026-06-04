package fr.inria.mbdo.mission.ir;

import lombok.Getter;
import org.eclipse.syson.sysml.Element;

@Getter
public class TransitionTriggerSimpleIR extends TransitionTriggerIR {

    private final Ref<MachineMessageIR> eventMessage;
    private final String acceptEventName;
    private final String acceptEventQualifiedName;
    private final String machineRefName;
    private final String machineTypeQualifiedName;
    private final String conditionMethodName;
    private final String triggerMethodName;
    private final String sysmlSource;

    public TransitionTriggerSimpleIR(Element element, Ref<MachineMessageIR> eventMessage) {
        super(element);
        this.eventMessage = eventMessage;
        this.acceptEventName = null;
        this.acceptEventQualifiedName = null;
        this.machineRefName = null;
        this.machineTypeQualifiedName = null;
        this.conditionMethodName = null;
        this.triggerMethodName = null;
        this.sysmlSource = null;
    }

    @Override
    public String getQualifiedName() {
            return String.format("%s::%s", super.getQualifiedName(), eventMessage.qName());
    }
}
