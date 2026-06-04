package fr.inria.mbdo.mission.ir;

import lombok.Getter;
import org.eclipse.syson.sysml.Element;

import java.util.List;
import java.util.Map;

@Getter
public class MachineIR extends ElementIR {
    private final Map<String, Ref<MachineActionIR>> actions;
    private final List<MachineAttributeIR> attributes;
    private final List<Ref<MachineMessageIR>> messages;

    public MachineIR(Element element, Map<String, Ref<MachineActionIR>> actions, List<MachineAttributeIR> attributes,
                     List<Ref<MachineMessageIR>> messages) {
        super(element);
        this.actions = actions;
        this.attributes = attributes;
        this.messages = messages;
    }
}
