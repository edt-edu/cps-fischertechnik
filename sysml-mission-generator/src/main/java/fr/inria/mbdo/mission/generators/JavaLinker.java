package fr.inria.mbdo.mission.generators;

import com.palantir.javapoet.ClassName;
import fr.inria.mbdo.mission.ir.*;
import fr.inria.mbdo.mission.utils.SysmlToJavaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Centralizes the mapping of IR elements to their Java type representations.
 * <p>
 * The linker registers every IR element that produces a generated Java type
 * into the
 * {@link TypeTable}, so that downstream consumers (e.g.
 * {@code JavaTransformer}) can
 * resolve any qualified name to a {@link com.palantir.javapoet.TypeName} or
 * Java package
 * without ad-hoc resolution logic.
 */
public class JavaLinker {
    private static final Logger logger = LoggerFactory.getLogger(JavaLinker.class);

    private static final String CUSTOM_EVENTS_SUFFIX = "::CustomEvents";

    public TypeTable link(IrRepository repository, String packagePrefix) {
        TypeTable typeTable = new TypeTable();

        // Register enumerations
        repository.getEnumerations().values().forEach(enumIr -> {
            ClassName className = ClassName.get(
                    SysmlToJavaUtils.javaPackage(packagePrefix, enumIr.getNamespace()),
                    SysmlToJavaUtils.javaClass(enumIr.getName()));
            typeTable.register(enumIr.getQualifiedName(), className);
        });

        // Register machines (adapter interfaces)
        repository.getMachines().values().forEach(machineIr -> {
            ClassName className = ClassName.get(
                    SysmlToJavaUtils.javaPackage(packagePrefix, machineIr.getNamespace()),
                    SysmlToJavaUtils.javaClass(machineIr.getName()));
            typeTable.register(machineIr.getQualifiedName(), className);
        });

        // Register event messages
        repository.getMessages().values().forEach(messageIr -> {
            ClassName className = ClassName.get(
                    SysmlToJavaUtils.javaPackage(packagePrefix, messageIr.getNamespace()),
                    SysmlToJavaUtils.javaClass(messageIr.getName()));
            typeTable.register(messageIr.getQualifiedName(), className);
        });

        // Register missions (state machine strategy classes)
        repository.getMissions().values().forEach(mission -> {
            ClassName className = ClassName.get(
                    SysmlToJavaUtils.javaPackage(packagePrefix, mission.getNamespace() + "::" + mission.getName()),
                    SysmlToJavaUtils.javaClass(mission.getName()));
            typeTable.register(mission.getQualifiedName(), className);
        });

        // Register accept-when trigger events
        repository.getTriggers().values().forEach(trigger -> {
            if (trigger instanceof TransitionTriggerWhenIR ttw) {
                ClassName className = ClassName.get(SysmlToJavaUtils.javaPackage(packagePrefix, ttw.getNamespace() + CUSTOM_EVENTS_SUFFIX), SysmlToJavaUtils.javaClass(ttw.getName()));
                typeTable.register(ttw.getQualifiedName(), className);
            }
        });

        // Register machine actions (so they can be resolved by qualified name)
        repository.getActions().values().forEach(actionIr -> {
            ClassName className = ClassName.get(
                    SysmlToJavaUtils.javaPackage(packagePrefix, actionIr.getNamespace()),
                    SysmlToJavaUtils.javaClass(actionIr.getName()));
            typeTable.register(actionIr.getQualifiedName(), className);
        });

        // Validate cross-references
        validateReferences(repository);

        return typeTable;
    }

    private void validateReferences(IrRepository repository) {
        repository.getMissions().values().forEach(mission -> {
            for (Ref<StateIR> stateRef : mission.getStates()) {
                if (!repository.getStates().containsKey(stateRef.qName())) {
                    logger.warn("Mission references unknown state: {}", stateRef.qName());
                }
            }
            for (MachineRefIR machineRef : mission.getMachinesRefs()) {
                if (machineRef.type() == null || machineRef.type().qualifiedName() == null) {
                    logger.warn("Mission has machine reference with missing type: {}",
                            mission.getQualifiedName());
                    continue;
                }
                if (!repository.getMachines().containsKey(machineRef.type().qualifiedName())) {
                    logger.warn("Mission references unknown machine: {}",
                            machineRef.type().qualifiedName());
                }
            }
        });

        repository.getStates().values().forEach(state -> {
            for (Ref<TransitionIR> transitionRef : state.getTransitions()) {
                if (!repository.getTransitions().containsKey(transitionRef.qName())) {
                    logger.warn("State references unknown transition: {}", transitionRef.qName());
                }
            }
        });

        repository.getTransitions().values().forEach(transition -> {
            if (transition.getToState() != null
                    && !repository.getStates().containsKey(transition.getToState().qName())) {
                logger.warn("Transition target state not found: {}", transition.getToState().qName());
            }
            if (transition.getTrigger() != null
                    && !repository.getTriggers().containsKey(transition.getTrigger().qName())) {
                logger.warn("Transition trigger not found: {}", transition.getTrigger().qName());
            }
        });
    }
}
