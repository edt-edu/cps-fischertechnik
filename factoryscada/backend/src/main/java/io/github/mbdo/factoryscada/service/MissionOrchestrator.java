package io.github.mbdo.factoryscada.service;

import java.time.Instant;
import java.util.ArrayList;

import io.github.mbdo.factoryscada.core.dtos.CommandMessage;
import io.github.mbdo.factoryscada.domain.CommandStatus;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import io.github.mbdo.factoryscada.core.AbstractMachine;
import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.domains.factoryscada.dtos.FactoryMissionsConfiguration;
import io.github.mbdo.factoryscada.domains.factoryscada.dtos.FactoryMissionsConfiguration.Command;
import io.github.mbdo.factoryscada.domains.factoryscada.dtos.FactoryMissionsConfiguration.MissionConfiguration;
import io.github.mbdo.factoryscada.socket.Protocol;
import io.github.mbdo.factoryscada.socket.exception.ProtocolException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Service
public class MissionOrchestrator {


    private final FactoryScada factoryScada;
    private final FactoryMissionsConfiguration missionsConfiguration;

    private MissionConfiguration runningMissionConfiguration;
    private Command runningCommand;
    private String runningCommandId;
    private int missionCommandIndex = 0;

    public MissionOrchestrator(FactoryScada factoryScada) {
        this.factoryScada = factoryScada;
        this.missionsConfiguration = factoryScada.getMissionsConfiguration();
    }

    public String startMission(String missionName) {
        var mission = this.missionsConfiguration.missions().stream().filter(m -> m.name().equals(missionName)).findAny();
        if(mission.isPresent()) {
            if(runningMissionConfiguration !=  null && runningCommand != null) {
                stopMission();
            }
            // start the mission
            if(!mission.get().commands().isEmpty()) {
                this.runningMissionConfiguration = mission.get();
                this.missionCommandIndex=0;

               this.sendMissionCommand(this.runningMissionConfiguration, this.missionCommandIndex);

                return "Mission "+missionName+" started";
            } else {
                log.error("Mission {} has no command to run", missionName);
                return "Mission has no command to run";
            }
            
        } else {
            log.error("no mission {} found in the configuration", missionName);
            return "Mission "+missionName+" not found";
        }
    }

    /**
     * Stop any running mission
     * @return
     */
    public String stopMission() {
        if(runningMissionConfiguration !=  null && runningCommand != null) {
            // look for the machine that is currently running a command in current the mission
            String machineName = this.getMachineNameFromJsonCommand(this.runningCommand.placeholder());
            AbstractMachine machine = factoryScada.getFactoryScadaInstance().machines().get(machineName);
            // TODO send stop to the machine
            CommandMessage stopMessage = new CommandMessage();
            stopMessage.setParameters(new ArrayList<>());
            stopMessage.setJsonType("COMMAND");
            stopMessage.setName("STOP");
            stopMessage.setType(this.getMachineTypeFromJsonCommand(this.runningCommand.placeholder()));
            stopMessage.setOutputId(Long.toString(factoryScada.getCommandIdGenerator().generateId()));
            GenericMachineCommandDTO<?> commandDTO = new GenericMachineCommandDTO<>(machineName, String.valueOf(Instant.now().toEpochMilli()),
                    stopMessage);
            ObjectMapper mapper = new ObjectMapper();
            Protocol protocol = machine.getProtocol();
            try {
                String jsonString = mapper.writeValueAsString(commandDTO);
                // send to plc socket
                protocol.send(jsonString);
                // update storage in backend
                CommandStatus status = this.factoryScada.getMachineLastCommandStatusMap().getOrDefault(machineName, new CommandStatus());
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
            this.runningMissionConfiguration =  null ;
            this.runningCommand = null;
            this.missionCommandIndex = 0;
            // TODO notify front about end of mission
            return "Mission stopped";
        }
        return "";
    }

    public void receivedMachineCommandFeedback(CommandStatus commandStatus) {
        // if isRunning a mission, wait for the current command feedback
        if(runningMissionConfiguration !=  null && runningCommand != null) {
            if (commandStatus.getCurrentCommandId().equals(this.runningCommandId)) {
                switch ( (commandStatus.getCommandFeedbackStatus().toUpperCase())){
                    case "DONE":
                        //   if success , send next command, or notify end of mission if this is the last
                        this.missionCommandIndex++;
                        if(this.runningMissionConfiguration.commands().size() > this.missionCommandIndex) {
                            log.info("Processing command {} of Mission {}", this.missionCommandIndex, this.runningMissionConfiguration.name());
                            this.sendMissionCommand(this.runningMissionConfiguration, this.missionCommandIndex);
                        } else {
                            log.info("Last command of Mission {} has finished", this.runningMissionConfiguration.name());
                        }
                        break;
                    case "INTERRUPTED":
                    case "ABORTED_ERROR":
                    case "ABORTED_TIMEOUT":
                        //   if aborted, notify mission aborted
                        this.stopMission();
                        break;
                    default:
                        // ignore other internal command feedback
                        break;
                }
            }
        }
    }

    protected String getMachineNameFromJsonCommand(String jsonCommandString) {
        ObjectMapper mapper = new ObjectMapper();
    	try {
            GenericMachineCommandDTO<?> machineCommand = mapper.readValue(jsonCommandString, GenericMachineCommandDTO.class);

            return machineCommand.getTopicName();
		} catch (JsonProcessingException e) {
			log.error("Error parsing feedback: {}", e.getMessage());
		}
        return null;
    }
    protected String getMachineTypeFromJsonCommand(String jsonCommandString) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            GenericMachineCommandDTO<?> machineCommand = mapper.readValue(jsonCommandString, GenericMachineCommandDTO.class);

            return machineCommand.getMessage().getType();
        } catch (JsonProcessingException e) {
            log.error("Error parsing feedback: {}", e.getMessage());
        }
        return null;
    }

    protected GenericMachineCommandDTO<?> genericMachineCommand( String jsonCommandString ) throws JsonProcessingException {
        ObjectMapper mapper = JsonMapper.builder().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).build();
        TypeReference<GenericMachineCommandDTO<?>> typeRef = new TypeReference<GenericMachineCommandDTO<?>>() {};
        return mapper.readValue(jsonCommandString, typeRef);
    }

    /**
     *
     */
    protected void sendMissionCommand(MissionConfiguration missionConfiguration, int missionCommandIndex){

        this.runningCommand = missionConfiguration.commands().get(this.missionCommandIndex);
        String machineName = this.getMachineNameFromJsonCommand(this.runningCommand.placeholder());
        AbstractMachine machine = factoryScada.getFactoryScadaInstance().machines().get(machineName);
        if(machine != null) {
            try {
                // update template with timestamp and generated commandId
                GenericMachineCommandDTO<?> commandDTO = genericMachineCommand(this.runningCommand.placeholder());
                if(commandDTO.getMessage().getOutputId().trim().equalsIgnoreCase("AUTO_ID")) {
                    String commandId = Long.toString(factoryScada.getCommandIdGenerator().generateId());
                    commandDTO.getMessage().setOutputId(commandId);
                }
                this.runningCommandId = commandDTO.getMessage().getOutputId();
                commandDTO.setTimestamp(String.valueOf(Instant.now().toEpochMilli()));
                ObjectMapper mapper = new ObjectMapper();
                Protocol protocol = machine.getProtocol();
                String jsonString = mapper.writeValueAsString(commandDTO);
                try {

                    // send to plc socket
                    protocol.send(jsonString);

                    // update storage in backend
                    CommandStatus status = this.factoryScada.getMachineLastCommandStatusMap().getOrDefault(machineName, new CommandStatus());
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
            log.error("Cannot run mission command {}, No machine {} in the configuration", this.missionCommandIndex, machineName);
        }

    }

}
