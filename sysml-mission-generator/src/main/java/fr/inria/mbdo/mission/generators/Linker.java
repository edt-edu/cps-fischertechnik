package fr.inria.mbdo.mission.generators;

import com.palantir.javapoet.ClassName;
import fr.inria.mbdo.mission.ir.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Linker {
    private static final Logger logger = LoggerFactory.getLogger(Linker.class);

    public TypeTable link(IrRepository repository, String packagePrefix) {
        TypeTable typeTable = new TypeTable();

        repository.getEnumerations().values().forEach(enumIr -> typeTable.register(enumIr.getQualifiedName(),
                ClassName.get(packagePrefix + "." + enumIr.getJavaPackage(), enumIr.getName())));

        repository.getMachines().values().forEach(machineIr -> typeTable.register(machineIr.getQualifiedName(),
                ClassName.get(packagePrefix + "." + machineIr.getJavaPackage(), machineIr.getName())));

        repository.getMessages().values().forEach(messageIr -> typeTable.register(messageIr.getQualifiedName(),
                ClassName.get(packagePrefix + "." + messageIr.getJavaPackage(), messageIr.getName())));

        repository.getMissions().values().forEach(mission -> {
            for (Ref<StateIR> stateRef : mission.getStates()) {
                if (!repository.getStates().containsKey(stateRef.qName())) {
                    logger.warn("Mission references unknown state: {}", stateRef.qName());
                }
            }
            for (MachineRefIR machineRef : mission.getMachinesRefs()) {
                if (machineRef.type() == null || machineRef.type().qualifiedName() == null) {
                    logger.warn("Mission has machine reference with missing type: {}", mission.getQualifiedName());
                    continue;
                }
                if (!repository.getMachines().containsKey(machineRef.type().qualifiedName())) {
                    logger.warn("Mission references unknown machine: {}", machineRef.type().qualifiedName());
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
            if (transition.getTo() != null && !repository.getStates().containsKey(transition.getTo().qName())) {
                logger.warn("Transition target state not found: {}", transition.getTo().qName());
            }
            if (transition.getTrigger() != null
                    && !repository.getTriggers().containsKey(transition.getTrigger().qName())) {
                logger.warn("Transition trigger not found: {}", transition.getTrigger().qName());
            }
        });

        return typeTable;
    }
}
