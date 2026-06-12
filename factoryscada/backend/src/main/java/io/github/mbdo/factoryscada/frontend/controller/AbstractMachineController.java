package io.github.mbdo.factoryscada.frontend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import io.github.mbdo.factoryscada.core.AbstractMachine;
import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.core.GenericMachineStatusRequestDTO;
import io.github.mbdo.factoryscada.domain.CommandStatus;
import io.github.mbdo.factoryscada.domain.MachineStatus;
import io.github.mbdo.factoryscada.frontend.dto.CommandStatusDTO;
import io.github.mbdo.factoryscada.frontend.dto.MachineStatusDTO;
import io.github.mbdo.factoryscada.frontend.mapper.CommandStatusMapper;
import io.github.mbdo.factoryscada.frontend.mapper.MachineStatusMapper;
import io.github.mbdo.factoryscada.service.FactoryScada;
import io.github.mbdo.factoryscada.socket.Protocol;
import io.github.mbdo.factoryscada.socket.exception.ProtocolException;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractMachineController<T extends AbstractMachine> {

    private final FactoryScada factoryScada;

    @Autowired
    public AbstractMachineController(FactoryScada factoryScada) {
        this.factoryScada = factoryScada;
    }

    protected String executeDebugCommand(String machineName, String message) {
    	
    	try {
            AbstractMachine machine = factoryScada.getFactoryScadaInstance().machines().get(machineName);
            
            Protocol protocol = machine.getProtocol();
            try {
                protocol.send(message);
                log.info("Message sent to controller "+ message);
                CommandStatus status = this.factoryScada.getMachineLastCommandStatusMap().getOrDefault(machineName, new CommandStatus());
                // update storage
                ObjectMapper mapper = JsonMapper.builder().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).build();
                TypeReference<GenericMachineCommandDTO<T>> typeRef = new TypeReference<GenericMachineCommandDTO<T>>() {}; 
                GenericMachineCommandDTO<T> dto = mapper.readValue(message, typeRef);
                status.setCurrentCommandTimestamp(dto.getTimestamp());
                status.setCurrentCommandName(dto.getMessage().getName());
                status.setCurrentCommandId(dto.getMessage().getOutputId());
                status.setCurrentCommandRawJSON(message);
                this.factoryScada.getMachineLastCommandStatusMap().put(machineName, status);
                // publish to frontend
                this.factoryScada.getTemplate().convertAndSend("/topic/"+machineName+"/command-status", 
    					CommandStatusMapper.INSTANCE.commandStatusToCommandStatusDTO(status));
                factoryScada.addLogsForFrontend(message);
                
            } catch (ProtocolException e) {
                log.error("Communication error with controller {}", e.getMessage());
                throw new RuntimeException(e);
            }
        } catch (Exception e) {
            log.error("Error executing debug command '{}' on machine '{}'", message, machineName, e);
            return "Error executing command";
        }
        return "Command executed successfully";
    }
    
    protected String executeCommand(String machineName, String commandName, GenericMachineCommandDTO<T> commandDTO) {
        try {
            AbstractMachine machine = factoryScada.getFactoryScadaInstance().machines().get(machineName);
            if (machine != null) {
                if(commandDTO.getMessage().getOutputId().trim().equalsIgnoreCase("AUTO_ID")) {
                    commandDTO.getMessage().setOutputId(Long.toString(factoryScada.getCommandIdGenerator().generateId()));
                }
                machine.executeCommand(commandName, commandDTO);
                log.info("Executed command '{}' on machine '{}' with payload.message {}", commandName, machineName, commandDTO.getMessage());
             } else {
                log.warn("Machine '{}' not found", machineName);
                return "Machine not found";
            }
        } catch (Exception e) {
            log.error("Error executing command '{}' on machine '{}'", commandName, machineName, e);
            return "Error executing command";
        }
        return "Command executed successfully";
    }
    
    protected String executeRequest(String machineName, GenericMachineStatusRequestDTO<T> requestDTO) {
        try {
            AbstractMachine machine = factoryScada.getFactoryScadaInstance().machines().get(machineName);
            if (machine != null) {
                machine.executeRequest(requestDTO);
                log.info("Executed StatusRequest on machine '{}' with payload.message {}", machineName, requestDTO.getMessage());
            } else {
                log.warn("Machine '{}' not found", machineName);
                return "Machine not found";
            }
        } catch (Exception e) {
            log.error("Error executing StatusRequest on machine '{}'", machineName, e);
            return "Error executing command";
        }
        return "Command executed successfully";
    }
    
    @MessageMapping("/{machineName}/command/debug")
    public String receiveDebugCommand(
            @DestinationVariable("machineName") String machineName,
            @Valid @Payload String message
    ) {
        return executeDebugCommand(machineName, message);
    }
    
    @MessageMapping("/{machineName}/command-status")
    @SendTo("/topic/{machineName}/command-status")
    public CommandStatusDTO getCommandStatus ( 
    	@DestinationVariable("machineName") String machineName
    )  {
        log.info("Received WS request on /"+machineName+"/command-status");
        CommandStatus status = this.factoryScada.getMachineLastCommandStatusMap().getOrDefault(machineName, new CommandStatus());
        return  CommandStatusMapper.INSTANCE.commandStatusToCommandStatusDTO(status);
    }

    @MessageMapping("/{machineName}/machine-status")
    @SendTo("/topic/{machineName}/machine-status")
    public MachineStatusDTO getMachineStatus ( 
    	@DestinationVariable("machineName") String machineName
    )  {
        log.info("Received WS request on /"+machineName+"/machine-status");
        MachineStatus status = this.factoryScada.getMachineLastMachineStatusMap().getOrDefault(machineName, new MachineStatus());
        return  MachineStatusMapper.INSTANCE.machineStatusToMachineStatusDTO(status);
    }
    
    /**
     * Fallback method in case of unmapped message
     * @param destination
     * @param messageContent
     */
    @MessageMapping("**")
    public void handleUnmappedMessage(
    		@Header("simpDestination") String destination,
    		@Payload String messageContent
    		) {
    	log.error("Unmapped message destination={} payload={}", destination, messageContent);
    }
}
