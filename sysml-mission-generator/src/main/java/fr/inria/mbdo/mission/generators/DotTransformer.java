package fr.inria.mbdo.mission.generators;

import fr.inria.mbdo.mission.ir.*;

import java.util.*;

/**
 * Produces Graphviz DOT diagrams from mission IR — one digraph per {@link MachineMissionIR}.
 *
 * <p>Each digraph contains:
 * <ul>
 *   <li>A pseudo-initial node ({@code __init__}) for the entry transition when present.</li>
 *   <li>One node per state.</li>
 *   <li>One labelled edge per transition, with the format {@code trigger [guard] / action}.</li>
 * </ul>
 */
public class DotTransformer {

    private static final String INIT_NODE = "__init__";

    private final IrRepository irRepository;

    public DotTransformer(IrRepository irRepository) {
        this.irRepository = irRepository;
    }

    /** Returns a map from mission simple name to its DOT diagram string. */
    public Map<String, String> generate() {
        Map<String, String> result = new LinkedHashMap<>();
        for (MachineMissionIR mission : irRepository.getMissions().values()) {
            result.put(mission.getName(), generateForMission(mission));
        }
        return result;
    }

    public String generateForMission(MachineMissionIR mission) {
        List<StateIR> states = resolveMissionStates(mission);
        List<TransitionIR> transitions = resolveMissionTransitions(mission, states);

        StringBuilder sb = new StringBuilder();
        sb.append("digraph ").append(mission.getName()).append(" {\n");
        sb.append("    fontname=\"Helvetica,Arial,sans-serif\"\n");
        sb.append("    node [fontname=\"Helvetica,Arial,sans-serif\"]\n");
        sb.append("    edge [fontname=\"Helvetica,Arial,sans-serif\"]\n");
        sb.append("    rankdir=LR;\n");

        boolean hasInitial = transitions.stream().anyMatch(t -> t.getFromState() == null);
        if (hasInitial) {
            sb.append("    node [shape=point, label=\"\"]; ").append(INIT_NODE).append(";\n");
        }
        sb.append("    node [shape=circle];\n");
        for (StateIR state : states) {
            sb.append("    \"").append(state.getName()).append("\";\n");
        }

        sb.append('\n');
        for (TransitionIR transition : transitions) {
            String fromNode = transition.getFromState() == null
                    ? INIT_NODE
                    : "\"" + resolveStateName(transition.getFromState()) + "\"";
            String toName = resolveStateName(transition.getToState());
            if (toName == null) continue;
            String toNode = "\"" + toName + "\"";

            String label = buildTransitionLabel(transition);
            if (label.isEmpty()) {
                sb.append("    ").append(fromNode).append(" -> ").append(toNode).append(";\n");
            } else {
                sb.append("    ").append(fromNode).append(" -> ").append(toNode)
                        .append(" [label=\"").append(escapeDotLabel(label)).append("\"];\n");
            }
        }

        sb.append('}');
        return sb.toString();
    }

    private String buildTransitionLabel(TransitionIR transition) {
        StringBuilder label = new StringBuilder();

        label.append(resolveTriggerLabel(transition));

        if (transition.getGuard() != null) {
            String guardName = simpleNameFromQName(transition.getGuard().qName());
            if (guardName != null && !guardName.isBlank()) {
                label.append(" [").append(guardName).append(']');
            }
        }

        if (transition.getAction() != null) {
            String actionStr = resolveActionLabel(transition.getAction());
            if (actionStr != null && !actionStr.isBlank()) {
                label.append(" / ").append(actionStr);
            }
        }

        return label.toString().trim();
    }

    private String resolveTriggerLabel(TransitionIR transition) {
        if (transition.getTrigger() == null) {
            return "ε";
        }
        TransitionTriggerIR trigger = irRepository.getTriggers().get(transition.getTrigger().qName());
        if (trigger == null) {
            return "ε";
        }
        return switch (trigger) {
            case TransitionTriggerSimpleIR simple -> {
                Ref<MachineMessageIR> msgRef = simple.getEventMessage();
                if (msgRef == null) yield "ε";
                MachineMessageIR msg = irRepository.getMessages().get(msgRef.qName());
                yield msg != null ? msg.getName() : simpleNameFromQName(msgRef.qName());
            }
            case TransitionTriggerWhenIR when -> "when(" + when + ")";
        };
    }

    private String resolveActionLabel(Ref<TransitionActionIR> actionRef) {
        if (actionRef == null) return null;
        TransitionActionIR action = irRepository.getTransitionActions().get(actionRef.qName());
        if (action == null) return null;
        return switch (action) {
            case TransitionActionCustomIR custom -> custom.getName();
            case TransitionActionMachineIR machine -> {
                String machineName = machine.getMachineRef() != null ? machine.getMachineRef().name() : "";
                yield (machineName.isBlank() ? "" : machineName + ".") + machine.getName() + "()";
            }
            case TransitionActionSendToIR sendTo -> {
                String msgName = sendTo.getMessage() != null ? sendTo.getMessage().name() : "?";
                String toName = sendTo.getTo() != null ? sendTo.getTo().name() : "?";
                yield "send " + msgName + " -> " + toName;
            }
        };
    }

    private String resolveStateName(Ref<StateIR> stateRef) {
        if (stateRef == null) return null;
        StateIR state = irRepository.getStates().get(stateRef.qName());
        return state != null ? state.getName() : simpleNameFromQName(stateRef.qName());
    }

    private String simpleNameFromQName(String qName) {
        if (qName == null) return null;
        int sep = qName.lastIndexOf("::");
        return sep >= 0 ? qName.substring(sep + 2) : qName;
    }

    private String escapeDotLabel(String label) {
        return label.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private List<StateIR> resolveMissionStates(MachineMissionIR mission) {
        List<StateIR> states = new ArrayList<>();
        for (Ref<StateIR> stateRef : mission.getStates()) {
            StateIR state = irRepository.getStates().get(stateRef.qName());
            if (state != null) states.add(state);
        }
        return states;
    }

    private List<TransitionIR> resolveMissionTransitions(MachineMissionIR mission, List<StateIR> states) {
        Map<String, TransitionIR> transitions = new LinkedHashMap<>();
        if (mission.getDefaultTransition() != null) {
            TransitionIR defaultTransition = irRepository.getTransitions().get(mission.getDefaultTransition().qName());
            if (defaultTransition != null) {
                transitions.put(defaultTransition.getQualifiedName(), defaultTransition);
            }
        }
        for (StateIR state : states) {
            for (Ref<TransitionIR> transitionRef : state.getTransitions()) {
                TransitionIR transition = irRepository.getTransitions().get(transitionRef.qName());
                if (transition != null) {
                    transitions.putIfAbsent(transition.getQualifiedName(), transition);
                }
            }
        }
        return new ArrayList<>(transitions.values());
    }
}
