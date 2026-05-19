package fr.inria.mbdo.mission.generators;

import fr.inria.mbdo.mission.ir.*;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class IrRepository {
    protected Map<String, MachineMissionIR> missions = new HashMap<>();
    protected Map<String, MachineIR> machines = new HashMap<>();
    protected Map<String, EnumerationIR> enumerations = new HashMap<>();
    protected Map<String, ActionIR> actions = new HashMap<>();
    protected Map<String, MachineMessageIR> messages = new HashMap<>();
    protected Map<String, AcceptExprIR> acceptExprs = new HashMap<>();
    protected Map<String, StateIR> states = new HashMap<>();
    protected Map<String, TransitionIR> transitions = new HashMap<>();
    protected Map<String, TriggerIR> triggers = new HashMap<>();

    public static class Builder {

        private final IrRepository irRepository = new IrRepository();

        public Builder add(ElementIR elementIR) {
            String name = elementIR.getQualifiedName();
            switch (elementIR) {
                case MachineMissionIR mission -> this.irRepository.missions.put(name, mission);
                case MachineIR machine -> this.irRepository.machines.put(name, machine);
                case EnumerationIR enumeration -> this.irRepository.enumerations.put(name, enumeration);
                case ActionIR action -> this.irRepository.actions.put(name, action);
                case MachineMessageIR message -> this.irRepository.messages.put(name, message);
                case AcceptExprIR acceptExprIR -> this.irRepository.acceptExprs.put(name, acceptExprIR);
                case StateIR state -> this.irRepository.states.put(name, state);
                case TransitionIR transition -> this.irRepository.transitions.put(name, transition);
                case TriggerIR trigger -> this.irRepository.triggers.put(name, trigger);
                default -> throw new IllegalStateException("Unexpected value: " + elementIR.getQualifiedName());
            }
            return this;
        }

        public IrRepository build() {
            return irRepository;
        }
    }
}
