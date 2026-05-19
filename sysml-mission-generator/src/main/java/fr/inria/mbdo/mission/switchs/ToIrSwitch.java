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
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        if (shouldSkipGeneration(object)) {
            return null;
        }

        List<MachineAttributeIR> attributes = new ArrayList<>();
        for (AttributeUsage ownedAttribute : object.getOwnedAttribute()) {
            TypeRef attributeType = toTypeRef(ownedAttribute.getType(), ownedAttribute.getQualifiedName());
            attributes.add(new MachineAttributeIR(ownedAttribute, attributeType));
        }

        List<ActionIR> actions = new ArrayList<>();
        for (ActionUsage ownedAction : object.getOwnedAction()) {
            actions.add(buildActionIR(ownedAction, ownedAction.getParameter()));
        }

        irRepositoryBuilder.add(new MachineIR(object, actions, attributes, List.of()));
        return null;
    }

    @Override
    public Void caseItemDefinition(ItemDefinition object) {
        logger.debug("Traversing item def: {}", object.getQualifiedName());
        if (shouldSkipGeneration(object) || index.resolvePart(object.getQualifiedName()).isPresent()) {
            return null;
        }
        irRepositoryBuilder.add(new MachineMessageIR(object));
        return null;
    }

    @Override
    public Void caseStateDefinition(StateDefinition object) {
        logger.debug("[State]\tTraversing state def: {}", object.getQualifiedName());
        if (shouldSkipGeneration(object)) {
            return null;
        }

        List<MachineRefIR> machinesRefs = collectMachineRefs(object);
        Map<String, List<Ref<TransitionIR>>> stateTransitions = initializeStateTransitions(object);
        List<Ref<StateIR>> stateRefs = collectStateRefs(object);

        TransitionIR defaultTransition = null;
        for (TransitionUsage transition : object.getOwnedTransition()) {
            TransitionIR transitionIR = buildTransitionIR(transition, stateTransitions);
            if (transitionIR.getTrigger() == null && defaultTransition == null) {
                defaultTransition = transitionIR;
            }
        }

        for (StateUsage state : object.getOwnedState()) {
            String stateQName = elementQualifiedName(object, state);
            List<Ref<TransitionIR>> transitions = stateTransitions.getOrDefault(stateQName, List.of());
            irRepositoryBuilder.add(new StateIR(state, transitions));
        }

        irRepositoryBuilder.add(new MachineMissionIR(object,
                defaultTransition != null ? new Ref<>(defaultTransition.getQualifiedName()) : null,
                stateRefs,
                machinesRefs,
                List.of()));
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

        irRepositoryBuilder.add(buildActionIR(object, object.getParameter()));
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
        return machinesRefs;
    }

    private Map<String, List<Ref<TransitionIR>>> initializeStateTransitions(StateDefinition object) {
        Map<String, List<Ref<TransitionIR>>> stateTransitions = new HashMap<>();
        for (StateUsage state : object.getOwnedState()) {
            String key = state.getQualifiedName();
            if (key == null || key.isBlank()) {
                key = elementQualifiedName(object, state);
            }
            stateTransitions.put(key, new ArrayList<>());
        }
        return stateTransitions;
    }

    private List<Ref<StateIR>> collectStateRefs(StateDefinition object) {
        List<Ref<StateIR>> stateRefs = new ArrayList<>();
        for (StateUsage state : object.getOwnedState()) {
            String key = state.getQualifiedName();
            if (key == null || key.isBlank()) {
                key = elementQualifiedName(object, state);
            }
            stateRefs.add(new Ref<>(key));
        }
        return stateRefs;
    }

    private TransitionIR buildTransitionIR(TransitionUsage transition,
            Map<String, List<Ref<TransitionIR>>> stateTransitions) {
        String sourceQName = transition.getSource() != null ? transition.getSource().getQualifiedName() : null;
        String targetQName = transition.getTarget().getQualifiedName();

        Ref<ActionIR> transitionActionRef = extractTransitionAction(transition);
        if (transitionActionRef == null && transition.getSource() instanceof PerformActionUsage performActionUsage) {
            ActionIR performAction = buildActionIR(performActionUsage, performActionUsage.getParameter());
            irRepositoryBuilder.add(performAction);
            transitionActionRef = new Ref<>(performAction.getQualifiedName());
        }

        if (sourceQName == null || transition.getSource() instanceof PerformActionUsage) {
            logger.debug("[State]\t\tFound default transition: {} -> {}", transition.getSource(), targetQName);
            TransitionIR defaultTransition = new TransitionIR(transition, new Ref<>(targetQName), null,
                    transitionActionRef);
            irRepositoryBuilder.add(defaultTransition);
            return defaultTransition;
        }

        logger.debug("[State]\t\tFound transition: {} -> {}", sourceQName, targetQName);

        String resolvedSourceKey = resolveStateKey(stateTransitions, sourceQName);
        if (resolvedSourceKey == null) {
            logger.warn("Transition source state not found (attempted {}): available states={}", sourceQName,
                    stateTransitions.keySet());
            throw new IllegalStateException("Transition source state not found: " + sourceQName);
        }

        String resolvedTargetKey = resolveStateKey(stateTransitions, targetQName);
        if (resolvedTargetKey == null) {
            logger.warn("Transition target state not found (attempted {}): available states={}", targetQName,
                    stateTransitions.keySet());
            throw new IllegalStateException("Transition target state not found: " + targetQName);
        }

        AcceptActionUsage triggerElement = extractTriggerElement(transition);
        AcceptExprIR accept = extractTriggerAcceptExpression(triggerElement);
        if (accept != null) {
            irRepositoryBuilder.add(accept);
        }
        TriggerIR trigger = new TriggerIR(triggerElement, accept != null ? new Ref<>(accept.getQualifiedName()) : null);
        irRepositoryBuilder.add(trigger);

        TransitionIR transitionIR = new TransitionIR(transition, new Ref<>(resolvedTargetKey),
                new Ref<>(trigger.getQualifiedName()), transitionActionRef);
        irRepositoryBuilder.add(transitionIR);
        stateTransitions.get(resolvedSourceKey).add(new Ref<>(transitionIR.getQualifiedName()));
        return transitionIR;
    }

    private String resolveStateKey(Map<String, List<Ref<TransitionIR>>> stateTransitions, String candidate) {
        if (candidate == null) {
            return null;
        }
        if (stateTransitions.containsKey(candidate)) {
            return candidate;
        }

        // try to match by suffix (local name)
        int last = Math.max(candidate.lastIndexOf("::"), candidate.lastIndexOf('.'));
        String local = last >= 0 ? candidate.substring(last + 2) : candidate;
        for (String key : stateTransitions.keySet()) {
            if (key.endsWith("::" + local) || key.endsWith("." + local) || key.equals(local)) {
                return key;
            }
            // also try last segment of key
            int klast = Math.max(key.lastIndexOf("::"), key.lastIndexOf('.'));
            String kLocal = klast >= 0 ? key.substring(klast + 2) : key;
            if (kLocal.equals(local)) {
                return key;
            }
        }

        return null;
    }

    private Ref<ActionIR> extractTransitionAction(TransitionUsage transition) {
        if (transition.getEffectAction() == null || transition.getEffectAction().isEmpty()) {
            return null;
        }

        ActionUsage effectActionUsage = transition.getEffectAction().getFirst();
        ActionIR effectAction = buildActionIR(effectActionUsage, effectActionUsage.getParameter());
        irRepositoryBuilder.add(effectAction);
        return new Ref<>(effectAction.getQualifiedName());
    }

    private ActionIR buildActionIR(Element element, EList<? extends Feature> parameters) {
        List<ParameterIR> parameterIRs = new ArrayList<>();
        for (Feature param : parameters) {
            TypeRef paramType = toParameterTypeRef(param, element.getQualifiedName());
            parameterIRs.add(new ParameterIR(param, paramType));
        }

        return new ActionIR(element, parameterIRs, extractActionBodyStatements(element));
    }

    private List<String> extractActionBodyStatements(Element element) {
        List<String> statements = new ArrayList<>();
        Set<Element> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        collectActionBodyStatements(element, statements, visited);
        return statements;
    }

    private void collectActionBodyStatements(Element element, List<String> statements, Set<Element> visited) {
        if (!visited.add(element)) {
            return;
        }

        if (element instanceof PerformActionUsage performActionUsage) {
            String statement = renderPerformActionStatement(performActionUsage);
            if (statement != null) {
                statements.add(statement);
            }
        } else if (element instanceof SendActionUsage sendActionUsage) {
            String statement = renderSendActionStatement(sendActionUsage);
            if (statement != null) {
                statements.add(statement);
            }
        }

        for (Element ownedElement : element.getOwnedElement()) {
            collectActionBodyStatements(ownedElement, statements, visited);
        }

        for (var ownedRelationship : element.getOwnedRelationship()) {
            for (Element relatedElement : ownedRelationship.getOwnedRelatedElement()) {
                collectActionBodyStatements(relatedElement, statements, visited);
            }
            for (Element relatedElement : ownedRelationship.getSource()) {
                collectActionBodyStatements(relatedElement, statements, visited);
            }
        }
    }

    private String renderPerformActionStatement(PerformActionUsage performActionUsage) {
        ActionUsage performedAction = performActionUsage.getPerformedAction();
        if (performedAction == null || performedAction.inputParameters() != null
                && !performedAction.inputParameters().isEmpty()) {
            return null;
        }

        logger.debug("[Action] render perform usage name={} performed={} args={} inputs={}",
                performActionUsage.getName(),
                performedAction.getQualifiedName(),
                performActionUsage.getOwnedElement().stream().map(Element::getName).toList(),
                performedAction.inputParameters().stream().map(Feature::getName).toList());

        String performedQualifiedName = performedAction.getQualifiedName();
        if (performedQualifiedName == null || performedQualifiedName.isBlank()) {
            return null;
        }

        String[] segments = performedQualifiedName.split("::");
        if (segments.length < 2) {
            return null;
        }

        String receiverName = segments[segments.length - 2];
        String actionName = segments[segments.length - 1];
        if (receiverName.isBlank() || actionName.isBlank()) {
            return null;
        }

        return receiverName + "." + actionName + "()";
    }

    private String renderSendActionStatement(SendActionUsage sendActionUsage) {
        String receiver = renderExpression(sendActionUsage.getReceiverArgument());
        String payload = renderSendPayload(sendActionUsage.getPayloadArgument());
        if (receiver == null || payload == null) {
            return null;
        }

        return receiver + ".publish(" + payload + ")";
    }

    private String renderSendPayload(org.eclipse.syson.sysml.Expression expression) {
        if (expression instanceof InvocationExpression invocationExpression) {
            if (invocationExpression.getFunction() == null
                    || invocationExpression.getFunction().getQualifiedName() == null) {
                return null;
            }
            return terminalName(invocationExpression.getFunction().getQualifiedName()) + ".now()";
        }

        if (expression instanceof FeatureReferenceExpression featureReferenceExpression) {
            if (featureReferenceExpression.getReferent() == null
                    || featureReferenceExpression.getReferent().getQualifiedName() == null) {
                return null;
            }
            return terminalName(featureReferenceExpression.getReferent().getQualifiedName()) + ".now()";
        }

        return null;
    }

    private String renderExpression(org.eclipse.syson.sysml.Expression expression) {
        if (expression instanceof FeatureReferenceExpression featureReferenceExpression) {
            if (featureReferenceExpression.getReferent() == null
                    || featureReferenceExpression.getReferent().getQualifiedName() == null) {
                return null;
            }
            return terminalName(featureReferenceExpression.getReferent().getQualifiedName());
        }

        return null;
    }

    private String terminalName(String qualifiedName) {
        if (qualifiedName == null || qualifiedName.isBlank()) {
            return null;
        }

        int separatorIndex = qualifiedName.lastIndexOf("::");
        if (separatorIndex < 0) {
            return qualifiedName;
        }

        return qualifiedName.substring(separatorIndex + 2);
    }

    private TypeRef toParameterTypeRef(Feature parameter, String ownerQualifiedName) {
        EList<Type> types = parameter.getType();
        if (types == null || types.isEmpty()) {
            return TypeRef.unknown(ownerQualifiedName + "::" + parameter.getName());
        }
        return toTypeRef(types, ownerQualifiedName);
    }

    private AcceptActionUsage extractTriggerElement(TransitionUsage transition) {
        if (transition.getTriggerAction() == null || transition.getTriggerAction().isEmpty()) {
            return null;
        }
        return transition.getTriggerAction().getFirst();
    }

    private AcceptExprIR extractTriggerAcceptExpression(AcceptActionUsage accept) {
        if (accept == null) {
            return null;
        }

        String acceptBaseName = "AcceptEventExpr";
        String acceptQualifiedName = buildUniqueAcceptExprQualifiedName(accept);

        // accept Event / accept evt : Event
        ReferenceUsage payload = accept.getPayloadParameter();
        if (payload != null) {
            EList<Type> payloadTypes = payload.getType();
            if (payloadTypes != null && !payloadTypes.isEmpty()) {
                TypeRef eventType = toTypeRef(payloadTypes, accept.getQualifiedName());
                logger.debug("[Trigger] accept payload param '{}' typed as {}",
                        payload.getName(), eventType.qualifiedName());
                return new AcceptEventExprIR(accept, eventType, acceptBaseName, acceptQualifiedName);
            }
        }

        // fallback: some models expose event type via payload argument reference
        if (accept.getPayloadArgument() instanceof FeatureReferenceExpression featureReferenceExpression
                && featureReferenceExpression.getReferent() != null
                && featureReferenceExpression.getReferent().getQualifiedName() != null) {
            String qName = featureReferenceExpression.getReferent().getQualifiedName();
            logger.debug("[Trigger] accept payload argument referent type {}", qName);
            return new AcceptEventExprIR(accept, TypeRef.user(qName), acceptBaseName, acceptQualifiedName);
        }

        logger.debug("[Trigger] unresolved accept expression for {}", accept.getQualifiedName());
        return null;
    }

    private String buildUniqueAcceptExprQualifiedName(AcceptActionUsage accept) {
        String ownerQName = accept.getOwner() != null ? accept.getOwner().getQualifiedName() : null;
        String base = accept.getQualifiedName();
        if (base == null || base.isBlank()) {
            base = (ownerQName == null || ownerQName.isBlank())
                    ? "AcceptActionUsage"
                    : ownerQName + "::AcceptActionUsage";
        }
        String elementId = accept.getElementId();
        if (elementId == null || elementId.isBlank()) {
            elementId = Integer.toHexString(System.identityHashCode(accept));
        }
        return base + "#" + elementId;
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
        String usageName = usage.getName();
        if (usageName == null || usageName.isBlank()) {
            return ownerName + "::" + usage.eClass().getName();
        }

        String result = ownerName + "::" + usageName;
        if (!result.equals(usage.getQualifiedName())) {
            throw new IllegalStateException(result + " != " + usage.getQualifiedName());
        }
        return result;
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
}
