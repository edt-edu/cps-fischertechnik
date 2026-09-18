package fr.inria.mbdo.mission.generators;

import fr.inria.mbdo.mission.ir.*;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class IrRepository {
    private final Map<String, MachineMissionIR> missions;
    private final Map<String, MachineIR> machines;
    private final Map<String, EnumerationIR> enumerations;
    private final Map<String, MachineActionIR> actions;
    private final Map<String, MachineMessageIR> messages;
    private final Map<String, StateIR> states;
    private final Map<String, TransitionIR> transitions;
    private final Map<String, TransitionTriggerIR> triggers;
    private final Map<String, TransitionActionIR> transitionActions;
    private final Map<String, CustomTypeIR> customTypes;

    private IrRepository(Builder b) {
        this.missions = Map.copyOf(b.missions);
        this.machines = Map.copyOf(b.machines);
        this.enumerations = Map.copyOf(b.enumerations);
        this.actions = Map.copyOf(b.actions);
        this.messages = Map.copyOf(b.messages);
        this.states = Map.copyOf(b.states);
        this.transitions = Map.copyOf(b.transitions);
        this.triggers = Map.copyOf(b.triggers);
        this.transitionActions = Map.copyOf(b.transitionActions);
        this.customTypes = Map.copyOf(b.customTypes);
    }

    public static class Builder {
        private final Map<String, MachineMissionIR> missions = new HashMap<>();
        private final Map<String, MachineIR> machines = new HashMap<>();
        private final Map<String, EnumerationIR> enumerations = new HashMap<>();
        private final Map<String, MachineActionIR> actions = new HashMap<>();
        private final Map<String, MachineMessageIR> messages = new HashMap<>();
        private final Map<String, StateIR> states = new HashMap<>();
        private final Map<String, TransitionIR> transitions = new HashMap<>();
        private final Map<String, TransitionTriggerIR> triggers = new HashMap<>();
        private final Map<String, TransitionActionIR> transitionActions = new HashMap<>();
        private final Map<String, CustomTypeIR> customTypes = new HashMap<>();

        public Builder add(ElementIR elementIR) {
            String name = elementIR.getQualifiedName();
            switch (elementIR) {
                case MachineMissionIR mission -> missions.put(name, mission);
                case MachineIR machine -> machines.put(name, machine);
                case EnumerationIR enumeration -> enumerations.put(name, enumeration);
                case CustomTypeIR customType -> customTypes.put(name, customType);
                case MachineActionIR action -> actions.put(name, action);
                case MachineMessageIR message -> messages.put(name, message);
                case StateIR state -> states.put(name, state);
                case TransitionIR transition -> transitions.put(name, transition);
                // TransitionTriggerIR subtypes
                case TransitionTriggerSimpleIR t -> triggers.put(name, t);
                case TransitionTriggerWhenIR t -> triggers.put(name, t);
                // TransitionActionIR subtypes
                case TransitionActionCustomIR a -> transitionActions.put(name, a);
                case TransitionActionMachineIR a -> transitionActions.put(name, a);
                case TransitionActionSendToIR a -> transitionActions.put(name, a);
                // not stored in the repository
                default -> {
                }
            }
            return this;
        }

        public IrRepository build() {
            return new IrRepository(this);
        }
    }
}
