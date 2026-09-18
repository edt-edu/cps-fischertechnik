package fr.inria.mbdo.mission.switchs;

import fr.inria.mbdo.mission.generators.SymbolIndex;
import fr.inria.mbdo.mission.ir.*;
import org.eclipse.emf.common.util.EList;
import org.eclipse.syson.sysml.*;
import org.eclipse.syson.sysml.impl.*;
import org.eclipse.syson.sysml.util.SysmlSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class ToIrSwitch extends SysmlSwitch<List<ElementIR>> {
    private static final Logger logger = LoggerFactory.getLogger(ToIrSwitch.class);

    private static final String MAIN_MACHINE_METADATA_QNAME = "MainMachineMetadata";

    private final SymbolIndex index;

    public ToIrSwitch(SymbolIndex index) {
        this.index = index;
    }

    // --- EMF metadata extraction ---

    private static IrMetadata meta(Element element) {
        String namespace = element.getOwningNamespace().getQualifiedName();
        String name = element.getName();
        String documentation = element.getDocumentation().stream()
                .map(Documentation::getBody).collect(Collectors.joining("\n"));
        String sourceUri = element.eResource() != null ? element.eResource().getURI().toString() : null;
        return new IrMetadata(namespace, name, documentation, sourceUri);
    }

    // --- Switch cases ---

    @Override
    public List<ElementIR> casePartDefinition(PartDefinition object) {
        logger.debug("Traversing part def: {}", object.getQualifiedName());
        if (shouldSkipGeneration(object)) {
            return List.of();
        }

        List<ElementIR> result = new ArrayList<>();

        List<MachineAttributeIR> attributes = new ArrayList<>();
        for (AttributeUsage ownedAttribute : object.getOwnedAttribute()) {
            TypeRef attrType = (ownedAttribute.getType() == null || ownedAttribute.getType().isEmpty())
                    ? TypeRef.unknown(object.getQualifiedName() + "::" + ownedAttribute.getName())
                    : toTypeRef(ownedAttribute.getType());
            attributes.add(new MachineAttributeIR(ownedAttribute.getName(), attrType));
        }

        List<Ref<MachineActionIR>> actionRefs = new ArrayList<>();
        for (ActionUsage ownedAction : object.getOwnedAction()) {
            MachineActionIR action = buildMachineActionIR(ownedAction);
            result.add(action);
            logger.info("Traversing action: {} of type {}", ownedAction.getQualifiedName(), action.getQualifiedName());
            actionRefs.add(new Ref<>(action.getQualifiedName()));
        }

        List<Ref<MachineMessageIR>> messagesRefs = new ArrayList<>();
      
        // TODO define messages sent by this machine ?
        result.add(new MachineIR(meta(object), actionRefs, attributes, messagesRefs));
        return result;
    }

    @Override
    public List<ElementIR> caseItemDefinition(ItemDefinition object) {
        logger.debug("Traversing item def: {}", object.getQualifiedName());
        if (!shouldSkipGeneration(object)
                && index.resolvePart(object.getQualifiedName()).isEmpty()
                && object.supertypes(true).stream().anyMatch(t -> t.getName().equals("EventMessage"))) {
            return List.of(new MachineMessageIR(meta(object)));
        }
        return List.of();
    }

    @Override
    public List<ElementIR> caseAttributeDefinition(AttributeDefinition object) {
        logger.debug("Traversing attribute def: {}", object.getQualifiedName());
        if (shouldSkipGeneration(object) || index.resolvePart(object.getQualifiedName()).isPresent()) {
            return List.of();
        }

        List<ParameterIR> parameterIRs = new ArrayList<>();
        for (Feature parameter : object.getFeature()) {
            EList<Type> types = parameter.getType();
            TypeRef paramType = (types == null || types.isEmpty())
                    ? TypeRef.unknown(parameter.getQualifiedName() + "::" + parameter.getName())
                    : toTypeRef(types);
            parameterIRs.add(new ParameterIR(parameter.getName(), paramType));
        }

        return List.of(new CustomTypeIR(meta(object), parameterIRs));
    }

    @Override
    public List<ElementIR> caseStateDefinition(StateDefinition object) {
        logger.debug("[State]\tTraversing state def: {}", object.getQualifiedName());
        if (shouldSkipGeneration(object)) {
            return List.of();
        }

        List<ElementIR> result = new ArrayList<>();
        TransitionIR defaultTransitionIR = null;
        List<MachineRefIR> machinesRefs = collectMachineRefs(object);
        List<Ref<StateIR>> statesRefs = collectStatesRefs(object.getOwnedState());
        Map<String, List<Ref<TransitionIR>>> transitionsRefs = new HashMap<>();

        for (TransitionUsage transition : object.getOwnedTransition()) {
            TransitionBundle bundle = buildTransitionIR(transition, statesRefs, machinesRefs);
            result.addAll(bundle.nodes());
            TransitionIR transitionIR = bundle.transition();

            if (defaultTransitionIR == null && transitionIR.getFromState() == null) {
                defaultTransitionIR = transitionIR;
                continue;
            }
            String sourceQName = transitionIR.getFromState().qName();
            transitionsRefs
                    .computeIfAbsent(sourceQName, k -> new ArrayList<>())
                    .add(new Ref<>(transitionIR.getQualifiedName()));
        }

        for (StateUsage state : object.getOwnedState()) {
            result.add(new StateIR(meta(state), transitionsRefs.get(state.getQualifiedName())));
        }

        if (defaultTransitionIR == null) {
            throw new IllegalStateException("No default transition found for state " + object.getQualifiedName());
        }

        result.add(new MachineMissionIR(meta(object), new Ref<>(defaultTransitionIR.getQualifiedName()),
                statesRefs, machinesRefs));

        return result;
    }

    @Override
    public List<ElementIR> caseEnumerationDefinition(EnumerationDefinition object) {
        logger.debug("Traversing enumeration def: {}", object.getQualifiedName());
        if (shouldSkipGeneration(object)) {
            return List.of();
        }
        List<String> constants = object.getEnumeratedValue().stream().map(Element::getName).toList();
        return List.of(new EnumerationIR(meta(object), constants));
    }

    @Override
    public List<ElementIR> caseActionDefinition(ActionDefinition object) {
        logger.debug("Traversing action def: {}", object.getQualifiedName());
        return List.of();
    }

    @Override
    public List<ElementIR> caseElement(Element object) {
        List<ElementIR> result = new ArrayList<>();
        for (Element owned : object.getOwnedElement()) {
            List<ElementIR> child = doSwitch(owned);
            if (child != null) {
                result.addAll(child);
            }
        }
        return result;
    }

    // --- private helpers ---

    private List<MachineRefIR> collectMachineRefs(StateDefinition object) {
        List<MachineRefIR> machinesRefs = new ArrayList<>();
        Map<String, List<MetadataUsage>> metadata = extractMetadataUsages(object);
        for (ReferenceUsage ref : object.getOwnedReference()) {
            String key = ref.getQualifiedName();
            boolean isMain = metadata.containsKey(key)
                    && metadata.get(key).stream()
                    .filter(m -> (m.getOwnedRelationship() instanceof Annotation annotation)
                            && annotation.getAnnotatedElement().getQualifiedName().equals(key)
                            && m.getType().getFirst().getName().equals(MAIN_MACHINE_METADATA_QNAME))
                    .toList().size() == 1;
            machinesRefs.add(new MachineRefIR(ref.getName(), ref.getQualifiedName(), toTypeRef(ref.getType()), isMain));
        }
        return machinesRefs;
    }

    private Map<String, List<MetadataUsage>> extractMetadataUsages(StateDefinition object) {
        Map<String, List<MetadataUsage>> result = new HashMap<>();

        for (Membership member : object.getOwnedMembership()) {
            if (member.getOwnedRelatedElement() instanceof MetadataUsage mu) {
                String key = member.getQualifiedName();
                ArrayList<MetadataUsage> list = !result.containsKey(key) ? new ArrayList<>()
                        : new ArrayList<>(result.get(key));
                list.add(mu);
                result.put(key, list);
            }
        }

        return result;
    }

    private List<Ref<StateIR>> collectStatesRefs(List<StateUsage> stateUsages) {
        List<Ref<StateIR>> stateRefs = new ArrayList<>();
        for (StateUsage state : stateUsages) {
            stateRefs.add(new Ref<>(state.getQualifiedName()));
        }
        return stateRefs;
    }

    private record TransitionBundle(List<ElementIR> nodes, TransitionIR transition) {
    }

    private TransitionBundle buildTransitionIR(TransitionUsage transition, List<Ref<StateIR>> statesRefs,
                                               List<MachineRefIR> machineRefs) {
        List<ElementIR> nodes = new ArrayList<>();

        /* Retrieve states From -> To */
        String sourceQName = transition.getSource() != null && !(transition.getSource() instanceof PerformActionUsage)
                ? transition.getSource().getQualifiedName()
                : null;
        String targetQName = transition.getTarget().getQualifiedName();

        if (sourceQName != null && statesRefs.stream().noneMatch(r -> r.qName().equals(sourceQName))) {
            logger.warn("Transition source state not found in index ({})", sourceQName);
            throw new IllegalStateException("Transition source state not found: " + sourceQName);
        }

        if (statesRefs.stream().noneMatch(r -> r.qName().equals(targetQName))) {
            logger.warn("Transition target state not found in index ({})", targetQName);
            throw new IllegalStateException("Transition target state not found: " + targetQName);
        }

        /* Build trigger */
        TransitionTriggerIR trigger = buildTriggerIR(transition);
        if (trigger != null)
            nodes.add(trigger);

        /* Build guard (Not implemented yet, TODO) */
        TransitionGuardIR guard = null;

        /* Build action */
        TransitionActionIR transitionAction = buildTransitionAction(transition, machineRefs);
        if (transitionAction != null)
            nodes.add(transitionAction);

        Ref<StateIR> sourceStateRef = sourceQName != null ? new Ref<>(sourceQName) : null;
        Ref<StateIR> targetStateRef = new Ref<>(targetQName);
        Ref<TransitionActionIR> transitionActionRef = transitionAction != null
                ? new Ref<>(transitionAction.getQualifiedName())
                : null;
        Ref<TransitionTriggerIR> triggerRef = trigger != null ? new Ref<>(trigger.getQualifiedName()) : null;
        Ref<TransitionGuardIR> guardRef = guard != null ? new Ref<>(guard.getQualifiedName()) : null;

        logger.debug("[State]\t\t\tNew transition {} -> {} [trigger={}, guard={}, action={}]",
                sourceQName, targetQName, triggerRef, guardRef, transitionActionRef);

        TransitionIR transitionIR = new TransitionIR(meta(transition), sourceStateRef, targetStateRef, triggerRef,
                guardRef, transitionActionRef);
        nodes.add(transitionIR);

        return new TransitionBundle(nodes, transitionIR);
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
            TypeRef paramType = (types == null || types.isEmpty())
                    ? TypeRef.unknown(actionUsage.getQualifiedName() + "::" + parameter.getName())
                    : toTypeRef(types);
            parameterIRs.add(new ParameterIR(parameter.getName(), paramType));
        }

        return new MachineActionIR(meta(actionUsage), parameterIRs);
    }

    private TransitionTriggerIR buildTriggerIR(TransitionUsage transition) {
        List<AcceptActionUsage> accepts = transition.getTriggerAction();
        if (accepts == null || accepts.isEmpty()) {
            return null;
        }
        AcceptActionUsage accept = accepts.getFirst();

        String explicitMessageTypeQName = resolveExplicitAcceptMessageTypeQName(accept);
        if (explicitMessageTypeQName != null) {
            return new TransitionTriggerSimpleIR(meta(transition), new Ref<>(explicitMessageTypeQName));
        }

        Expression payloadArg = accept.getPayloadArgument();
        TriggerExpressionIR triggerExpression = extractTriggerExpression(payloadArg);
        if (triggerExpression == null) {
            String argName = (payloadArg != null && !payloadArg.getParameter().isEmpty())
                    ? payloadArg.getParameter().getFirst().getName()
                    : "<no payload argument>";
            logger.warn("No trigger expression found for {}, skipping accept-when trigger", argName);
            return null;
        }

        return new TransitionTriggerWhenIR(meta(transition), triggerExpression);
    }

    private TriggerExpressionIR extractTriggerExpression(Element element) {
        switch (element) {
            case null -> {
                return null;
            }
            case FeatureChainExpression fce -> {
                return new TriggerMachineAttributeExpressionIR(extractMachineRef(fce),
                        extractMachineAttributeRef(fce));
            }
            case OperatorExpression oe -> {
                List<Feature> parameters = oe.getParameter();
                if (parameters != null && parameters.size() < 2) {
                    throw new IllegalArgumentException("Illegal operator expression, must contain 2 elements");
                }
                TriggerExpressionIR leftExpression = extractTriggerExpression(parameters.get(0));
                TriggerExpressionIR rightExpression = extractTriggerExpression(parameters.get(1));
                return new TriggerOperatorExpressionIR(oe.getOperator(), leftExpression, rightExpression);
            }
            case LiteralExpression le -> {
                return switch (le) {
                    case LiteralBoolean lb -> new TriggerLiteralExpressionIR(TypeRef.scalar(ScalarType.BOOLEAN),
                            String.valueOf(lb.isValue()));
                    case LiteralInteger li -> new TriggerLiteralExpressionIR(TypeRef.scalar(ScalarType.INTEGER),
                            String.valueOf(li.getValue()));
                    case LiteralString ls -> new TriggerLiteralExpressionIR(TypeRef.scalar(ScalarType.STRING),
                            ls.getValue());
                    case LiteralRational lr -> new TriggerLiteralExpressionIR(TypeRef.scalar(ScalarType.REAL),
                            String.valueOf(lr.getValue()));
                    default -> null;
                };
            }
            default -> {
                for (Element member : element.getOwnedElement()) {
                    TriggerExpressionIR tmp = extractTriggerExpression(member);
                    if (tmp != null) {
                        return tmp;
                    }
                }
            }
        }

        return null;
    }

    private MachineRefIR extractMachineRef(Element element) {
        switch (element) {
            case FeatureChainExpression fce -> {
                for (Feature feat : fce.getParameter()) {
                    MachineRefIR tmp = extractMachineRef(feat);
                    if (tmp != null) {
                        return tmp;
                    }
                }
            }
            case FeatureReferenceExpression ffr -> {
                Feature referent = ffr.getReferent();
                return new MachineRefIR(referent.getName(), referent.getQualifiedName(), toTypeRef(referent.getType()));
            }
            default -> {
                for (Element member : element.getOwnedElement()) {
                    MachineRefIR tmp = extractMachineRef(member);
                    if (tmp != null) {
                        return tmp;
                    }
                }
            }
        }
        return null;
    }

    private MachineAttributeRefIR extractMachineAttributeRef(Element element) {
        switch (element) {
            case FeatureChainExpression fce -> {
                return extractMachineAttributeRef(fce.getTargetFeature());
            }
            case AttributeUsage au -> {
                return new MachineAttributeRefIR(au.getName(), au.getQualifiedName(), toTypeRef(au.getType()));
            }
            default -> {
                return null;
            }
        }
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
            EList<Type> payloadTypes = sau.getPayloadArgument().getType();
            if (payloadTypes == null || payloadTypes.isEmpty()) {
                // Constructor expressions (e.g. `send new Foo() to x`) don't have their type
                // list
                // resolved by SySon — skip rather than crash; the send action won't be in the
                // IR.
                logger.warn("SendActionUsage has unresolvable payload type (constructor expression?), skipping: {}",
                        sau.eClass().getName());
            } else {
                String receiverName = sau.getSenderArgument().getResult().getName();
                TypeRef messageTypeRef = toTypeRef(payloadTypes);
                MachineRefIR senderMachine = machineRefs.stream()
                        .filter(ref -> ref.name().equals(receiverName)).findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                "Receiver was not specified for message " + messageTypeRef.qualifiedName()));

                logger.warn("SendActionUsage called message={} to={}", messageTypeRef.qualifiedName(), senderMachine);
                MachineMessageRefIR messageRef = new MachineMessageRefIR(
                        payloadTypes.getFirst().getQualifiedName(), messageTypeRef);

                action = new TransitionActionSendToIR(meta(sau), List.of(), messageRef, senderMachine);
            }
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
                action = new TransitionActionCustomIR(meta(feat), List.of(), List.of());
            } else {
                action = new TransitionActionMachineIR(meta(feat), List.of(), machineRef);
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

    private TypeRef toTypeRef(EList<Type> types) {
        if (types == null || types.isEmpty()) {
            return TypeRef.unknown(null);
        }

        Type type = types.getFirst();
        String qualifiedName = type.getQualifiedName();
        if (qualifiedName == null || qualifiedName.isBlank()) {
            return TypeRef.unknown(null);
        }

        return switch (qualifiedName) {
            case "ScalarValues::Boolean" -> TypeRef.scalar(ScalarType.BOOLEAN);
            case "ScalarValues::Integer" -> TypeRef.scalar(ScalarType.INTEGER);
            case "ScalarValues::Real" -> TypeRef.scalar(ScalarType.REAL);
            case "ScalarValues::String" -> TypeRef.scalar(ScalarType.STRING);
            default -> TypeRef.user(qualifiedName);
        };
    }
}
