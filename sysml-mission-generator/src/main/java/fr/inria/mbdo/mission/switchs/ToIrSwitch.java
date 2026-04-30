package fr.inria.mbdo.mission.switchs;

import fr.inria.mbdo.mission.generators.IrRepository;
import fr.inria.mbdo.mission.generators.SymbolIndex;
import fr.inria.mbdo.mission.ir.*;
import org.eclipse.emf.common.util.EList;
import org.eclipse.syson.sysml.*;
import org.eclipse.syson.sysml.util.SysmlSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ToIrSwitch extends SysmlSwitch<Void> {
    private static final Logger logger = LoggerFactory.getLogger(ToIrSwitch.class);

    private final SymbolIndex index;
    private final IrRepository.Builder irRepositoryBuilder;

    public ToIrSwitch(SymbolIndex index, IrRepository.Builder irRepositoryBuilder) {
        this.index = index;
        this.irRepositoryBuilder = irRepositoryBuilder;
    }

    @Override
    public Void casePartDefinition(PartDefinition object) {
        logger.debug("Traversing part def: {}", object.getQualifiedName());

        List<MachineAttributeIR> attributes = new ArrayList<>();
        for (AttributeUsage ownedAttribute : object.getOwnedAttribute()) {
            TypeRef attributeType = toTypeRef(ownedAttribute.getType(), ownedAttribute.getQualifiedName());
            attributes.add(new MachineAttributeIR(ownedAttribute, attributeType));
        }

        List<ActionIR> actions = new ArrayList<>();
        for (ActionUsage ownedAction : object.getOwnedAction()) {
            List<ParameterIR> parameters = new ArrayList<>();
            for (Feature param : ownedAction.getParameter()) {
                TypeRef paramType = toTypeRef(param.getType(), ownedAction.getQualifiedName());
                parameters.add(new ParameterIR(param, paramType));

            }
            actions.add(new ActionIR(ownedAction, parameters));
        }

        if (object.getOwnedPart() != null) {
            logger.debug("Traversing part def: {}", object.getQualifiedName());
        }

        List<MachineMessageIR> messages = new ArrayList<>();

        irRepositoryBuilder.add(new MachineIR(object, actions, attributes, messages));

        return null;
    }

    @Override
    public Void caseItemDefinition(ItemDefinition object) {
        logger.debug("Traversing item def: {}", object.getQualifiedName());
        // irRepositoryBuilder.add(new MachineMessageIR(object, null));
        return null;
    }

    @Override
    public Void caseStateDefinition(StateDefinition object) {
        logger.debug("[State]\tTraversing state def: {}", object.getQualifiedName());

        List<MachineRefIR> machinesRefs = new ArrayList<>();
        for (ReferenceUsage ref : object.getOwnedReference()) {
            TypeRef machineType = toTypeRef(ref.getType(), ref.getQualifiedName());
            if (machineType.kind() != TypeRef.TypeKind.USER) {
                throw new IllegalStateException("Machine reference must target a user type: " + ref.getQualifiedName());
            }
            String refName = ref.getName();
            if (refName == null || refName.isBlank()) {
                refName = "machine" + machinesRefs.size();
            }
            machinesRefs.add(new MachineRefIR(refName, machineType));
        }

        Map<String, List<Ref<TransitionIR>>> stateTransitions = new HashMap<>();
        List<Ref<StateIR>> stateRefs = new ArrayList<>();
        Ref<StateIR> defaultState = null;

        for (StateUsage state : object.getOwnedState()) {
            String stateQName = elementQualifiedName(object, state);
            stateTransitions.put(stateQName, new ArrayList<>());
            stateRefs.add(new Ref<>(stateQName));
        }

        for (TransitionUsage transition : object.getOwnedTransition()) {
            logger.debug("[State]\t\tTraversing transition usage: {} -> {}", transition.getSource().getName(),
                    transition.getTarget().getName());

            if (transition.getSource() == null || transition.getTarget() == null) {
                throw new IllegalStateException(
                        "Transition missing source or target: " + transition.getQualifiedName());
            }

            String sourceQName = elementQualifiedName(object, transition.getSource());
            String targetQName = elementQualifiedName(object, transition.getTarget());
            sourceQName = transition.getSource().getQualifiedName();
            targetQName = transition.getTarget().getQualifiedName();

            logger.debug("Source : {}, Target: {}", sourceQName, targetQName);

//            if (sourceQName == null) {
//                defaultState = new Ref<>(transition.getTarget().getName());
//            }
//            else if (!stateTransitions.containsKey(sourceQName)) {
//                throw new IllegalStateException("Transition source state not found: " + sourceQName);
//            }
//            if (!stateTransitions.containsKey(targetQName)) {
//                throw new IllegalStateException("Transition target state not found: " + targetQName);
//            }

            TriggerIR trigger = new TriggerIR(transition, "implicit");
            irRepositoryBuilder.add(trigger);
            TransitionIR transitionIR = new TransitionIR(transition, new Ref<>(targetQName),
                    new Ref<>(trigger.getQualifiedName()), null);
            irRepositoryBuilder.add(transitionIR);
            if (sourceQName != null) {
                stateTransitions.get(sourceQName).add(new Ref<>(transitionIR.getQualifiedName()));
            }
            // for (AcceptActionUsage accept : transition.getTriggerAction()) {
            // logger.debug("[State]\t\t\tAccept action usage: {}", accept.getName());
            //
            // String expr = accept.getPayloadArgument() != null ?
            // accept.getPayloadArgument().toString()
            // : accept.getName();
            // TriggerIR trigger = new TriggerIR(accept, expr);
            // irRepositoryBuilder.add(trigger);
            //
            // TransitionIR transitionIR = new TransitionIR(transition, new
            // Ref<>(targetQName),
            // new Ref<>(trigger.getQualifiedName()), null);
            // irRepositoryBuilder.add(transitionIR);
            // stateTransitions.get(sourceQName).add(new
            // Ref<>(transitionIR.getQualifiedName()));
            // }
        }

        for (StateUsage state : object.getOwnedState()) {
            String stateQName = elementQualifiedName(object, state);
            List<Ref<TransitionIR>> transitions = stateTransitions.getOrDefault(stateQName, List.of());
            StateIR stateIR = new StateIR(state, transitions);
            irRepositoryBuilder.add(stateIR);
        }

        MachineMissionIR mission = new MachineMissionIR(object, stateRefs.getFirst(), stateRefs, machinesRefs, List.of());
        irRepositoryBuilder.add(mission);

        return null;
    }

    @Override
    public Void caseEnumerationDefinition(EnumerationDefinition object) {
        logger.debug("Traversing enumeration def: {}", object.getQualifiedName());
        List<String> constants = object.getEnumeratedValue().stream().map(Element::getName).toList();
        irRepositoryBuilder.add(new EnumerationIR(object, constants));

        return null;
    }

    @Override
    public Void caseActionDefinition(ActionDefinition object) {
        logger.debug("Traversing action def: {}", object.getQualifiedName());
        List<ParameterIR> parameters = new ArrayList<>();
        for (Feature param : object.getParameter()) {
            TypeRef paramType = toTypeRef(param.getType(), object.getQualifiedName());
            parameters.add(new ParameterIR(param, paramType));
        }
        irRepositoryBuilder.add(new ActionIR(object, parameters));
        return null;
    }

    @Override
    public Void caseElement(Element object) {
        return doSwitchForAllOwnedElements(object);
    }

    /**
     * Look into owned children elements
     * 
     * @param object
     * @return List of JavaFile generated from the traversal
     */
    private Void doSwitchForAllOwnedElements(Element object) {
        object.getOwnedElement().forEach(this::doSwitch);
        return null;
    }

    private TypeRef toTypeRef(EList<Type> types, String ownerQualifiedName) {
        if (types == null || types.isEmpty()) {
            throw new IllegalStateException("Missing type for " + ownerQualifiedName);
        }

        String qualifiedName = types.getFirst().getQualifiedName();
        if (qualifiedName == null) {
            throw new IllegalStateException("Type without qualified name for " + ownerQualifiedName);
        }

        return switch (qualifiedName) {
            case "ScalarValues::Boolean" -> TypeRef.scalar(ScalarType.BOOLEAN);
            case "ScalarValues::Integer" -> TypeRef.scalar(ScalarType.INTEGER);
            case "ScalarValues::Real" -> TypeRef.scalar(ScalarType.REAL);
            default -> index.resolveAny(qualifiedName)
                    .map(def -> TypeRef.user(qualifiedName))
                    .orElseThrow(() -> new IllegalStateException(
                            "Cannot resolve type: " + qualifiedName + " for " + ownerQualifiedName));
        };
    }

    private String elementQualifiedName(Definition owner, Usage usage) {
        String ownerName = owner.getQualifiedName();
        String stateName = usage.getName();
        if (stateName == null || stateName.isBlank()) {
            return ownerName + "::" + usage.eClass().getName();
        }

        String result = ownerName + "::" + stateName;
        if (!result.equals(usage.getQualifiedName())) {
            throw new IllegalStateException(result + " != " + usage.getQualifiedName());
        }
        return result;
    }

    @Override
    public Void caseMetadataUsage(MetadataUsage object) {
        logger.debug("Traversing metadata usage: {}, parent: {}", object.getType().getFirst(), object.getOwner().getQualifiedName());
        return null;
    }

    /**
     *
     * @param object
     * @return
     */
    private boolean skipGeneration(Definition object) {
        for (MetadataUsage m : object.getOwnedMetadata()) {
            logger.debug("Skipping generation: {}", m.getType());
        }
        return object.getOwnedMetadata().stream().map(Usage::getType).toList().contains("Common::NoGeneration");
    }
}
