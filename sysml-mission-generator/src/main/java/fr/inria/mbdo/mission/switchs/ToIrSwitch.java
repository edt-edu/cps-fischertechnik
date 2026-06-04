package fr.inria.mbdo.mission.switchs;

import fr.inria.mbdo.mission.generators.IrRepository;
import fr.inria.mbdo.mission.generators.SymbolIndex;
import fr.inria.mbdo.mission.ir.*;
import org.eclipse.emf.common.util.EList;
import org.eclipse.syson.sysml.*;
import org.eclipse.syson.sysml.impl.FeatureChainingImpl;
import org.eclipse.syson.sysml.util.SysmlSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ToIrSwitch extends SysmlSwitch<Void> {
    private static final Logger logger = LoggerFactory.getLogger(ToIrSwitch.class);
    private static final Pattern MACHINE_REF_TOKEN = Pattern.compile("\\b([A-Za-z_]\\w*)\\s*\\.");

    private final SymbolIndex index;
    private final IrRepository.Builder irRepositoryBuilder;

    public ToIrSwitch(SymbolIndex index, IrRepository.Builder irRepositoryBuilder) {
        this.index = index;
        this.irRepositoryBuilder = irRepositoryBuilder;
    }

    @Override
    public Void casePartDefinition(PartDefinition object) {
        logger.debug("Traversing part def: {}", object.getQualifiedName());
        if (shouldSkipGeneration(object)) {
            return null;
        }

        List<MachineAttributeIR> attributes = new ArrayList<>();
        for (AttributeUsage ownedAttribute : object.getOwnedAttribute()) {
            TypeRef attributeType = toTypeRef(ownedAttribute.getType());
            attributes.add(new MachineAttributeIR(ownedAttribute, attributeType));
        }

        Map<String, Ref<MachineActionIR>> actions = new HashMap<>();
        for (ActionUsage ownedAction : object.getOwnedAction()) {
            MachineActionIR action = buildMachineActionIR(ownedAction);
            irRepositoryBuilder.add(action);
            logger.info("Traversing action: {} of type {}", ownedAction.getQualifiedName(), action.getQualifiedName());
            actions.put(ownedAction.getQualifiedName(), new Ref<>(action.getQualifiedName()));
        }

        // TODO define messages sent by this machine ?
        irRepositoryBuilder.add(new MachineIR(object, actions, attributes, List.of()));
        return null;
    }

    @Override
    public Void caseItemDefinition(ItemDefinition object) {
        logger.debug("Traversing item def: {}", object.getQualifiedName());
        if (shouldSkipGeneration(object) || index.resolvePart(object.getQualifiedName()).isPresent()) {
            return null;
        }
        if (object.supertypes(true).stream().anyMatch(t -> t.getName().equals("EventMessage"))) {
            irRepositoryBuilder.add(new MachineMessageIR(object));
        }
        return null;
    }

    @Override
    public Void caseStateDefinition(StateDefinition object) {
        logger.debug("[State]\tTraversing state def: {}", object.getQualifiedName());
        if (shouldSkipGeneration(object)) {
            return null;
        }

        TransitionIR defaultTransitionIR = null;
        List<MachineRefIR> machinesRefs = collectMachineRefs(object);
        List<Ref<StateIR>> statesRefs = collectStatesRefs(object.getOwnedState());
        Map<String, List<Ref<TransitionIR>>> transitionsRefs = new HashMap<>();

        for (TransitionUsage transition : object.getOwnedTransition()) {
            TransitionIR transitionIR = buildTransitionIR(transition, statesRefs, machinesRefs);
            irRepositoryBuilder.add(transitionIR);
            if (defaultTransitionIR == null && transitionIR.getFromState() == null) {
                defaultTransitionIR = transitionIR;
                continue;
            }
            String sourceQName = transitionIR.getFromState().qName();
            Ref<TransitionIR> transitionRef = new Ref<>(transitionIR.getQualifiedName());

            transitionsRefs
                    .computeIfAbsent(sourceQName, k -> new ArrayList<>())
                    .add(transitionRef);
        }

        for (StateUsage state : object.getOwnedState()) {
            StateIR stateIR = new StateIR(state, transitionsRefs.get(state.getQualifiedName()));
            irRepositoryBuilder.add(stateIR);
        }

        if (defaultTransitionIR == null) {
            throw new IllegalStateException("No default transition found for state " + object.getQualifiedName());
        }

        irRepositoryBuilder.add(new MachineMissionIR(object, new Ref<>(defaultTransitionIR.getQualifiedName()), statesRefs, machinesRefs));

        return null;
    }

    @Override
    public Void caseEnumerationDefinition(EnumerationDefinition object) {
        logger.debug("Traversing enumeration def: {}", object.getQualifiedName());
        if (shouldSkipGeneration(object)) {
            return null;
        }

        List<String> constants = object.getEnumeratedValue().stream().map(Element::getName).toList();
        irRepositoryBuilder.add(new EnumerationIR(object, constants));
        return null;
    }

    @Override
    public Void caseActionDefinition(ActionDefinition object) {
        logger.debug("Traversing action def: {}", object.getQualifiedName());
        if (shouldSkipGeneration(object)) {
            return null;
        }
        return null;
    }

    @Override
    public Void caseElement(Element object) {
        return doSwitchForAllOwnedElements(object);
    }

    private Void doSwitchForAllOwnedElements(Element object) {
        object.getOwnedElement().forEach(this::doSwitch);
        return null;
    }

    private List<MachineRefIR> collectMachineRefs(StateDefinition object) {
        List<MachineRefIR> machinesRefs = new ArrayList<>();
        for (ReferenceUsage ref : object.getOwnedReference()) {
            TypeRef machineType = toTypeRef(ref.getType());
            logger.info("[State] Machine ref {} : id={}", ref.getName(), ref.getElementId());
            if (machineType.kind() != TypeRef.TypeKind.USER) {
                throw new IllegalStateException("Machine reference must target a user type: " + ref.getQualifiedName());
            }
            machinesRefs.add(new MachineRefIR(ref.getName(), machineType));
        }
        return machinesRefs;
    }

    private List<Ref<StateIR>> collectStatesRefs(List<StateUsage> stateUsages) {
        List<Ref<StateIR>> stateRefs = new ArrayList<>();
        for (StateUsage state : stateUsages) {
            stateRefs.add(new Ref<>(state.getQualifiedName()));
        }
        return stateRefs;
    }

    private TransitionIR buildTransitionIR(TransitionUsage transition, List<Ref<StateIR>> statesRefs,
                                           List<MachineRefIR> machineRefs) {
        String sourceQName = transition.getSource() != null ? transition.getSource().getQualifiedName() : null;
        String targetQName = transition.getTarget().getQualifiedName();
        TransitionActionIR transitionAction = buildTransitionAction(transition, machineRefs);
        Ref<TransitionActionIR> transitionActionRef = transitionAction != null
                ? new Ref<>(transitionAction.getQualifiedName())
                : null;
        if (transitionAction != null) {
            irRepositoryBuilder.add(transitionAction);
        }
        logger.debug("[State]\t\tFound transition: {} -> {} : {}", sourceQName, targetQName, transitionActionRef);

        if (sourceQName == null) {
            logger.warn("[State]\t\t\tFound null source for transition, what should I do ?");
        } else if (transition.getSource() instanceof PerformActionUsage) {
            logger.warn("[State]\t\t\tFound action source for transition");
            sourceQName = null; // Set to null to avoid referencing action as source state
        } else {
            String finalSourceQName = sourceQName;
            if (statesRefs.stream().noneMatch(r -> r.qName().equals(finalSourceQName))) {
                logger.warn("Transition source state not found in index ({})", sourceQName);
                throw new IllegalStateException("Transition source state not found: " + sourceQName);
            }
        }

        if (statesRefs.stream().noneMatch(r -> r.qName().equals(targetQName))) {
            logger.warn("Transition target state not found in index ({})", targetQName);
            throw new IllegalStateException("Transition target state not found: " + targetQName);
        }

        TransitionTriggerIR trigger = null;
        List<AcceptActionUsage> accepts = transition.getTriggerAction();

        if (accepts != null && !accepts.isEmpty()) {
            AcceptActionUsage aau = accepts.getFirst();
            if (aau.isTriggerAction()) {
                trigger = buildTriggerIR(transition, aau, sourceQName, targetQName, machineRefs);
                irRepositoryBuilder.add(trigger);
            } else {
                logger.debug("[State]\t\t\tNo trigger action found for transition");
            }
        }

        TransitionGuardIR guard = null;

        for (Expression e : transition.getGuardExpression()) {
            for (TextualRepresentation tr : e.getTextualRepresentation()) {
                logger.info("[State]\t\t\tFound guard expression: {}", tr.getBody());
            }
        }

        // TODO build Guard ?
        Ref<StateIR> sourceStateRef = sourceQName != null ? new Ref<>(sourceQName) : null;
        Ref<StateIR> targetStateRef = new Ref<>(targetQName);
        Ref<TransitionTriggerIR> triggerRef = trigger != null ? new Ref<>(trigger.getQualifiedName()) : null;
        Ref<TransitionGuardIR> guardRef = guard != null ? new Ref<>(guard.getQualifiedName()) : null;

        logger.debug("[State]\t\t\tBuilt transition IR: {} -> {} [trigger={}, guard={}, action={}]",
                sourceQName, targetQName, triggerRef, guardRef, transitionActionRef);

        return new TransitionIR(transition, sourceStateRef, targetStateRef, triggerRef, guardRef, transitionActionRef);
    }

    private MachineActionIR buildMachineActionIR(ActionUsage actionUsage) {

        ActionUsage performedAction;

        if (actionUsage instanceof PerformActionUsage pau && (performedAction = pau.getPerformedAction()) != null) {

            Feature feat = performedAction.referencedFeatureTarget();
            if (feat instanceof ActionUsage) {
                performedAction = (ActionUsage) feat;
                logger.warn("Feature {} is of type ActionUsage: {}", feat.getQualifiedName(),
                        feat.getOwner().getQualifiedName());
                for (Behavior beh : ((ActionUsage) feat).getActionDefinition()) {
                    logger.warn("Behavior def {} owned by {}", beh.getQualifiedName(), beh.getOwner());
                }
            }
            logger.info("PerformedAction: {} // {} -> {}", performedAction.getQualifiedName(),
                    feat == null ? "feat is null" : feat.getQualifiedName(), actionUsage.getTextualRepresentation()
                            .stream().map(TextualRepresentation::getBody).collect(Collectors.joining(", ")));
        }

        List<ParameterIR> parameterIRs = new ArrayList<>();
        for (Feature parameter : actionUsage.getParameter()) {
            EList<Type> types = parameter.getType();
            TypeRef paramType = (types == null || types.isEmpty()) ?
                    TypeRef.unknown(actionUsage.getQualifiedName() + "::" + parameter.getName()) : toTypeRef(types);
            parameterIRs.add(new ParameterIR(parameter, paramType));
        }

        return new MachineActionIR(actionUsage, parameterIRs);
    }

    private TypeRef toParameterTypeRef(Feature parameter, String ownerQualifiedName) {
        EList<Type> types = parameter.getType();
        if (types == null || types.isEmpty()) {
            return TypeRef.unknown(ownerQualifiedName + "::" + parameter.getName());
        }
        return toTypeRef(types);
    }

    private TransitionTriggerIR buildTriggerIR(TransitionUsage transition, AcceptActionUsage accept, String sourceQName,
                                               String targetQName, List<MachineRefIR> machineRefs) {
        String explicitMessageTypeQName = resolveExplicitAcceptMessageTypeQName(accept);
        if (explicitMessageTypeQName != null) {
            return new TransitionTriggerSimpleIR(accept, new Ref<>(explicitMessageTypeQName));
        }

        String sourceToken = simpleToken(sourceQName, "Init");
        String targetToken = simpleToken(targetQName, "Target");
        String acceptEventName = "AcceptWhen" + sourceToken + "To" + targetToken + "Event";
        String acceptEventQualifiedName = transition.getQualifiedName() + "::" + acceptEventName;
        String machineRefName = "TODO";
        String machineTypeQualifiedName = machineRefs.stream()
                .filter(machineRef -> machineRef.name().equals(machineRefName))
                .map(machineRef -> machineRef.type().qualifiedName())
                .findFirst()
                .orElse(null);

        String conditionMethodName = "isAcceptWhenEventConditionMet";
        String triggerMethodName = "checkAndTriggerAcceptWhenEvent";
        String sysmlSource = "accept when // TODO boolean expression";

        return new TransitionTriggerWhenIR(
                accept,
                acceptEventName,
                acceptEventQualifiedName,
                machineRefName,
                machineTypeQualifiedName,
                conditionMethodName,
                triggerMethodName,
                sysmlSource);
    }

    private String resolveExplicitAcceptMessageTypeQName(AcceptActionUsage accept) {
        if (accept == null) {
            return null;
        }

        if (accept.getNestedReference() != null && !accept.getNestedReference().isEmpty()) {
            ReferenceUsage nestedReference = accept.getNestedReference().getFirst();
            if (nestedReference != null && nestedReference.getType() != null && !nestedReference.getType().isEmpty()) {
                String qualifiedName = nestedReference.getType().getFirst().getQualifiedName();
                if (qualifiedName != null && !qualifiedName.isBlank()) {
                    return qualifiedName;
                }
            }
        }

        ReferenceUsage payloadParameter = accept.getPayloadParameter();
        if (payloadParameter != null && payloadParameter.getType() != null && !payloadParameter.getType().isEmpty()) {
            String qualifiedName = payloadParameter.getType().getFirst().getQualifiedName();
            if (qualifiedName != null && !qualifiedName.isBlank() && !qualifiedName.equals("ScalarValues::Boolean")) {
                return qualifiedName;
            }
        }

        if (accept.getPayloadArgument() instanceof FeatureReferenceExpression featureReferenceExpression
                && featureReferenceExpression.getReferent() != null
                && featureReferenceExpression.getReferent().getQualifiedName() != null) {
            return featureReferenceExpression.getReferent().getQualifiedName();
        }

        return null;
    }

    private String simpleToken(String qualifiedName, String fallback) {
        if (qualifiedName == null || qualifiedName.isBlank()) {
            return fallback;
        }
        String token = qualifiedName;
        int separator = qualifiedName.lastIndexOf("::");
        if (separator >= 0 && separator + 2 < qualifiedName.length()) {
            token = qualifiedName.substring(separator + 2);
        }
        return toUpperCamel(token, fallback);
    }

    private String toUpperCamel(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        String[] parts = value.split("[^A-Za-z0-9]+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            builder.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }

        return builder.isEmpty() ? fallback : builder.toString();
    }

    private TypeRef toTypeRef(EList<Type> types) {
        if (types == null || types.isEmpty()) {
            throw new IllegalArgumentException("Missing type");
        }

        String qualifiedName = types.getFirst().getQualifiedName();
        if (qualifiedName == null) {
            throw new IllegalArgumentException("Type without qualified name");
        }

        return switch (qualifiedName) {
            case "ScalarValues::Boolean" -> TypeRef.scalar(ScalarType.BOOLEAN);
            case "ScalarValues::Integer" -> TypeRef.scalar(ScalarType.INTEGER);
            case "ScalarValues::Real" -> TypeRef.scalar(ScalarType.REAL);
            default -> index.resolveAny(qualifiedName)
                    .map(def -> TypeRef.user(qualifiedName))
                    .orElseThrow(() -> new IllegalStateException(
                            "Cannot resolve type: " + qualifiedName));
        };
    }

    private boolean shouldSkipGeneration(Definition object) {
        for (MetadataUsage metadataUsage : object.getOwnedMetadata()) {
            if (metadataUsage.getType().stream().map(Type::getQualifiedName).anyMatch("Common::NoGeneration"::equals)) {
                logger.debug("Skipping generation: {}", metadataUsage.getType());
                return true;
            }
        }

        return false;
    }

    private TransitionActionIR buildTransitionAction(TransitionUsage transition, List<MachineRefIR> machineRefs) {
        TransitionActionIR action = null;
        ActionUsage actionUsage = extractTransitionAction(transition);

        if (actionUsage instanceof SendActionUsage sau) {
            String receiverName = sau.getSenderArgument().getResult().getName();
            TypeRef messageTypeRef = toTypeRef(sau.getPayloadArgument().getType());
            MachineRefIR senderMachine = machineRefs.stream()
                    .filter(ref -> ref.name().equals(receiverName)).findFirst().orElseThrow(() -> new IllegalStateException("Receiver was not specified for message " + messageTypeRef.qualifiedName()));

            logger.warn("SendActionUsage called message={} to={}", messageTypeRef.qualifiedName(), senderMachine);
            MachineMessageRefIR messageRef = new MachineMessageRefIR(sau.getPayloadArgument().getType().getFirst().getQualifiedName(), messageTypeRef);

            action = new TransitionActionSendToIR(sau, List.of(), messageRef, senderMachine);
            // Event qName: sau.getPayloadArgument().getType().getFirst().getQualifiedName
            // Sender: sau.getSenderArgument().getResult()
            // Receiver:
        } else if (actionUsage instanceof PerformActionUsage pea) {
            logger.debug("Building transition action: {} with class {}", actionUsage, actionUsage.getClass());

            Feature feat = pea.referencedFeatureTarget();
            if (feat == null) {
                feat = pea;
            }

            List<String> featuredChainingReferences = extractChainingReferencesFromAction(actionUsage);
            List<MachineRefIR> featuredMachinesRefs = machineRefs.stream()
                    .filter(ref -> featuredChainingReferences.contains(ref.name())).toList();
            if (featuredMachinesRefs.size() > 1) {
                throw new IllegalStateException("More than 1 machine ref for transition");
            }

            MachineRefIR machineRef = !featuredMachinesRefs.isEmpty() ? featuredMachinesRefs.getFirst() : null;
            if (featuredMachinesRefs.isEmpty()) {
                action = new TransitionActionCustomIR(feat, List.of(), List.of());
            } else {
                action = new TransitionActionMachineIR(feat, List.of(), machineRef);
            }
        } else if (actionUsage != null) {
            logger.info("ActionUsage {} is of type {}", actionUsage, actionUsage.getClass());
        }

        return action;
    }

    private ActionUsage extractTransitionAction(TransitionUsage transition) {
        ActionUsage actionUsage = null;
        if (transition.getSource() instanceof PerformActionUsage pau) {
            actionUsage = pau.getPerformedAction();
        } else if (transition.getEffectAction() != null && !transition.getEffectAction().isEmpty()) {
            actionUsage = transition.getEffectAction().getFirst();
        }

        return actionUsage;
    }

    private List<String> extractChainingReferencesFromAction(ActionUsage actionUsage) {
        List<String> result = new ArrayList<>();
        if (actionUsage != null) {
            for (Relationship rel : actionUsage.getOwnedRelationship()) {
                for (Element element : rel.getOwnedRelatedElement()) {
                    for (Relationship rel2 : element.getOwnedRelationship()) {
                        if (rel2 instanceof FeatureChainingImpl featChaining) {
                            result.add(featChaining.getChainingFeature().getName());
                        }
                    }
                }
            }
        }

        return result;
    }
}
