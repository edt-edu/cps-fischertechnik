package fr.inria.mbdo.mission.generators;

import com.palantir.javapoet.ClassName;
import fr.inria.mbdo.mission.ir.*;

public class Linker {
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
                    throw new IllegalStateException("Mission references unknown state: " + stateRef.qName());
                }
            }
            for (MachineRefIR machineRef : mission.getMachinesRefs()) {
                if (machineRef.type() == null || machineRef.type().qualifiedName() == null) {
                    throw new IllegalStateException(
                            "Mission has machine reference with missing type: " + mission.getQualifiedName());
                }
                if (!repository.getMachines().containsKey(machineRef.type().qualifiedName())) {
                    throw new IllegalStateException(
                            "Mission references unknown machine: " + machineRef.type().qualifiedName());
                }
            }
        });

        repository.getStates().values().forEach(state -> {
            for (Ref<TransitionIR> transitionRef : state.getTransitions()) {
                if (!repository.getTransitions().containsKey(transitionRef.qName())) {
                    throw new IllegalStateException("State references unknown transition: " + transitionRef.qName());
                }
            }
        });

        repository.getTransitions().values().forEach(transition -> {
            if (transition.getTo() != null && !repository.getStates().containsKey(transition.getTo().qName())) {
                throw new IllegalStateException("Transition target state not found: " + transition.getTo().qName());
            }
            if (transition.getTrigger() != null
                    && !repository.getTriggers().containsKey(transition.getTrigger().qName())) {
                throw new IllegalStateException("Transition trigger not found: " + transition.getTrigger().qName());
            }
        });

        return typeTable;
    }
}
