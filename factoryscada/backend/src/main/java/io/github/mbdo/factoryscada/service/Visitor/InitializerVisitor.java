package io.github.mbdo.factoryscada.service.Visitor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

import io.github.mbdo.factoryscada.domains.mission.dtos.Node_dto;
import io.github.mbdo.factoryscada.domains.mission.dtos.EntryNode_dto;
import io.github.mbdo.factoryscada.domains.mission.dtos.Fork_dto;
import io.github.mbdo.factoryscada.domains.mission.dtos.Join_dto;
import io.github.mbdo.factoryscada.domains.mission.dtos.RawMachineCommand_dto;
import io.github.mbdo.factoryscada.domains.mission.dtos.WaitAction_dto;

@Slf4j
public class InitializerVisitor extends Visitor {
    /*
     * This visitor is used to initialized the nodes after their creation.
     * The goal is to set the outputs as references to node,
     * and to make verifications about the validity of the missions graph.
     */
    private final Map<String, Node_dto> idToNodeMap;

    public InitializerVisitor(List<Node_dto> nodes) {
        Map<String, Node_dto> tempMap = new HashMap<>();
    
        int numberEntryNode = 0;

        for (Node_dto node : nodes) {
            String id = node.getId();
            if (tempMap.containsKey(id)) {
                String error = String.format("Duplicate node ID detected: '%s'.", id);
                log.error(error);
                throw new IllegalArgumentException(error);
            }
            tempMap.put(id, node);

            if (node instanceof EntryNode_dto) {
                numberEntryNode ++;
            }
        }

        this.idToNodeMap = tempMap;

        if (numberEntryNode == 0) {
            String error = "Mission graph must contain one EntryNode, none were found.";
            log.error(error);
            throw new IllegalStateException(error);
        } else if (numberEntryNode > 1){
            String error = "Mission graph must contain only one EntryNode, multiple were found.";
            log.error(error);
            throw new IllegalStateException(error);
        }
    }

    private void resolve(Node_dto node) {
        List<String> outputs = node.getOutputs();
        if (outputs == null)
            return;

        for (String outputId : outputs) {
            Node_dto target = idToNodeMap.get(outputId);
            if (target == null) {
                String error = String.format("Node '%s' references unknown output node ID '%s'.", node.getId(),
                        outputId);
                log.error(error);
                throw new IllegalStateException(error);
            }
        }

        List<Node_dto> resolvedOutputs = outputs.stream()
                .map(idToNodeMap::get)
                .collect(Collectors.toList());

        node.setOutputNodes(resolvedOutputs);
    }

    public void visit(Fork_dto node) {
        resolve(node);
    }

    public void visit(Join_dto node) {
        resolve(node);
    }

    public void visit(RawMachineCommand_dto node) {
        resolve(node);
    }

    public void visit(WaitAction_dto node) {
        resolve(node);
    }

    public void visit(EntryNode_dto node) {
        resolve(node);
    }

}
