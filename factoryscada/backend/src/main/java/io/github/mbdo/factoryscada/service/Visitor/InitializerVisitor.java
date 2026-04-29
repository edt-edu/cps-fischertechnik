package io.github.mbdo.factoryscada.service.Visitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.github.mbdo.factoryscada.domains.mission.dsl.dtos.EntryNode_dto;
import io.github.mbdo.factoryscada.domains.mission.dsl.dtos.Fork_dto;
import io.github.mbdo.factoryscada.domains.mission.dsl.dtos.Join_dto;
import io.github.mbdo.factoryscada.domains.mission.dsl.dtos.Node_dto;
import io.github.mbdo.factoryscada.domains.mission.dsl.dtos.RawMachineCommand_dto;
import io.github.mbdo.factoryscada.domains.mission.dsl.dtos.WaitAction_dto;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InitializerVisitor extends Visitor {
    /*
     * This visitor is used to initialized the nodes after their creation.
     * The goal is to set the outputs as references to node,
     * and to make verifications about the validity of the missions graph.
     */
    private final Map<String, Node_dto> idToNodeMap;

    public InitializerVisitor(List<Node_dto> nodes) {
        Map<String, Node_dto> IdtoNode = new HashMap<>();
    
        int numberEntryNode = 0;

        List<String> outputs = new ArrayList<>();

        // This first pass is to :
        //          - verify unicity of Ids
        //          - count number of entry
        //          - list all outputs of nodes
        //          - verify number of output for each nodes
        for (Node_dto node : nodes) {
            String id = node.getId();
            if (IdtoNode.containsKey(id)) {
                String error = String.format("Duplicate node ID detected: '%s'.", id);
                log.error(error);
                throw new IllegalArgumentException(error);
            }
            IdtoNode.put(id, node);

            if (node instanceof EntryNode_dto) {
                numberEntryNode ++;
            }

            outputs.addAll(node.getOutputs());

            if (node.getOutputs().size()>1 && !(node instanceof Fork_dto)){
                String warning = String.format("Multiple output detected, should be unique for non-fork node: '%s'.", id);
                log.warn(warning);
            }
        }

        this.idToNodeMap = IdtoNode;

        if (numberEntryNode == 0) {
            String error = "Mission graph must contain one EntryNode, none were found.";
            log.error(error);
            throw new IllegalStateException(error);
        } else if (numberEntryNode > 1){
            String error = "Mission graph must contain only one EntryNode, multiple were found.";
            log.error(error);
            throw new IllegalStateException(error);
        }

        // The second pass is to :
        //      - initialize the number of inputs for join nodes
        //      - verifiy the existence of nodes outputs
        for (Node_dto node : nodes) {
            if (node instanceof Join_dto joinNode) {
                joinNode.setNumberInputs(Collections.frequency(outputs, joinNode.getId()));
            }

            for (String output : node.getOutputs()){
                if (!IdtoNode.containsKey(output)){
                    String error = String.format("The specified output ID cannot be found: '%s'.", output);
                    log.error(error);
                    throw new IllegalArgumentException(error);
                }
            }
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
