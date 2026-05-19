package fr.inria.mbdo.mission.generators;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import fr.inria.mbdo.mission.ir.ActionIR;
import fr.inria.mbdo.mission.ir.AcceptEventExprIR;
import fr.inria.mbdo.mission.ir.ElementIR;
import fr.inria.mbdo.mission.ir.EnumerationIR;
import fr.inria.mbdo.mission.ir.MachineAttributeIR;
import fr.inria.mbdo.mission.ir.MachineIR;
import fr.inria.mbdo.mission.ir.MachineMessageIR;
import fr.inria.mbdo.mission.ir.MachineMissionIR;
import fr.inria.mbdo.mission.ir.MachineRefIR;
import fr.inria.mbdo.mission.ir.Ref;
import fr.inria.mbdo.mission.ir.StateIR;
import fr.inria.mbdo.mission.ir.TransitionIR;
import fr.inria.mbdo.mission.ir.TriggerIR;
import fr.inria.mbdo.mission.runtime.api.AbstractMissionStrategy;
import fr.inria.mbdo.mission.runtime.api.MachineAdapter;
import fr.inria.mbdo.mission.runtime.rtc.def.RuntimeState;
import fr.inria.mbdo.mission.runtime.rtc.def.RuntimeTransition;
import fr.inria.mbdo.mission.runtime.rtc.event.CompletionEvent;
import fr.inria.mbdo.mission.runtime.rtc.event.DomainEvent;
import fr.inria.mbdo.mission.runtime.rtc.event.Event;
import fr.inria.mbdo.mission.runtime.rtc.exec.RuntimeGuards;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.Modifier;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static fr.inria.mbdo.mission.utils.StringUtils.toUpperFirst;

public class JavaTransformer {

    private static final String JAVADOC_FROM = "From $L\n$L";
    private static final String COMMENT_STATE_BOXES = "State boxes hold runtime targets for each state.";
    private static final String COMMENT_ACTIONS = "Action runtime handlers are generated as small lambdas and can be refined later.";
    private static final String COMMENT_TRANSITIONS = "Transitions connect triggers, runtime actions, and next-state targets.";
    private static final String COMMENT_STATES = "States are built from the collected transitions.";
    private static final String COMMENT_STATE_BINDING = "Bind the runtime states into their box references.";
    private static final String COMMENT_INITIAL_STATE = "Build the initial state and connect it to the first concrete target.";
    private static final String COMMENT_SUBSCRIPTIONS = "Subscribe each mission machine to trigger event types used by this mission.";
    private static final String COMMENT_NO_EXPLICIT_DEFAULT = "TODO refine the initial transition: no explicit default transition was defined, so this draft falls back to the first state.";
    private static final String COMMENT_NO_STATES = "TODO refine the initial transition: the mission currently exposes no states.";
    private static final String TRIGGER_IMPLICIT = "implicit";
    private static final String FIELD_STATE = "state";
    private static final String FIELD_TIMESTAMP = "timestamp";
    private static final String FIELD_CORRELATION_ID = "correlationId";
    private static final String FIELD_CAUSATION_ID = "causationId";
    private static final String METHOD_TIMESTAMP = "timestamp";
    private static final String METHOD_CORRELATION_ID = "correlationId";
    private static final String METHOD_CAUSATION_ID = "causationId";
    private static final String ASSIGN_FIELD = "this.$N = $N";
    private static final String RETURN_FIELD = "return $N";
    private static final String EMPTY_RUNTIME_ACTION = "event -> { }";
    private static final TypeName OPTIONAL_UUID = ParameterizedTypeName.get(ClassName.get(Optional.class),
            ClassName.get(UUID.class));

    private final Logger logger = LoggerFactory.getLogger(JavaTransformer.class);
    private final IrRepository irRepository;
    private final TypeTable typeTable;
    private final String packagePrefix;

    public JavaTransformer(IrRepository irRepository, TypeTable typeTable, String packagePrefix) {
        this.irRepository = irRepository;
        this.typeTable = typeTable;
        this.packagePrefix = packagePrefix;
    }

    public Map<String, JavaFile> generate() {
        Map<String, JavaFile> result = new LinkedHashMap<>();
        result.putAll(generateMachinesInterfaces());
        result.putAll(generateEnumerations());
        result.putAll(generateMachineMessages());
        result.putAll(generateMachinesMissionsClasses());
        return result;
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

            if (hasCurrentCommand(machine) && hasMessage("CommandSuccessEventMessage")) {
                methods.add(MethodSpec.methodBuilder("setCommandSuccess")
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(findCurrentCommandType(machine), "command")
                        .build());
            }

            for (ActionIR action : machine.getActions()) {
                MethodSpec.Builder actionBuilder = MethodSpec.methodBuilder(action.getName())
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addJavadoc(JAVADOC_FROM, action.getQualifiedName(), action.getDocumentation());
                for (var parameter : action.getParameters()) {
                    actionBuilder.addParameter(
                            ParameterSpec.builder(typeTable.resolve(parameter.getType()), parameter.getName()).build());
                }
                methods.add(actionBuilder.build());
            }

            interfaceBuilder.addMethods(methods);
            javaFiles.put(machine.getQualifiedName() + "::interface",
                    JavaFile.builder(javaPackageOf(machine), interfaceBuilder.build()).build());
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
                    JavaFile.builder(javaPackageOf(enumeration), enumBuilder.build()).build());
        }

        return javaFiles;
    }

    private Map<String, JavaFile> generateMachineMessages() {
        Map<String, JavaFile> javaFiles = new LinkedHashMap<>();

        for (MachineMessageIR message : irRepository.getMessages().values()) {
            logger.info("Generating message for {}", message.getQualifiedName());
            javaFiles.put(message.getQualifiedName(), buildMessageJavaFile(message));
        }

        return javaFiles;
    }

    private JavaFile buildMessageJavaFile(MachineMessageIR message) {
        TypeSpec.Builder messageBuilder = TypeSpec.classBuilder(message.getName())
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addSuperinterface(DomainEvent.class);

        messageBuilder.addField(FieldSpec.builder(TypeName.get(Instant.class), FIELD_TIMESTAMP, Modifier.PRIVATE,
                Modifier.FINAL).build());
        messageBuilder.addField(FieldSpec.builder(TypeName.get(UUID.class), FIELD_CORRELATION_ID, Modifier.PRIVATE,
                Modifier.FINAL).build());
        messageBuilder.addField(FieldSpec.builder(OPTIONAL_UUID, FIELD_CAUSATION_ID, Modifier.PRIVATE, Modifier.FINAL)
                .build());

        messageBuilder.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(TypeName.get(Instant.class), FIELD_TIMESTAMP)
                .addParameter(TypeName.get(UUID.class), FIELD_CORRELATION_ID)
                .addParameter(OPTIONAL_UUID, FIELD_CAUSATION_ID)
                .addStatement(ASSIGN_FIELD, FIELD_TIMESTAMP, FIELD_TIMESTAMP)
                .addStatement(ASSIGN_FIELD, FIELD_CORRELATION_ID, FIELD_CORRELATION_ID)
                .addStatement(ASSIGN_FIELD, FIELD_CAUSATION_ID, FIELD_CAUSATION_ID)
                .build());

        messageBuilder.addMethod(MethodSpec.methodBuilder(METHOD_TIMESTAMP)
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.get(Instant.class))
                .addStatement(RETURN_FIELD, FIELD_TIMESTAMP)
                .build());
        messageBuilder.addMethod(MethodSpec.methodBuilder(METHOD_CORRELATION_ID)
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.get(UUID.class))
                .addStatement(RETURN_FIELD, FIELD_CORRELATION_ID)
                .build());
        messageBuilder.addMethod(MethodSpec.methodBuilder(METHOD_CAUSATION_ID)
                .addModifiers(Modifier.PUBLIC)
                .returns(OPTIONAL_UUID)
                .addStatement(RETURN_FIELD, FIELD_CAUSATION_ID)
                .build());
        messageBuilder.addMethod(MethodSpec.methodBuilder("now")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ClassName.get(javaPackageOf(message), message.getName()))
                .addStatement("return new $T($T.now(), $T.randomUUID(), $T.empty())",
                        ClassName.get(javaPackageOf(message), message.getName()), Instant.class, UUID.class,
                        Optional.class)
                .build());

        return JavaFile.builder(javaPackageOf(message), messageBuilder.build()).build();
    }

    private Map<String, JavaFile> generateMachinesMissionsClasses() {
        Map<String, JavaFile> javaFiles = new LinkedHashMap<>();

        for (MachineMissionIR mission : irRepository.getMissions().values()) {
            logger.info("Generating mission for {}", mission.getQualifiedName());

            List<StateIR> states = resolveMissionStates(mission);
            List<TransitionIR> transitions = resolveMissionTransitions(mission, states);
            List<ActionIR> actions = resolveMissionActions(mission, transitions);

            TypeSpec.Builder missionBuilder = TypeSpec.classBuilder(mission.getName())
                    .addModifiers(Modifier.PUBLIC)
                    .superclass(AbstractMissionStrategy.class)
                    .addJavadoc(JAVADOC_FROM, mission.getQualifiedName(), mission.getDocumentation())
                    .addField(FieldSpec.builder(ClassName.get(Logger.class), "logger", Modifier.PRIVATE,
                            Modifier.STATIC, Modifier.FINAL)
                            .initializer("$T.getLogger($T.class)", LoggerFactory.class,
                                    ClassName.get(javaPackageOf(mission), mission.getName()))
                            .build());

            for (MachineRefIR machineRef : mission.getMachinesRefs()) {
                missionBuilder.addField(buildMachineRefField(machineRef));
            }

            missionBuilder.addMethod(buildMissionConstructor(mission, states, transitions, actions));
            missionBuilder.addMethod(buildGetNameMethod(mission));
            for (ActionIR action : actions) {
                logger.info("Generating runtime handler for mission action {}", action.getQualifiedName());
                missionBuilder.addMethod(buildActionExecutorMethod(mission, action));
            }

            javaFiles.put(mission.getQualifiedName(),
                    JavaFile.builder(javaPackageOf(mission), missionBuilder.build()).build());
        }

        return javaFiles;
    }

    private MethodSpec buildMissionConstructor(MachineMissionIR mission, List<StateIR> states,
            List<TransitionIR> transitions, List<ActionIR> actions) {
        MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC);

        for (MachineRefIR machineRef : mission.getMachinesRefs()) {
            String machineName = machineRef.name();
            constructorBuilder.addParameter(typeTable.resolve(machineRef.type()), machineName);
            constructorBuilder.addStatement(ASSIGN_FIELD, machineName, machineName);
        }

        constructorBuilder.addCode("\n");
        Map<String, String> stateBoxNames = emitStateBoxBlock(constructorBuilder, states);

        constructorBuilder.addCode("\n");
        Map<String, String> actionEffectNames = emitActionBlock(constructorBuilder, actions);

        constructorBuilder.addCode("\n");
        Map<String, String> transitionNames = emitTransitionBlock(constructorBuilder, transitions, stateBoxNames,
                actionEffectNames);

        constructorBuilder.addCode("\n");
        Map<String, String> stateRuntimeNames = emitStateBlock(constructorBuilder, states, transitionNames);

        constructorBuilder.addCode("\n");
        emitStateBindingBlock(constructorBuilder, states, stateBoxNames, stateRuntimeNames);

        constructorBuilder.addCode("\n");
        emitInitialStateBlock(constructorBuilder, mission, states, stateBoxNames, actionEffectNames);

        constructorBuilder.addCode("\n");
        emitEventSubscriptions(constructorBuilder, mission.getMachinesRefs(), transitions);

        return constructorBuilder.build();
    }

    private Map<String, String> emitStateBoxBlock(MethodSpec.Builder constructorBuilder, List<StateIR> states) {
        constructorBuilder.addComment(COMMENT_STATE_BOXES);
        Map<String, String> stateBoxNames = new LinkedHashMap<>();
        for (StateIR state : states) {
            String stateBoxName = localStateBoxName(state.getName(), stateBoxNames.values());
            stateBoxNames.put(state.getQualifiedName(), stateBoxName);
            constructorBuilder.addStatement("StateBox $N = new StateBox()", stateBoxName);
        }
        return stateBoxNames;
    }

    private Map<String, String> emitActionBlock(MethodSpec.Builder constructorBuilder, List<ActionIR> actions) {
        constructorBuilder.addComment(COMMENT_ACTIONS);
        Map<String, String> actionEffectNames = new LinkedHashMap<>();
        for (ActionIR action : actions) {
            String actionEffectName = localActionEffectName(action.getName(), actionEffectNames.values());
            actionEffectNames.put(action.getQualifiedName(), actionEffectName);
            constructorBuilder.addStatement("RuntimeAction $N = this::$N", actionEffectName, actionEffectName);
        }
        return actionEffectNames;
    }

    private Map<String, String> emitTransitionBlock(MethodSpec.Builder constructorBuilder,
            List<TransitionIR> transitions, Map<String, String> stateBoxNames,
            Map<String, String> actionEffectNames) {
        constructorBuilder.addComment(COMMENT_TRANSITIONS);
        Map<String, String> transitionDeclarations = new LinkedHashMap<>();
        for (TransitionIR transition : transitions) {
            logger.info("Generating transition with trigger {} and target state {}",
                    transition.getTrigger(),
                    transition.getTo());
            StateIR targetState = irRepository.getStates().get(transition.getTo().qName());
            String targetStateName = targetState != null ? targetState.getName() : FIELD_STATE;
            String stateLocalName = localStateName(targetStateName);
            String transitionBaseName = "transitionTo" + toUpperFirst(stateLocalName);
            String transitionName = uniqueLocalName(transitionBaseName, transitionDeclarations.values());
            transitionDeclarations.put(transition.getQualifiedName(), transitionName);

            ResolvedTrigger resolvedTrigger = resolveTrigger(transition);
            if (resolvedTrigger.unresolved()) {
                constructorBuilder.addComment("TODO refine trigger expression '$L' for $L",
                        resolvedTrigger.triggerExpression(), transition.getQualifiedName());
            }

            String targetBoxName = resolveTargetStateBoxName(transition, stateBoxNames);
            String actionExpression = resolveTransitionActionExpression(transition, actionEffectNames);

            if (resolvedTrigger.unresolved()) {
                constructorBuilder.addStatement("$T $N = new $T($T.class, $L, $L, $N::get)",
                        RuntimeTransition.class, transitionName, RuntimeTransition.class,
                        resolvedTrigger.triggerType(), "event -> false", actionExpression, targetBoxName);
            } else {
                constructorBuilder.addStatement("$T $N = new $T($T.class, $T.always(), $L, $N::get)",
                        RuntimeTransition.class, transitionName, RuntimeTransition.class,
                        resolvedTrigger.triggerType(), RuntimeGuards.class, actionExpression, targetBoxName);
            }
        }
        return transitionDeclarations;
    }

    private Map<String, String> emitStateBlock(MethodSpec.Builder constructorBuilder, List<StateIR> states,
            Map<String, String> transitionNames) {
        constructorBuilder.addComment(COMMENT_STATES);
        Map<String, String> stateRuntimeNames = new LinkedHashMap<>();
        for (StateIR state : states) {
            List<String> stateTransitions = new ArrayList<>();
            for (Ref<TransitionIR> transitionRef : state.getTransitions()) {
                TransitionIR transition = irRepository.getTransitions().get(transitionRef.qName());
                if (transition == null) {
                    continue;
                }
                String transitionName = transitionNames.get(transition.getQualifiedName());
                if (transitionName != null) {
                    stateTransitions.add(transitionName);
                }
            }

            String stateVarName = localStateName(state.getName());
            String transitionList = stateTransitions.isEmpty() ? "List.of()"
                    : "List.of(" + String.join(", ", stateTransitions) + ")";
            constructorBuilder.addStatement("$T $N = new $T($S, $L, List.of(), List.of(), false)",
                    RuntimeState.class, stateVarName, RuntimeState.class, state.getName(), transitionList);
            stateRuntimeNames.put(state.getQualifiedName(), stateVarName);
        }
        return stateRuntimeNames;
    }

    private void emitStateBindingBlock(MethodSpec.Builder constructorBuilder, List<StateIR> states,
            Map<String, String> stateBoxNames, Map<String, String> stateRuntimeNames) {
        constructorBuilder.addComment(COMMENT_STATE_BINDING);
        for (StateIR state : states) {
            String stateBoxName = stateBoxNames.get(state.getQualifiedName());
            String stateVarName = stateRuntimeNames.get(state.getQualifiedName());
            if (stateBoxName != null && stateVarName != null) {
                constructorBuilder.addStatement("$N.set($N)", stateBoxName, stateVarName);
            }
        }
    }

    private void emitInitialStateBlock(MethodSpec.Builder constructorBuilder, MachineMissionIR mission,
            List<StateIR> states, Map<String, String> stateBoxNames, Map<String, String> actionEffectNames) {
        constructorBuilder.addComment(COMMENT_INITIAL_STATE);

        TransitionIR defaultTransition = mission.getDefaultTransition() != null
                ? irRepository.getTransitions().get(mission.getDefaultTransition().qName())
                : null;
        String initTargetBoxName;
        String initActionExpression;
        if (defaultTransition != null) {
            initTargetBoxName = resolveTargetStateBoxName(defaultTransition, stateBoxNames);
            initActionExpression = resolveTransitionActionExpression(defaultTransition, actionEffectNames);
        } else if (!states.isEmpty()) {
            StateIR firstState = states.getFirst();
            initTargetBoxName = stateBoxNames.get(firstState.getQualifiedName());
            initActionExpression = EMPTY_RUNTIME_ACTION;
            constructorBuilder.addComment(COMMENT_NO_EXPLICIT_DEFAULT);
        } else {
            initTargetBoxName = null;
            initActionExpression = EMPTY_RUNTIME_ACTION;
            constructorBuilder.addComment(COMMENT_NO_STATES);
        }

        if (initTargetBoxName != null) {
            constructorBuilder.addStatement("$T initToFirst = new $T($T.class, event -> true, $L, $N::get)",
                    RuntimeTransition.class, RuntimeTransition.class, CompletionEvent.class, initActionExpression,
                    initTargetBoxName);
            constructorBuilder.addStatement("$T init = new $T($S, List.of(initToFirst), List.of(), List.of(), false)",
                    RuntimeState.class, RuntimeState.class, "__Init");
        } else {
            constructorBuilder.addStatement("$T init = new $T($S, List.of(), List.of(), List.of(), false)",
                    RuntimeState.class, RuntimeState.class, "__Init");
        }
    }

    private void emitEventSubscriptions(MethodSpec.Builder constructorBuilder, List<MachineRefIR> machineRefs,
            List<TransitionIR> transitions) {
        constructorBuilder.addComment(COMMENT_SUBSCRIPTIONS);
        constructorBuilder.addStatement("this.runtime.setInitialState(init)");

        Map<String, TypeName> triggerTypesByQName = new LinkedHashMap<>();
        for (TransitionIR transition : transitions) {
            ResolvedTrigger resolved = resolveTrigger(transition);
            if (resolved == null || resolved.unresolved()
                    || resolved.triggerTypeQualifiedName() == null
                    || TRIGGER_IMPLICIT.equals(resolved.triggerExpression())) {
                continue;
            }
            triggerTypesByQName.putIfAbsent(resolved.triggerTypeQualifiedName(), resolved.triggerType());
        }

        for (MachineRefIR machineRef : machineRefs) {
            for (Map.Entry<String, TypeName> triggerEntry : triggerTypesByQName.entrySet()) {
                if (isEventRelatedToMachine(triggerEntry.getKey(), machineRef, machineRefs)) {
                    constructorBuilder.addStatement("$N.subscribe($T.class, this::onEvent)",
                            machineRef.name(), triggerEntry.getValue());
                }
            }
        }

        constructorBuilder.addStatement("logger.info($S)", "Mission initialized and subscribed to trigger events");
    }

    private MethodSpec buildGetNameMethod(MachineMissionIR mission) {
        return MethodSpec.methodBuilder("getName")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement("return $S", mission.getName())
                .build();
    }

    private FieldSpec buildMachineRefField(MachineRefIR machineRef) {
        return FieldSpec.builder(typeTable.resolve(machineRef.type()), machineRef.name(), Modifier.PRIVATE,
                Modifier.FINAL).build();
    }

    private MethodSpec buildActionExecutorMethod(MachineMissionIR mission, ActionIR action) {
        String actionMethodName = localActionEffectName(action.getName(), List.of());
        MethodSpec.Builder builder = MethodSpec.methodBuilder(actionMethodName)
                .addModifiers(Modifier.PRIVATE)
                .addParameter(Event.class, "event")
                .addJavadoc(JAVADOC_FROM, action.getQualifiedName(), action.getDocumentation());

        if (!action.getBodyStatements().isEmpty()) {
            builder.addComment("Generated from the SysML action body.");

            // build mapping from extracted receiver token -> mission machine ref name
            Map<String, String> receiverMap = new LinkedHashMap<>();
            for (var machineRef : mission.getMachinesRefs()) {
                String localName = machineRef.name();
                receiverMap.put(localName, localName);
                if (machineRef.type() != null && machineRef.type().qualifiedName() != null) {
                    String q = machineRef.type().qualifiedName();
                    int sep = Math.max(q.lastIndexOf("::"), q.lastIndexOf('.'));
                    String terminal = sep >= 0 ? q.substring(sep + 2) : q;
                    if (terminal != null && !terminal.isBlank()) {
                        receiverMap.put(terminal, localName);
                    }
                }
            }

            for (String bodyStatement : action.getBodyStatements()) {
                String rendered = bodyStatement;
                // replace leading receiver token if we can map it
                String trimmed = rendered.trim();
                int dot = trimmed.indexOf('.');
                if (dot > 0) {
                    String receiver = trimmed.substring(0, dot);
                    String mapped = receiverMap.get(receiver);
                    if (mapped != null && !mapped.equals(receiver)) {
                        rendered = rendered.replaceFirst("\\b" + receiver + "\\b", mapped);
                    }
                }
                builder.addStatement("$L", rendered);
            }
        } else {
            builder.addComment("TODO refine mission action wiring for $L", action.getQualifiedName());
            builder.addComment(
                    "TODO the action body is still unresolved, so this draft keeps the runtime action as a no-op.");
        }

        return builder.build();
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

    private List<ActionIR> resolveMissionActions(MachineMissionIR mission, List<TransitionIR> transitions) {
        Map<String, ActionIR> actions = new LinkedHashMap<>();

        for (Ref<ActionIR> actionRef : mission.getCustomActions()) {
            ActionIR action = irRepository.getActions().get(actionRef.qName());
            if (action != null) {
                actions.put(action.getQualifiedName(), action);
            }
        }

        for (TransitionIR transition : transitions) {
            if (transition.getAction() == null) {
                continue;
            }
            ActionIR action = irRepository.getActions().get(transition.getAction().qName());
            if (action != null) {
                actions.putIfAbsent(action.getQualifiedName(), action);
            }
        }

        return new ArrayList<>(actions.values());
    }

    private ResolvedTrigger resolveTrigger(TransitionIR transition) {
        if (transition.getTrigger() == null) {
            return new ResolvedTrigger(ClassName.get(CompletionEvent.class), false, TRIGGER_IMPLICIT, null);
        }

        TriggerIR trigger = irRepository.getTriggers().get(transition.getTrigger().qName());
        if (trigger == null || trigger.getExpr() == null) {
            return new ResolvedTrigger(ClassName.get(CompletionEvent.class), false, TRIGGER_IMPLICIT, null);
        }

        var acceptExpr = irRepository.getAcceptExprs().get(trigger.getExpr().qName());
        if (acceptExpr instanceof AcceptEventExprIR acceptEventExprIR) {
            if (acceptEventExprIR.getTypeRef() != null && acceptEventExprIR.getTypeRef().qualifiedName() != null) {
                String triggerTypeQName = acceptEventExprIR.getTypeRef().qualifiedName();
                MachineMessageIR message = irRepository.getMessages().get(triggerTypeQName);
                if (message != null) {
                    return new ResolvedTrigger(
                            ClassName.get(javaPackageOf(message), message.getName()),
                            false,
                            triggerTypeQName,
                            triggerTypeQName);
                }
            }
            return new ResolvedTrigger(ClassName.get(DomainEvent.class), true, acceptExpr.getQualifiedName(), null);
        }

        return new ResolvedTrigger(ClassName.get(DomainEvent.class), true,
                trigger.getExpr() != null ? trigger.getExpr().qName() : "unknown",
                null);
    }

    private boolean isEventRelatedToMachine(String eventQName, MachineRefIR machineRef, List<MachineRefIR> allRefs) {
        String eventDomain = extractDomainRoot(eventQName);
        String machineDomain = extractDomainRoot(machineRef.type() != null ? machineRef.type().qualifiedName() : null);
        if (eventDomain != null && machineDomain != null && eventDomain.equals(machineDomain)) {
            return true;
        }

        // If no machine can be mapped by domain, subscribe all mission machines to avoid missing events.
        boolean hasAnyMappedMachine = false;
        for (MachineRefIR ref : allRefs) {
            String refDomain = extractDomainRoot(ref.type() != null ? ref.type().qualifiedName() : null);
            if (eventDomain != null && eventDomain.equals(refDomain)) {
                hasAnyMappedMachine = true;
                break;
            }
        }
        return !hasAnyMappedMachine;
    }

    private String extractDomainRoot(String qualifiedName) {
        if (qualifiedName == null || qualifiedName.isBlank()) {
            return null;
        }
        int sep = qualifiedName.indexOf("::");
        if (sep > 0) {
            return qualifiedName.substring(0, sep);
        }
        int dot = qualifiedName.indexOf('.');
        if (dot > 0) {
            return qualifiedName.substring(0, dot);
        }
        return qualifiedName;
    }

    private String resolveTransitionActionExpression(TransitionIR transition, Map<String, String> actionEffectNames) {
        if (transition.getAction() == null) {
            return EMPTY_RUNTIME_ACTION;
        }

        String actionName = actionEffectNames.get(transition.getAction().qName());
        if (actionName == null) {
            return EMPTY_RUNTIME_ACTION;
        }

        return actionName;
    }

    private String resolveTargetStateBoxName(TransitionIR transition, Map<String, String> stateBoxNames) {
        return resolveTargetStateBoxName(transition, stateBoxNames, List.of());
    }

    private String resolveTargetStateBoxName(TransitionIR transition, Map<String, String> stateBoxNames,
            List<StateIR> states) {
        if (transition.getTo() != null) {
            String resolved = stateBoxNames.get(transition.getTo().qName());
            if (resolved != null) {
                return resolved;
            }
        }

        if (!states.isEmpty()) {
            return stateBoxNames.get(states.getFirst().getQualifiedName());
        }

        return null;
    }

    private String localStateName(String stateName) {
        return localName(stateName, FIELD_STATE);
    }

    private String localStateBoxName(String stateName, Iterable<String> usedNames) {
        return uniqueLocalName(localName(stateName, FIELD_STATE) + "Ref", usedNames);
    }

    private String localActionEffectName(String actionName, Iterable<String> usedNames) {
        return uniqueLocalName(localName(actionName, "action") + "Effect", usedNames);
    }

    private String localName(String candidate, String fallback) {
        if (candidate == null || candidate.isBlank()) {
            return fallback;
        }

        String cleaned = candidate.replaceAll("\\W", "_");
        if (cleaned.isBlank()) {
            return fallback;
        }

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

        private record ResolvedTrigger(TypeName triggerType, boolean unresolved, String triggerExpression,
            String triggerTypeQualifiedName) {
    }

    private boolean hasCurrentCommand(MachineIR machine) {
        return machine.getAttributes().stream().anyMatch(attribute -> "currentCommand".equals(attribute.getName()));
    }

    private boolean hasMessage(String messageName) {
        return irRepository.getMessages().values().stream().anyMatch(message -> messageName.equals(message.getName()));
    }

    private TypeName findCurrentCommandType(MachineIR machine) {
        return machine.getAttributes().stream()
                .filter(attribute -> "currentCommand".equals(attribute.getName()))
                .findFirst()
                .map(attribute -> typeTable.resolve(attribute.getType()))
                .orElse(TypeName.get(Object.class));
    }

    private String javaPackageOf(ElementIR element) {
        return packagePrefix + "." + element.getJavaPackage();
    }
}