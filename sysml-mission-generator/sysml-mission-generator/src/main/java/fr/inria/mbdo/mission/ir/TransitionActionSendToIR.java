package fr.inria.mbdo.mission.ir;

import lombok.Getter;

import java.util.List;

@Getter
public final class TransitionActionSendToIR extends TransitionActionIR {
    private final MachineMessageRefIR message;
    private final MachineRefIR to;

    public TransitionActionSendToIR(IrMetadata metadata, List<ParameterIR> parameters,
                                    MachineMessageRefIR message, MachineRefIR to) {
        super(metadata, parameters);
        this.message = message;
        this.to = to;
    }

    @Override
    public String getQualifiedName() {
        return getClass().getName() + ":" + message.name() + "->" + to.name();
    }

    @Override
    public String toString() {
        return "[message=" + message.toString() + ", to=" + to.toString() + "]";
    }
}
