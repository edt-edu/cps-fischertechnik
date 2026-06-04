package fr.inria.mbdo.mission.ir;

import lombok.Getter;
import org.eclipse.syson.sysml.Element;

@Getter
public class MachineMessageIR extends ElementIR {
    public MachineMessageIR(Element element) {
        super(element);
    }
}
