package fr.inria.mbdo.mission.ir;

import lombok.Getter;
import org.eclipse.syson.sysml.Element;

@Getter
public class TransitionTriggerSimpleIR extends TransitionTriggerIR {

    private final Ref<MachineMessageIR> eventMessage;

    public TransitionTriggerSimpleIR(Element element, Ref<MachineMessageIR> eventMessage) {
        super(element);
        this.eventMessage = eventMessage;
    }

    @Override
    public String getQualifiedName() {
        return String.format("%s::%s", super.getQualifiedName(), eventMessage.qName());
    }
}
