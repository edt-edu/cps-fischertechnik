package fr.inria.mbdo.mission.ir;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.eclipse.syson.sysml.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
public class MachineIR extends ElementIR {
    private final List<ActionIR> actions;
    private final List<MachineAttributeIR> attributes;
    private final List<MachineMessageIR> messages;

    public  MachineIR(Element element, List<ActionIR> actions, List<MachineAttributeIR> attributes, List<MachineMessageIR> messages) {
        super(element);
        this.actions = actions;
        this.attributes = attributes;
        this.messages = messages;
    }
}
