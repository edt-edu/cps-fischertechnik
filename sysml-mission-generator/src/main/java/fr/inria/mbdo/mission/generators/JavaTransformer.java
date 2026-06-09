package fr.inria.mbdo.mission.generators;

import com.palantir.javapoet.*;
import fr.inria.mbdo.mission.ir.*;
import fr.inria.mbdo.mission.runtime.api.AbstractMissionStrategy;
import fr.inria.mbdo.mission.runtime.api.MachineAdapter;
import fr.inria.mbdo.mission.runtime.rtc.def.RuntimeState;
import fr.inria.mbdo.mission.runtime.rtc.def.RuntimeTransition;
import fr.inria.mbdo.mission.runtime.rtc.event.CompletionEvent;
import fr.inria.mbdo.mission.runtime.rtc.event.Event;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.Modifier;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static fr.inria.mbdo.mission.utils.StringUtils.toUpperFirst;

public class JavaTransformer {

    private static final String JAVADOC_FROM = "From $L\n$L";
    private static final String COMMENT_TRANSITIONS = "Transitions connect triggers, runtime actions, and next-state targets.";
    private static final String COMMENT_STATES = "States are built from the collected transitions.";
    private static final String COMMENT_SUBSCRIPTIONS = "Subscribe each mission machine to trigger event types used by this mission.";
    private static final String EMPTY_RUNTIME_ACTION = "event -> { }";
    private static final String ALWAYS_TRUE_GUARD = "event -> true";
    private static final String ACTIONS_SUFFIX = "Actions";
    private static final String GUARDS_SUFFIX = "Guards";
    private static final String CURRENT_COMMAND_ATTR = "currentCommand";
    private static final String COMMAND_SUCCESS_MESSAGE = "CommandSuccessEventMessage";
    private static final String JAVADOC_ACCEPT_WHEN_EVENT = """
            Generated accept-when event trigger for runtime transitions.
            SysML source:
            <pre>{@code
            $L
            }</pre>
            """;
    private static final String JAVADOC_ADAPTER_TRIGGER_PLACEHOLDER = """
            Adapter-side placeholder to trigger this accept-when event.
            Replace the false guard with the real condition and keep calling this method from adapter polling/loops.
            SysML source:
            <pre>{@code
            $L
            }</pre>""";
    private static final String JAVADOC_TRANSITION_ACTION = """
            Transition action for $L.
            Generated from SysML action body:
            <pre>{@code
            $L
            }</pre>""";
    private static final String JAVADOC_TRANSITION_ACTION_TODO = "TODO: implement transition action for $L";

    private static final String STMT_ASSIGN_FIELD = "this.$N = $N";
    private static final String STMT_NEW_STATE = "$T $N = new $T($S)";
    private static final String STMT_ADD_TRANSITION = "$N.addTransition(new $T($T.class, $L, $L, $N))";
    private static final String STMT_SET_ENTRY_TRANSITION = "this.runtime.setEntryTransition(new $T($T.class, $L, $L, $N))";
    private static final String STMT_SUBSCRIBE = "$N.subscribe($T.class, this::onEvent)";
    private static final String STMT_RETURN_NAME = "return $S";
    private static final String STMT_DECLARE_ACCEPT_EVENT = "$T acceptWhenEvent = null";
    private static final String STMT_PUBLISH_ACCEPT_EVENT = "publish(acceptWhenEvent)";

    private static final String EXPR_CUSTOM_ACTION_PREFIX = "event -> this.actions.";
    private static final String EXPR_MACHINE_ACTION_WITH_EVENT = "event -> this.%s.%s(event)";
    private static final String EXPR_MACHINE_ACTION_NO_ARGS = "event -> this.%s.%s()";
    private static final String EXPR_SEND_TO_PUBLISH = "event -> this.%s.publish(new $T())";

    private final Logger logger = LoggerFactory.getLogger(JavaTransformer.class);
    private final IrRepository irRepository;
    private final TypeTable typeTable;

    public JavaTransformer(IrRepository irRepository, TypeTable typeTable) {
        this.irRepository = irRepository;
        this.typeTable = typeTable;
    }

    public Map<String, JavaFile> generate() {
        Map<String, JavaFile> result = new LinkedHashMap<>();
        result.putAll(generateMachinesInterfaces());
        result.putAll(generateEnumerations());
        result.putAll(generateMachineEventMessages());
        result.putAll(generateAcceptEventClasses());
        result.putAll(generateMachinesMissionsClasses());
        result.putAll(generateMachinesMissionsActionsUtilsInterfaces());

        return result;
    }

    private Map<String, JavaFile> generateAcceptEventClasses() {
        Map<String, JavaFile> javaFiles = new LinkedHashMap<>();

        for (TransitionTriggerIR trigger : irRepository.getTriggers().values()) {
            if (trigger instanceof TransitionTriggerWhenIR triggerWhen) {
                logger.info("Generating accept-event class for {}", triggerWhen.getQualifiedName());

                TypeSpec.Builder eventBuilder = TypeSpec.classBuilder(typeTable.resolveClassNameOrThrow(trigger.getQualifiedName()))
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .addSuperinterface(Event.class)
                        .addJavadoc(JAVADOC_ACCEPT_WHEN_EVENT, trigger.toString());

                javaFiles.put(triggerWhen.getQualifiedName(),
                        JavaFile.builder(typeTable.resolvePackageOrThrow(trigger.getQualifiedName()),
                                eventBuilder.build()).build());
            }
        }

        return javaFiles;
    }

    private Map<String, JavaFile> generateMachinesInterfaces() {
        Map<String, JavaFile> javaFiles = new LinkedHashMap<>();

        for (MachineIR machine : irRepository.getMachines().values()) {
            logger.info("Generating machine interface for {}", machine.getQualifiedName());

            TypeSpec.Builder interfaceBuilder = TypeSpec.interfaceBuilder(machine.getName())
                    .addModifiers(Modifier.PUBLIC)
                    .addSuperinterface(MachineAdapter.class)
                    .addJavadoc(JAVADOC_FROM, machine.getQualifiedName(), machine.getDocumentation());

            List<MethodSpec> methods = new ArrayList<>();

            /* Setup Adapter attributes */
            for (MachineAttributeIR attribute : machine.getAttributes()) {
                TypeName type = typeTable.resolve(attribute.getType());
                methods.add(MethodSpec.methodBuilder("get" + toUpperFirst(attribute.getName()))
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .returns(type)
                        .build());
                methods.add(MethodSpec.methodBuilder("set" + toUpperFirst(attribute.getName()))
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(type, attribute.getName())
                        .build());
            }

            /*
             * Add this method in case the machine defines a CommandSuccessEventMessage, so
             * it can be used to notify the related SM about completion result
             */
            if (hasCurrentCommand(machine) && hasMessage(COMMAND_SUCCESS_MESSAGE)) {
                methods.add(MethodSpec.methodBuilder("setCommandSuccess")
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(findCurrentCommandType(machine), "command")
                        .build());
            }

            /* Implement here the operations corresponding to machine actions */
            for (Map.Entry<String, Ref<MachineActionIR>> action : machine.getActions().entrySet()) {
                Ref<MachineActionIR> actionRef = action.getValue();
                MachineActionIR machineActionIR = resolveActionIR(actionRef);
                if (machineActionIR == null) {
                    // best-effort: skip missing actions rather than fail generation
                    logger.warn("Skipping unknown action {} on machine {}",
                            actionRef == null ? null : actionRef.qName(),
                            machine.getQualifiedName());
                    continue;
                }

                MethodSpec.Builder actionBuilder = MethodSpec.methodBuilder(machineActionIR.getName())
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addJavadoc(JAVADOC_FROM, machineActionIR.getQualifiedName(),
                                machineActionIR.getDocumentation());
                for (var parameter : machineActionIR.getParameters()) {
                    actionBuilder.addParameter(
                            ParameterSpec.builder(typeTable.resolve(parameter.getType()), parameter.getName()).build());
                }
                methods.add(actionBuilder.build());
            }

            for (TransitionTriggerWhenIR triggerWhen : resolveAcceptWhenTriggersForMachine(machine)) {
                ClassName acceptEventType = typeTable.resolveClassNameOrThrow(triggerWhen.getQualifiedName());

                methods.add(MethodSpec.methodBuilder("trigger" + triggerWhen.getQualifiedName())
                        .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                        .addJavadoc(JAVADOC_ADAPTER_TRIGGER_PLACEHOLDER, triggerWhen.toString())
                        .beginControlFlow("if (false)")
                        .addStatement(STMT_DECLARE_ACCEPT_EVENT, acceptEventType)
                        .addStatement(STMT_PUBLISH_ACCEPT_EVENT)
                        .endControlFlow()
                        .build());
            }

            interfaceBuilder.addMethods(methods);
            javaFiles.put(machine.getQualifiedName() + "::interface",
                    JavaFile.builder(typeTable.resolvePackageOrThrow(machine.getQualifiedName()),
                            interfaceBuilder.build()).build());
        }

        return javaFiles;
    }

    private Map<String, JavaFile> generateEnumerations() {
        Map<String, JavaFile> javaFiles = new LinkedHashMap<>();

        for (EnumerationIR enumeration : irRepository.getEnumerations().values()) {
            logger.info("Generating enumeration for {}", enumeration.getQualifiedName());
            TypeSpec.Builder enumBuilder = TypeSpec.enumBuilder(enumeration.getName())
                    .addModifiers(Modifier.PUBLIC)
                    .addJavadoc(JAVADOC_FROM, enumeration.getQualifiedName(), enumeration.getDocumentation());
            for (String constant : enumeration.getConstants()) {
                enumBuilder.addEnumConstant(constant);
            }
            javaFiles.put(enumeration.getQualifiedName(),
                    JavaFile.builder(typeTable.resolvePackageOrThrow(enumeration.getQualifiedName()),
                            enumBuilder.build()).build());
        }

        return javaFiles;
    }

    private Map<String, JavaFile> generateMachineEventMessages() {
        Map<String, JavaFile> javaFiles = new LinkedHashMap<>();

        for (MachineMessageIR message : irRepository.getMessages().values()) {
            TypeSpec messageClass = TypeSpec.classBuilder(message.getName())
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addSuperinterface(Event.class).build();
            javaFiles.put(message.getQualifiedName(), JavaFile
                    .builder(typeTable.resolvePackageOrThrow(message.getQualifiedName()), messageClass).build());
        }

        return javaFiles;
    }

    private Map<String, JavaFile> generateMachinesMissionsClasses() {
        Map<String, JavaFile> javaFiles = new LinkedHashMap<>();

        for (MachineMissionIR mission : irRepository.getMissions().values()) {
            logger.info("Generating mission for {}", mission.getQualifiedName());

            List<StateIR> states = resolveMissionStates(mission);
            List<TransitionIR> transitions = resolveMissionTransitions(mission, states);

            TypeSpec.Builder missionBuilder = TypeSpec.classBuilder(mission.getName())
                    .addModifiers(Modifier.PUBLIC)
                    .superclass(AbstractMissionStrategy.class)
                    .addJavadoc(JAVADOC_FROM, mission.getQualifiedName(), mission.getDocumentation())
                    .addField(FieldSpec.builder(ClassName.get(Logger.class), "logger", Modifier.PRIVATE,
                                    Modifier.STATIC, Modifier.FINAL)
                            .initializer("$T.getLogger($S)", LoggerFactory.class,
                                    mission.getName())
                            .build());

            // Add a field for the runtime actions implementor that users should provide.
            ClassName actionsInterface = ClassName.get(typeTable.resolvePackageOrThrow(mission.getQualifiedName()),
                    mission.getName() + ACTIONS_SUFFIX);
            missionBuilder.addField(FieldSpec.builder(actionsInterface, "actions", Modifier.PRIVATE,
                    Modifier.FINAL).build());
            for (MachineRefIR machineRef : mission.getMachinesRefs()) {
                missionBuilder.addField(buildMachineRefField(machineRef));
            }

            missionBuilder.addMethod(buildMissionConstructor(mission, states, transitions));
            missionBuilder.addMethod(buildGetNameMethod(mission));

            javaFiles.put(mission.getQualifiedName(),
                    JavaFile.builder(typeTable.resolvePackageOrThrow(mission.getQualifiedName()),
                            missionBuilder.build()).build());
        }

        return javaFiles;
    }

    private Map<String, JavaFile> generateMachinesMissionsActionsUtilsInterfaces() {
        Map<String, JavaFile> javaFiles = new LinkedHashMap<>();

        for (MachineMissionIR mission : irRepository.getMissions().values()) {
            logger.info("Generating mission actions utils interface for {}", mission.getQualifiedName());

            List<StateIR> states = resolveMissionStates(mission);
            List<TransitionIR> transitions = resolveMissionTransitions(mission, states);
            Map<String, String> customActionMethodNamesByQualifiedName = resolveCustomTransitionActionMethodNames(
                    transitions);

            TypeSpec.Builder actionsInterfaceBuilder = TypeSpec.interfaceBuilder(mission.getName() + ACTIONS_SUFFIX)
                    .addModifiers(Modifier.PUBLIC)
                    .addJavadoc(JAVADOC_FROM, mission.getQualifiedName(), mission.getDocumentation());

            Map<String, TransitionActionCustomIR> customActions = transitions.stream()
                    .map(this::resolveTransitionAction)
                    .filter(TransitionActionCustomIR.class::isInstance)
                    .map(TransitionActionCustomIR.class::cast)
                    .collect(Collectors.toMap(
                            TransitionActionCustomIR::getQualifiedName,
                            Function.identity(),
                            (first, second) -> first));

            for (TransitionActionCustomIR customAction : customActions.values()) {
                String methodName = customActionMethodNamesByQualifiedName.get(customAction.getQualifiedName());

                if (methodName == null) {
                    throw new IllegalStateException(
                            "Missing generated method name for custom transition action "
                                    + customAction.getQualifiedName());
                }

                String sysmlBody = String.join("\n", customAction.getBodyStatements());

                MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(Event.class, "event")
                        .addParameters(mission.getMachinesRefs().stream()
                                .map(ref -> ParameterSpec.builder(typeTable.resolve(ref.type()), ref.name()).build())
                                .toList());

                if (!sysmlBody.isBlank()) {
                    methodBuilder.addJavadoc(JAVADOC_TRANSITION_ACTION,
                            customAction.getQualifiedName(),
                            sysmlBody);
                } else {
                    methodBuilder.addJavadoc(JAVADOC_TRANSITION_ACTION_TODO,
                            customAction.getQualifiedName());
                }

                actionsInterfaceBuilder.addMethod(methodBuilder.build());
            }

            javaFiles.put(mission.getQualifiedName() + "::actions-interface",
                    JavaFile.builder(typeTable.resolvePackageOrThrow(mission.getQualifiedName()),
                            actionsInterfaceBuilder.build()).build());
        }

        return javaFiles;
    }

    private MethodSpec buildMissionConstructor(MachineMissionIR mission, List<StateIR> states,
                                               List<TransitionIR> transitions) {
        MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC);

        for (MachineRefIR machineRef : mission.getMachinesRefs()) {
            String machineName = machineRef.name();
            constructorBuilder.addParameter(typeTable.resolve(machineRef.type()), machineName);
            constructorBuilder.addStatement(STMT_ASSIGN_FIELD, machineName, machineName);
        }

        // Add constructor parameter for the actions implementor and assign it.
        ClassName actionsInterface = ClassName.get(typeTable.resolvePackageOrThrow(mission.getQualifiedName()),
                mission.getName() + ACTIONS_SUFFIX);
        constructorBuilder.addParameter(actionsInterface, "actions");
        constructorBuilder.addStatement(STMT_ASSIGN_FIELD, "actions", "actions");

        Map<String, String> customActionMethodNamesByQualifiedName = resolveCustomTransitionActionMethodNames(
                transitions);

        Map<String, String> guardMethodNamesByQualifiedName = resolveTransitionGuardMethodNames(transitions);
        Map<String, String> transitionGuardExpressionsByGuardQName = new LinkedHashMap<>();
        ClassName guardsClass = ClassName.get(typeTable.resolvePackageOrThrow(mission.getQualifiedName()),
                mission.getName() + GUARDS_SUFFIX);
        for (TransitionIR transition : transitions) {
            String transitionGuardQName = resolveTransitionGuardQName(transition);
            if (transitionGuardQName == null) {
                continue;
            }

            String methodName = guardMethodNamesByQualifiedName.get(transitionGuardQName);
            if (methodName == null) {
                throw new IllegalStateException("Missing generated method name for guard "
                        + transitionGuardQName);
            }

            transitionGuardExpressionsByGuardQName.put(transitionGuardQName,
                    guardsClass.simpleName() + "::" + methodName);
        }

        constructorBuilder.addCode("\n");
        Map<String, String> stateRuntimeNames = emitStateBlock(constructorBuilder, states);

        constructorBuilder.addCode("\n");
        emitStateTransitionsBlock(constructorBuilder, transitions, stateRuntimeNames,
                customActionMethodNamesByQualifiedName, transitionGuardExpressionsByGuardQName,
                mission.getMachinesRefs());

        constructorBuilder.addCode("\n");
        emitEventSubscriptions(constructorBuilder, mission.getMachinesRefs(), transitions);

        return constructorBuilder.build();
    }

    private Map<String, String> emitStateBlock(MethodSpec.Builder constructorBuilder, List<StateIR> states) {
        constructorBuilder.addComment(COMMENT_STATES);
        Map<String, String> stateRuntimeNames = new LinkedHashMap<>();
        for (StateIR state : states) {
            String stateVarName = cleanName(state.getName());
            constructorBuilder.addStatement(STMT_NEW_STATE, RuntimeState.class, stateVarName, RuntimeState.class,
                    state.getName());
            stateRuntimeNames.put(state.getQualifiedName(), stateVarName);
        }

        return stateRuntimeNames;
    }

    private void emitStateTransitionsBlock(MethodSpec.Builder constructorBuilder, List<TransitionIR> transitions,
                                           Map<String, String> stateRuntimeNames,
                                           Map<String, String> customActionMethodNamesByQualifiedName,
                                           Map<String, String> transitionGuardExpressionsByTransitionQName,
                                           List<MachineRefIR> machineRefs) {
        constructorBuilder.addComment(COMMENT_TRANSITIONS);

        for (TransitionIR transition : transitions) {

            logger.info("[Transition]\t\tBuilding transition {}", transition.getQualifiedName());

            Optional<StateIR> sourceState = resolveState(transition.getFromState());
            Optional<StateIR> targetState = resolveState(transition.getToState());
            TypeName triggerType = resolveTrigger(transition);
            CodeBlock transitionActionExpression = resolveTransitionActionExpression(transition,
                    customActionMethodNamesByQualifiedName, machineRefs);
            String transitionGuardExpression = resolveTransitionGuardExpression(transition,
                    transitionGuardExpressionsByTransitionQName);

            String targetStateVarName = stateRuntimeNames.get(
                    targetState.orElseThrow(() -> new RuntimeException("Missing target state")).getQualifiedName());

            if (sourceState.isPresent()) {
                String sourceStateVarName = stateRuntimeNames.get(sourceState.get().getQualifiedName());
                constructorBuilder.addStatement(STMT_ADD_TRANSITION,
                        sourceStateVarName, RuntimeTransition.class,
                        triggerType, transitionGuardExpression, transitionActionExpression, targetStateVarName);
            } else {
                constructorBuilder.addStatement(STMT_SET_ENTRY_TRANSITION,
                        RuntimeTransition.class,
                        triggerType, transitionGuardExpression, transitionActionExpression, targetStateVarName);
            }
        }
    }

    private void emitEventSubscriptions(MethodSpec.Builder constructorBuilder, List<MachineRefIR> machineRefs,
                                        List<TransitionIR> transitions) {
        constructorBuilder.addComment(COMMENT_SUBSCRIPTIONS);

        for (MachineRefIR machineRef : machineRefs) {
            MachineIR machine = resolveMachine(machineRef);
            Set<TypeName> subscribedEventTypes = new LinkedHashSet<>();

            for (TransitionIR transition : transitions) {
                TransitionTriggerIR trigger = resolveTriggerIR(transition);
                TypeName triggerType = resolveTrigger(transition);
                if (trigger == null || triggerType.equals(TypeName.get(CompletionEvent.class))) {
                    continue;
                }

                MachineMessageIR eventMessage = resolveTriggerMessage(transition);
                if (eventMessage == null || !machineDefinesMessage(machineRef, machine, eventMessage)) {
                    continue;
                }

                subscribedEventTypes.add(triggerType);
            }

            for (TypeName subscribedEventType : subscribedEventTypes) {
                constructorBuilder.addStatement(STMT_SUBSCRIBE,
                        machineRef.name(), subscribedEventType);
            }
        }
    }

    private MethodSpec buildGetNameMethod(MachineMissionIR mission) {
        return MethodSpec.methodBuilder("getName")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement(STMT_RETURN_NAME, mission.getName())
                .build();
    }

    private FieldSpec buildMachineRefField(MachineRefIR machineRef) {
        return FieldSpec.builder(typeTable.resolve(machineRef.type()), machineRef.name(), Modifier.PRIVATE,
                Modifier.FINAL).build();
    }

    private List<StateIR> resolveMissionStates(MachineMissionIR mission) {
        List<StateIR> states = new ArrayList<>();
        for (Ref<StateIR> stateRef : mission.getStates()) {
            StateIR state = irRepository.getStates().get(stateRef.qName());
            if (state != null) {
                states.add(state);
            }
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

    private Map<String, String> resolveCustomTransitionActionMethodNames(List<TransitionIR> transitions) {
        Map<String, String> customActionMethodNamesByQualifiedName = new LinkedHashMap<>();
        Set<String> usedMethodNames = new LinkedHashSet<>();

        for (TransitionIR transition : transitions) {
            TransitionActionIR transitionAction = resolveTransitionAction(transition);
            if (!(transitionAction instanceof TransitionActionCustomIR customAction)) {
                continue;
            }

            if (customAction.getName() == null) {
                throw new IllegalStateException("Custom transition action has no name for transition " + transition.getQualifiedName());
            }

            customActionMethodNamesByQualifiedName.computeIfAbsent(customAction.getQualifiedName(), key -> {
                String baseMethodName = cleanName(customAction.getName());
                String methodName = uniqueLocalName(baseMethodName, usedMethodNames);
                usedMethodNames.add(methodName);
                return methodName;
            });
        }

        return customActionMethodNamesByQualifiedName;
    }

    private TransitionActionIR resolveTransitionAction(TransitionIR transition) {
        if (transition == null || transition.getAction() == null) {
            return null;
        }

        TransitionActionIR transitionAction = irRepository.getTransitionActions().get(transition.getAction().qName());
        if (transitionAction == null) {
            throw new IllegalStateException("Missing transition action IR for transition "
                    + transition.getQualifiedName());
        }

        return transitionAction;
    }

    private Optional<StateIR> resolveState(Ref<StateIR> stateRef) {
        if (stateRef == null) {
            return Optional.empty();
        }
        StateIR state = irRepository.getStates().get(stateRef.qName());
        return state != null ? Optional.of(state) : Optional.empty();
    }

    private MachineIR resolveMachine(MachineRefIR machineRef) {
        if (machineRef == null || machineRef.type() == null || machineRef.type().qualifiedName() == null) {
            throw new IllegalStateException("Mission references a machine without a qualified type");
        }

        MachineIR machine = irRepository.getMachines().get(machineRef.type().qualifiedName());
        if (machine == null) {
            throw new IllegalStateException("Missing machine IR for mission reference: "
                    + machineRef.type().qualifiedName());
        }

        return machine;
    }

    private MachineMessageIR resolveTriggerMessage(TransitionIR transition) {
        TransitionTriggerIR trigger = resolveTriggerIR(transition);
        if (trigger instanceof TransitionTriggerSimpleIR triggerSimple) {
            Ref<MachineMessageIR> eventMessageRef = triggerSimple.getEventMessage();
            if (eventMessageRef == null || eventMessageRef.qName() == null || eventMessageRef.qName().isBlank()) {
                throw new IllegalStateException("Trigger " + triggerSimple.getQualifiedName()
                        + " must reference a generated EventMessage class");
            }

            MachineMessageIR eventMessage = irRepository.getMessages().get(eventMessageRef.qName());
            if (eventMessage == null) {
                throw new IllegalStateException("Trigger " + triggerSimple.getQualifiedName()
                        + " must reference a generated EventMessage class, but no generated message exists for "
                        + eventMessageRef.qName());
            }

            return eventMessage;
        }

        return null;
    }

    private TransitionTriggerIR resolveTriggerIR(TransitionIR transition) {

        if (transition == null || transition.getTrigger() == null) {
            return null;
        }

        TransitionTriggerIR trigger = irRepository.getTriggers().get(transition.getTrigger().qName());
        if (trigger == null) {
            throw new IllegalStateException("Missing trigger IR for transition " + transition.getQualifiedName());
        }

        return trigger;
    }

    private boolean machineDefinesMessage(MachineRefIR machineRef, MachineIR machine, MachineMessageIR message) {
        if (machine == null || message == null) {
            return false;
        }

        if (machine.getMessages().stream()
                .anyMatch(machineMessage -> message.getQualifiedName().equals(machineMessage.qName()))) {
            return true;
        }

        List<String> ownershipSignals = getMachineSignals(machineRef, machine);

        String messageQualifiedName = message.getQualifiedName() == null ? ""
                : message.getQualifiedName().toLowerCase(Locale.ROOT);
        String messagePackage = typeTable.resolvePackage(message.getQualifiedName()).orElse("");
        messagePackage = messagePackage.toLowerCase(Locale.ROOT);
        for (String ownershipSignal : ownershipSignals) {
            if (ownershipSignal == null || ownershipSignal.isBlank()) {
                continue;
            }
            String normalizedSignal = ownershipSignal.toLowerCase(Locale.ROOT);
            if (messageQualifiedName.contains(normalizedSignal) || messagePackage.contains(normalizedSignal)) {
                return true;
            }
        }

        return false;
    }

    private static @NonNull List<String> getMachineSignals(MachineRefIR machineRef, MachineIR machine) {
        List<String> ownershipSignals = new ArrayList<>();
        if (machineRef != null && machineRef.name() != null) {
            ownershipSignals.add(machineRef.name());
        }
        if (machine.getName() != null) {
            ownershipSignals.add(machine.getName());
        }

        String machineTypeQName = machineRef != null && machineRef.type() != null ? machineRef.type().qualifiedName()
                : null;
        if (machineTypeQName != null && !machineTypeQName.isBlank()) {
            int sysmlSeparator = machineTypeQName.lastIndexOf("::");
            int dotSeparator = machineTypeQName.lastIndexOf('.');
            int lastNamespaceSeparator = Math.max(sysmlSeparator, dotSeparator);
            String machineTypeSimpleName = lastNamespaceSeparator >= 0
                    ? machineTypeQName.substring(lastNamespaceSeparator + (sysmlSeparator >= dotSeparator ? 2 : 1))
                    : machineTypeQName;
            ownershipSignals.add(machineTypeSimpleName);
        }
        return ownershipSignals;
    }

    private TypeName resolveTrigger(TransitionIR transition) {
        TransitionTriggerIR trigger = resolveTriggerIR(transition);
        if (trigger == null) {
            return TypeName.get(CompletionEvent.class);
        }

        if (trigger instanceof TransitionTriggerWhenIR triggerWhen) {
            return typeTable.resolveClassNameOrThrow(triggerWhen.getQualifiedName());
        }

        MachineMessageIR eventMessage = resolveTriggerMessage(transition);
        if (eventMessage == null) {
            throw new IllegalStateException("Missing trigger IR for transition " + transition.getQualifiedName());
        }

        return typeTable.resolveClassNameOrThrow(eventMessage.getQualifiedName());
    }

    private CodeBlock resolveTransitionActionExpression(TransitionIR transition,
                                                        Map<String, String> customActionMethodNamesByQualifiedName,
                                                        List<MachineRefIR> machineRefs) {
        TransitionActionIR transitionAction = resolveTransitionAction(transition);
        if (transitionAction == null) {
            return CodeBlock.of(EMPTY_RUNTIME_ACTION);
        }

        return switch (transitionAction) {
            case TransitionActionCustomIR customAction -> {
                String methodName = customActionMethodNamesByQualifiedName.get(customAction.getQualifiedName());
                if (methodName == null) {
                    throw new IllegalStateException("Transition " + transition.getQualifiedName()
                            + " has a custom action but no generated action method mapping");
                }
                StringBuilder call = new StringBuilder();
                call.append(EXPR_CUSTOM_ACTION_PREFIX).append(methodName).append("(");
                call.append("event");
                if (machineRefs != null) {
                    for (MachineRefIR mr : machineRefs) {
                        call.append(", this.").append(mr.name());
                    }
                }
                call.append(")");
                yield CodeBlock.of(call.toString());
            }
            case TransitionActionMachineIR machineAction -> {
                String machineName = machineAction.getMachineRef() != null ? machineAction.getMachineRef().name()
                        : null;
                if (machineName == null || machineName.isBlank()) {
                    throw new IllegalStateException("Transition " + transition.getQualifiedName()
                            + " has a machine action without a machine reference");
                }
                String methodName = cleanName(machineAction.getName());
                // try to resolve ActionIR to match parameter count
                String underlyingQName = machineAction.getQualifiedName();
                int idx = underlyingQName.indexOf(':');
                String rawQName = idx >= 0 ? underlyingQName.substring(idx + 1) : underlyingQName;
                MachineActionIR machineActionIR = irRepository.getActions().get(rawQName);
                if (machineActionIR == null) {
                    yield CodeBlock.of(String.format(EXPR_MACHINE_ACTION_WITH_EVENT, machineName, methodName));
                }

                int paramCount = machineActionIR.getParameters().size();
                if (paramCount == 0) {
                    yield CodeBlock.of(String.format(EXPR_MACHINE_ACTION_NO_ARGS, machineName, methodName));
                } else if (paramCount == 1) {
                    yield CodeBlock.of(String.format(EXPR_MACHINE_ACTION_WITH_EVENT, machineName, methodName));
                } else {
                    throw new IllegalStateException("Machine action has unsupported parameter count (" + paramCount
                            + ") for transition " + transition.getQualifiedName());
                }
            }
            case TransitionActionSendToIR sendToAction -> {
                String receiverName = sendToAction.getTo() != null ? sendToAction.getTo().name() : null;
                MachineMessageRefIR messageRef = sendToAction.getMessage();
                if (receiverName == null || receiverName.isBlank()) {
                    throw new IllegalStateException("Transition " + transition.getQualifiedName()
                            + " has a send action without a receiver machine");
                }
                if (messageRef == null) {
                    throw new IllegalStateException("Transition " + transition.getQualifiedName()
                            + " has a send action without a resolvable message");
                }

                TypeName messageType = typeTable.resolve(messageRef.type());
                yield CodeBlock.of(String.format(EXPR_SEND_TO_PUBLISH, receiverName), messageType);
            }
            default -> throw new IllegalStateException("Unsupported transition action type for transition "
                    + transition.getQualifiedName() + ": " + transitionAction.getClass().getSimpleName());
        };
    }

    private Map<String, String> resolveTransitionGuardMethodNames(List<TransitionIR> transitions) {
        Map<String, String> transitionGuardMethodNamesByQualifiedName = new LinkedHashMap<>();
        Set<String> usedMethodNames = new LinkedHashSet<>();

        for (TransitionIR transition : transitions) {
            String transitionGuardQName = resolveTransitionGuardQName(transition);
            if (transitionGuardQName == null) {
                continue;
            }

            String baseMethodName = cleanName(transitionGuardQName);
            String methodName = uniqueLocalName(baseMethodName, usedMethodNames);
            usedMethodNames.add(methodName);

            transitionGuardMethodNamesByQualifiedName.put(transitionGuardQName, methodName);
        }

        return transitionGuardMethodNamesByQualifiedName;
    }

    private String resolveTransitionGuardExpression(TransitionIR transition,
                                                    Map<String, String> transitionGuardExpressionsByGuardQName) {
        if (transition == null) {
            return ALWAYS_TRUE_GUARD;
        }

        String transitionGuardQName = resolveTransitionGuardQName(transition);
        if (transitionGuardQName == null) {
            return ALWAYS_TRUE_GUARD;
        }

        String transitionGuardExpression = transitionGuardExpressionsByGuardQName
                .get(transitionGuardQName);
        if (transitionGuardExpression == null) {
            throw new IllegalStateException("Transition " + transition.getQualifiedName()
                    + " has no generated guard mapping");
        }

        return transitionGuardExpression;
    }

    private String resolveTransitionGuardQName(TransitionIR transition) {
        if (transition == null || transition.getGuard() == null) {
            return null;
        }

        String guardQName = transition.getGuard().qName();
        if (guardQName == null || guardQName.isBlank()) {
            throw new IllegalStateException("Missing transition guard qualified name for transition "
                    + transition.getQualifiedName());
        }

        return guardQName;
    }

    private String cleanName(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            throw new IllegalStateException("Candidate parameter is empty.");
        }

        String cleaned = candidate.replaceAll("\\W", "_");

        if (Character.isUpperCase(cleaned.charAt(0))) {
            cleaned = Character.toLowerCase(cleaned.charAt(0)) + cleaned.substring(1);
        }

        return cleaned;
    }

    private String uniqueLocalName(String baseName, Iterable<String> usedNames) {
        Set<String> names = new LinkedHashSet<>();
        for (String usedName : usedNames) {
            names.add(usedName);
        }

        String candidate = baseName;
        int suffix = 2;
        while (names.contains(candidate)) {
            candidate = baseName + suffix;
            suffix++;
        }
        return candidate;
    }

    private MachineActionIR resolveActionIR(Ref<MachineActionIR> actionRef) {
        if (actionRef == null || actionRef.qName() == null || actionRef.qName().isBlank()) {
            return null;
        }

        // direct lookup
        String qName = actionRef.qName();
        MachineActionIR machineActionIR = irRepository.getActions().get(qName);
        if (machineActionIR != null) {
            return machineActionIR;
        }

        // try to strip a possible prefix like "machine:qualifiedName"
        int idx = qName.indexOf(':');
        if (idx >= 0 && idx + 1 < qName.length()) {
            String raw = qName.substring(idx + 1);
            machineActionIR = irRepository.getActions().get(raw);
            if (machineActionIR != null) {
                return machineActionIR;
            }
        }

        // fallback: try last segment after '.' or '::'
        int lastDot = qName.lastIndexOf('.');
        int lastSep = qName.lastIndexOf("::");
        int last = Math.max(lastDot, lastSep);
        if (last >= 0 && last + 1 < qName.length()) {
            String lastSegment = qName.substring(last + 1);
            machineActionIR = irRepository.getActions().get(lastSegment);
            if (machineActionIR != null) {
                return machineActionIR;
            }
        }

        return null;
    }

    private boolean hasCurrentCommand(MachineIR machine) {
        return machine.getAttributes().stream().anyMatch(attribute -> CURRENT_COMMAND_ATTR.equals(attribute.getName()));
    }

    private List<TransitionTriggerWhenIR> resolveAcceptWhenTriggersForMachine(MachineIR machine) {
        Map<String, TransitionTriggerWhenIR> acceptWhenTriggers = new LinkedHashMap<>();

        for (TransitionTriggerIR trigger : irRepository.getTriggers().values()) {
            if (trigger instanceof TransitionTriggerWhenIR triggerWhen) {
                MachineRefIR machineRef = triggerWhen.getAssociatedMachine();
                if (machineRef != null
                        && machineRef.qualifiedName().equals(machine.getQualifiedName())) {
                    acceptWhenTriggers.putIfAbsent(triggerWhen.getQualifiedName(), triggerWhen);
                }
            }
        }

        return new ArrayList<>(acceptWhenTriggers.values());
    }

    private boolean hasMessage(String messageName) {
        return irRepository.getMessages().values().stream().anyMatch(message -> messageName.equals(message.getName()));
    }

    private TypeName findCurrentCommandType(MachineIR machine) {
        return machine.getAttributes().stream()
                .filter(attribute -> CURRENT_COMMAND_ATTR.equals(attribute.getName()))
                .findFirst()
                .map(attribute -> typeTable.resolve(attribute.getType()))
                .orElse(TypeName.get(Object.class));
    }
}