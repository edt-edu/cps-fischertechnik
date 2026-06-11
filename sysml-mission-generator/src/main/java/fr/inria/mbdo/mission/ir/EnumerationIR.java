package fr.inria.mbdo.mission.ir;

import lombok.Getter;

import java.util.List;

@Getter
public final class EnumerationIR extends ElementIR {
    private final List<String> constants;

    public EnumerationIR(IrMetadata metadata, List<String> constants) {
        super(metadata);
        this.constants = constants;
    }
}
