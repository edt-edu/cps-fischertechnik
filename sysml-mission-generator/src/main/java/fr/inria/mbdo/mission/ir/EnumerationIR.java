package fr.inria.mbdo.mission.ir;

import lombok.Getter;
import org.eclipse.syson.sysml.Element;

import java.util.List;

@Getter
public class EnumerationIR extends ElementIR {
    private final List<String> constants;

    public EnumerationIR(Element element, List<String> constants) {
        super(element);
        this.constants = constants;
    }
}
