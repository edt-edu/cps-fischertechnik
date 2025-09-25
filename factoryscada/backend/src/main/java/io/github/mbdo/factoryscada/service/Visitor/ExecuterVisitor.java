package io.github.mbdo.factoryscada.service.Visitor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import io.github.mbdo.factoryscada.domains.mission.dtos.Node_dto;
import io.github.mbdo.factoryscada.core.AbstractMachine;
import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.core.dtos.CommandMessage;
import io.github.mbdo.factoryscada.domain.CommandStatus;
import io.github.mbdo.factoryscada.domains.mission.dtos.EntryNode_dto;
import io.github.mbdo.factoryscada.domains.mission.dtos.FactoryMissionsParallelized_dto;
import io.github.mbdo.factoryscada.domains.mission.dtos.MissionParallelized_dto;
import io.github.mbdo.factoryscada.domains.mission.dtos.Fork_dto;
import io.github.mbdo.factoryscada.domains.mission.dtos.Join_dto;
import io.github.mbdo.factoryscada.domains.mission.dtos.RawMachineCommand_dto;
import io.github.mbdo.factoryscada.domains.mission.dtos.WaitAction_dto;
import io.github.mbdo.factoryscada.service.FactoryScada;
import io.github.mbdo.factoryscada.socket.Protocol;
import io.github.mbdo.factoryscada.socket.exception.ProtocolException;

@Slf4j
@Getter
public class ExecuterVisitor extends Visitor {
    /*
     * This visitor is used to execute a mission.
     */

    private Map<String, Node_dto> missionIdToEntryNode = new HashMap<>();
    private FactoryMissionsParallelized_dto factoryMissions;
    private FactoryScada factoryScada;
    private List<Node_dto> isCurrentlyVisiting = new CopyOnWriteArrayList<>();
    private Map<String, String> nodeIdToCommandId = new HashMap<>();

    private Map<String, Integer> joinIdNumberInputs = new HashMap<>();

    private String actualMissionName;

    private final SimpMessagingTemplate template;

    public ExecuterVisitor(FactoryScada factoryScada, FactoryMissionsParallelized_dto factoryMissions,
            SimpMessagingTemplate template) {
        this.factoryScada = factoryScada;
        this.factoryMissions = factoryMissions;
        this.template = template;

        // Store the entry node for each missions
        for (MissionParallelized_dto mission : factoryMissions.getMissions()) {
            for (Node_dto node : mission.getNodes()) {
                if (node instanceof EntryNode_dto) {
                    String missionName = mission.getName();
                    missionIdToEntryNode.put(missionName, node);
                    break;
                }
            }
        }

    }

    public String startMission(String missionName) {
        if (missionIdToEntryNode.containsKey(missionName)) {
            // Clear HashMaps from previous missions
            nodeIdToCommandId.clear();
            joinIdNumberInputs.clear();
            actualMissionName = missionName;
            missionIdToEntryNode.get(missionName).accept(this);
            return "Mission " + missionName + " started";
        } else {
            log.error("no mission {} found in the configuration", missionName);
            return "Mission " + missionName + " not found";
        }
    }

    public String stopMission() {
        if (!isCurrentlyVisiting.isEmpty()) {
            for (Node_dto node : isCurrentlyVisiting) {
                if (node instanceof RawMachineCommand_dto rawMachineCommand_dto) {
                    // look for the machine that is currently running a command in this node
                    String machineName = this.getMachineNameFromJsonCommand(rawMachineCommand_dto.getPlaceholder());
                    AbstractMachine machine = factoryScada.getFactoryScadaInstance().machines().get(machineName);
                    // TODO send stop to the machine
                    CommandMessage stopMessage = new CommandMessage();
                    stopMessage.setParameters(new ArrayList<>());
                    stopMessage.setJsonType("COMMAND");
                    stopMessage.setName("STOP");
                    stopMessage.setType(this.getMachineTypeFromJsonCommand(rawMachineCommand_dto.getPlaceholder()));
                    stopMessage.setOutputId(Long.toString(factoryScada.getCommandIdGenerator().generateId()));
                    GenericMachineCommandDTO<?> commandDTO = new GenericMachineCommandDTO<>(machineName,
                            String.valueOf(Instant.now().toEpochMilli()),
                            stopMessage);
                    ObjectMapper mapper = new ObjectMapper();
                    Protocol protocol = machine.getProtocol();
                    try {
                        String jsonString = mapper.writeValueAsString(commandDTO);
                        // send to plc socket
                        protocol.send(jsonString);

                        // add the command to log list
                        factoryScada.addLogsForFrontend(jsonString.replaceAll("\\[[^\\]]*\\]", "[]"));

                        // update storage in backend
                        CommandStatus status = this.factoryScada.getMachineLastCommandStatusMap()
                                .getOrDefault(machineName, new CommandStatus());
                        status.setCurrentCommandTimestamp(commandDTO.getTimestamp());
                        status.setCurrentCommandName(commandDTO.getMessage().getName());
                        status.setCurrentCommandId(commandDTO.getMessage().getOutputId());
                        status.setCurrentCommandRawJSON(jsonString);
                        this.factoryScada.getMachineLastCommandStatusMap().put(machineName, status);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    } catch (ProtocolException e) {
                        throw new RuntimeException(e);
                    }
                }
                isCurrentlyVisiting.remove(node);
            }
            template.convertAndSend("/topic/actual-command-executing", isCurrentlyVisiting);
            return "Mission " + this.actualMissionName + " stopped";
        } else {
            return "";
        }

    }

    public void receivedMachineCommandFeedback(CommandStatus commandStatus) {
        // if isRunning a mission, wait for the current command feedback
        if (!isCurrentlyVisiting.isEmpty()) {
            for (Node_dto node : isCurrentlyVisiting) {
                if (commandStatus.getCurrentCommandId().equals(nodeIdToCommandId.get(node.getId()))) {
                    switch ((commandStatus.getCommandFeedbackStatus().toUpperCase())) {
                        case "DONE":
                            // if success , send next command, or notify end of mission if this is the last
                            if (!node.getOutputs().isEmpty()) {
                                log.info("Processing command {} of Mission {}", node.getOutputNodes().get(0).getId(),
                                        this.actualMissionName);
                                node.getOutputNodes().get(0).accept(this);
                            } else {
                                log.info("Last command of Mission {} has finished", this.actualMissionName);
                            }
                            isCurrentlyVisiting.remove(node);
                            break;
                        case "INTERRUPTED":
                        case "ABORTED_ERROR":
                        case "ABORTED_TIMEOUT":
                            // if aborted, notify mission aborted
                            this.stopMission();
                            break;
                        default:
                            // ignore other internal command feedback
                            break;
                    }
                }
            }
            template.convertAndSend("/topic/actual-command-executing", isCurrentlyVisiting);
        }
    }

    public void visit(Fork_dto node) {
        log.info("Visiting fork : {}", node.getId());
        for (Node_dto outputNodes : node.getOutputNodes()) {
            outputNodes.accept(this);
        }
    }

    public void visit(Join_dto node) {
        log.info("Visiting join : {}", node.getId());
        if (joinIdNumberInputs.containsKey(node.getId())) {
            String id = node.getId();
            joinIdNumberInputs.put(id, joinIdNumberInputs.get(id) + 1);
        } else {
            joinIdNumberInputs.put(node.getId(), 1);
        }

        if (joinIdNumberInputs.get(node.getId()) >= node.getNumberInputs()) {
            if (node.getOutputNodes().isEmpty()) {
                log.info("Last command of Mission {} has finished", this.actualMissionName);
            } else {
                node.getOutputNodes().get(0).accept(this);
            }
        }
    }

    public void visit(RawMachineCommand_dto node) {
        log.info("Visiting RMC : {}", node.getId());
        isCurrentlyVisiting.add(node);
        template.convertAndSend("/topic/actual-command-executing", isCurrentlyVisiting);
        sendMissionCommand(node);
    }

    public void visit(WaitAction_dto node) {
        log.info("Visiting WA : {}", node.getId());
        isCurrentlyVisiting.add(node);

        try {
            Thread.sleep(node.getTime() * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Thread was interrupted during sleep.");
        }

        isCurrentlyVisiting.remove(node);
        node.getOutputNodes().get(0).accept(this);
    }

    public void visit(EntryNode_dto node) {
        log.info("Visiting EntNo : {}", node.getId());
        node.getOutputNodes().get(0).accept(this);
    }

    protected String getMachineNameFromJsonCommand(String jsonCommandString) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            GenericMachineCommandDTO<?> machineCommand = mapper.readValue(jsonCommandString,
                    GenericMachineCommandDTO.class);

            return machineCommand.getTopicName();
        } catch (JsonProcessingException e) {
            log.error("Error parsing feedback: {}", e.getMessage());
        }
        return null;
    }

    protected String getMachineTypeFromJsonCommand(String jsonCommandString) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            GenericMachineCommandDTO<?> machineCommand = mapper.readValue(jsonCommandString,
                    GenericMachineCommandDTO.class);

            return machineCommand.getMessage().getType();
        } catch (JsonProcessingException e) {
            log.error("Error parsing feedback: {}", e.getMessage());
        }
        return null;
    }

    protected GenericMachineCommandDTO<?> genericMachineCommand(String jsonCommandString)
            throws JsonProcessingException {
        ObjectMapper mapper = JsonMapper.builder().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .build();
        TypeReference<GenericMachineCommandDTO<?>> typeRef = new TypeReference<GenericMachineCommandDTO<?>>() {
        };
        return mapper.readValue(jsonCommandString, typeRef);
    }

    protected void sendMissionCommand(RawMachineCommand_dto node) {
        /*
         * This function take a rawMachineCommand node and execute it's command
         */
        String command = node.getPlaceholder();
        String machineName = this.getMachineNameFromJsonCommand(command);
        AbstractMachine machine = factoryScada.getFactoryScadaInstance().machines().get(machineName);
        if (machine != null) {
            try {
                // update template with timestamp and generated commandId
                GenericMachineCommandDTO<?> commandDTO = genericMachineCommand(command);
                if (commandDTO.getMessage().getOutputId().trim().equalsIgnoreCase("AUTO_ID")) {
                    String commandId = Long.toString(factoryScada.getCommandIdGenerator().generateId());
                    commandDTO.getMessage().setOutputId(commandId);

                    nodeIdToCommandId.put(node.getId(), commandId);
                } else {
                    String commandId = commandDTO.getMessage().getOutputId().trim();
                    nodeIdToCommandId.put(node.getId(), commandId);
                }
                command = commandDTO.getMessage().getOutputId();
                commandDTO.setTimestamp(String.valueOf(Instant.now().toEpochMilli()));
                ObjectMapper mapper = new ObjectMapper();
                Protocol protocol = machine.getProtocol();
                String jsonString = mapper.writeValueAsString(commandDTO);
                try {

                    // send to plc socketthis.runningCommand
                    protocol.send(jsonString);

                    // add the command to log list
                    factoryScada.addLogsForFrontend(jsonString);

                    // update storage in backend
                    CommandStatus status = this.factoryScada.getMachineLastCommandStatusMap().getOrDefault(machineName,
                            new CommandStatus());
                    status.setCurrentCommandTimestamp(commandDTO.getTimestamp());
                    status.setCurrentCommandName(commandDTO.getMessage().getName());
                    status.setCurrentCommandId(commandDTO.getMessage().getOutputId());
                    status.setCurrentCommandRawJSON(jsonString);
                    this.factoryScada.getMachineLastCommandStatusMap().put(machineName, status);
                } catch (ProtocolException e) {
                    log.error("Communication error with controller {}", e.getMessage());
                    throw new RuntimeException(e);
                }

            } catch (JsonProcessingException e) {
                log.error("Json conversion error {}", e.getMessage());
                throw new RuntimeException(e);
            }
        } else {
            log.error("Cannot run mission command {}, No machine {} in the configuration", command, machineName);
        }

    }
}
