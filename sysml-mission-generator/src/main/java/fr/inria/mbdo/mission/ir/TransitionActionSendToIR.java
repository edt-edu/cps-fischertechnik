package fr.inria.mbdo.mission.ir;

import lombok.Getter;
import org.eclipse.syson.sysml.Element;

import java.util.List;

@Getter
public class TransitionActionSendToIR extends TransitionActionIR {
    private final Ref<MachineMessageIR> message;
    private final MachineRefIR to;

    public TransitionActionSendToIR(Element element, List<ParameterIR> parameters, Ref<MachineMessageIR> message, MachineRefIR to) {
        super(element, parameters);
        this.message = message;
        this.to = to;
    }

    @Override
    public String toString() {
        return "[message=" + message.toString() + ", to=" + to.toString() + "]";
    }
}
