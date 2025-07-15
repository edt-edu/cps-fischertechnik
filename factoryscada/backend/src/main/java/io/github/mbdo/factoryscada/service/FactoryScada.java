package io.github.mbdo.factoryscada.service;

import static io.github.mbdo.factoryscada.utilities.Utilities.convertYamlToObject;
import static io.github.mbdo.factoryscada.utilities.Utilities.findMachineClass;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.mbdo.factoryscada.core.AbstractMachine;
import io.github.mbdo.factoryscada.core.CommandFeedbackDTO;
import io.github.mbdo.factoryscada.domain.CommandStatus;
import io.github.mbdo.factoryscada.domain.MachineStatus;
import io.github.mbdo.factoryscada.domains.factoryscada.dtos.FactoryScadaConfiguration;
import io.github.mbdo.factoryscada.domains.factoryscada.dtos.FactoryScadaInstance;
import io.github.mbdo.factoryscada.domains.mission.dtos.FactoryMissionsParallelized_dto;
import io.github.mbdo.factoryscada.domains.mission.dtos.MissionParallelized_dto;
import io.github.mbdo.factoryscada.domains.mission.dtos.Node_dto;
import io.github.mbdo.factoryscada.frontend.WebSocketPublisher;
import io.github.mbdo.factoryscada.service.Visitor.ExecuterVisitor;
import io.github.mbdo.factoryscada.service.Visitor.InitializerVisitor;
import io.github.mbdo.factoryscada.socket.Protocol;
import io.github.mbdo.factoryscada.socket.SocketProtocol;
import io.github.mbdo.factoryscada.socket.exception.ProtocolException;
import io.github.mbdo.factoryscada.utilities.AppEnvironment;
import io.github.mbdo.factoryscada.utilities.CommandIdGenerator;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Service
public class FactoryScada {

    /**
     * data coming from the configuration yaml file, usually : "configuration.yml" 
     */
    private final FactoryScadaConfiguration factoryScadaConfiguration;
    /**
     * contains the currently controlled PLC and machines ()
     */
    private final FactoryScadaInstance factoryScadaInstance;

    private final Map<String, Map<String, String>> commandPlaceholder;

    private final WebSocketPublisher webSocketPublisher;
    
    /**
     * data coming from the mission configuration yaml file, usually : "missions-configuration.yml" 
     */
    private final FactoryMissionsParallelized_dto missionsParallelized_dto;
    private final Map<String, CommandStatus> machineLastCommandStatusMap = new HashMap<>();
    private final Map<String, MachineStatus> machineLastMachineStatusMap = new HashMap<>();
    private final SimpMessagingTemplate template;
    private final AppEnvironment appEnvironment;
    private final ExecuterVisitor executerVisitor;
    private final CommandIdGenerator commandIdGenerator;

    private final ApplicationContext applicationContext;

    @Autowired
    public FactoryScada(SimpMessagingTemplate template, AppEnvironment appEnvironment, ApplicationContext applicationContext, WebSocketPublisher webSocketPublisher) {
        this.applicationContext = applicationContext;
        this.appEnvironment = appEnvironment;
        this.template = template;
        this.webSocketPublisher = webSocketPublisher;
        this.factoryScadaInstance = factoryInstance();
        this.commandPlaceholder = commandPlaceholder();
        this.factoryScadaConfiguration = factoryConfiguration(); 

        // Initialization and validation of mission graph
        this.missionsParallelized_dto = missionsParallelized();
        for (MissionParallelized_dto mission : this.missionsParallelized_dto.getMissions()){
            InitializerVisitor resolver = new InitializerVisitor(mission.getNodes());
            for (Node_dto node : mission.getNodes()) {
                node.accept(resolver);
            }
        }
        this.executerVisitor = new ExecuterVisitor(this, this.missionsParallelized_dto, this.template);

        this.commandIdGenerator = new CommandIdGenerator();
    }

    /**
     * Retrieves the factory configuration by converting a YAML file located at the specified path
     * into an instance of {@link FactoryScadaConfiguration} using Jackson ObjectMapper.
     *
     * @return The FactoryConfiguration instance parsed from the YAML file.
     * @throws RuntimeException If there is an error during YAML parsing or file reading.
     */
    private FactoryScadaConfiguration factoryConfiguration() {
        return convertYamlToObject(applicationContext, appEnvironment.getConfigurationFilePath(), FactoryScadaConfiguration.class);
    }

    /**
     * Creates a {@link FactoryScadaInstance} based on the provided {@link FactoryScadaConfiguration}.
     * This method initializes controllers and machines defined in the configuration,
     * creating instances of {@link Protocol} and {@link AbstractMachine} respectively.
     *
     * @return A {@link FactoryScadaInstance} containing initialized controllers and machines based on the configuration.
     * @throws RuntimeException If there is an error during controller or machine instantiation.
     */
    private FactoryScadaInstance factoryInstance() {
        @Valid FactoryScadaConfiguration factoryScadaConfiguration = factoryConfiguration();
        Map<String, Protocol> controllers = new HashMap<>();
        Map<String, AbstractMachine> machines = new HashMap<>();

        for (FactoryScadaConfiguration.ControllerConfiguration controllerConfiguration : factoryScadaConfiguration.controllers()) {
            // Create controller instance and subscribe to a feedback topic
            Protocol controllerInstance = new SocketProtocol(
                    controllerConfiguration.host(),
                    controllerConfiguration.commandPort(),
                    controllerConfiguration.notificationPort(),
                    (message) -> {
                    	// forward raw feedback / notify frontend
                        webSocketPublisher.sendControllerFeedback(message);
                    	// store feedback for later request and use  
                    	updateMachineLastCommandStatusFeedback(message);
                    },
                    (sendChannelConnected, receivedChannelConnected) -> {
                        webSocketPublisher.sendPlcStatus(controllerConfiguration.name(), sendChannelConnected, receivedChannelConnected);
                    }
            );
            try {
				controllerInstance.start();
			} catch (ProtocolException e) {
				log.error("Failed to start controller protocol", e);
			}
            controllers.put(controllerConfiguration.name(), controllerInstance);

            // Create machines for each controller
            if(controllerConfiguration.machines() != null) {
	            for (FactoryScadaConfiguration.ControllerConfiguration.MachineConfiguration machineConfiguration : controllerConfiguration.machines()) {
	                AbstractMachine machineInstance = createMachineInstance(machineConfiguration, controllerInstance);
	                machines.put(machineConfiguration.name(), machineInstance);
	            }
            } else {
            	log.warn("No machine declared on controller "+controllerConfiguration.name() + " ; you should verify the configuration file");
            }
        }

        return new FactoryScadaInstance(controllers, machines);
    }

    /**
     * parse the feedback message to update the LastCommandStatus or the LastMachineStatus
     * @param feedbackMsg
     */
    private void updateMachineLastCommandStatusFeedback(String feedbackMsg) {
    	ObjectMapper mapper = new ObjectMapper();
    	try {
			CommandFeedbackDTO feedback = mapper.readValue(feedbackMsg, CommandFeedbackDTO.class);
			String machineName = feedback.getTopicName();

            String jsonType = feedback.getMessage().getJsonType();
            switch (jsonType) {
                case "COMMAND_FEEDBACK":
                    CommandStatus commandStatus = this.machineLastCommandStatusMap.getOrDefault(machineName, new CommandStatus());
                    
                    commandStatus.setCommandFeedbackStatus(feedback.getMessage().getStatus());
                    commandStatus.setCommandFeedbackTimestamp(feedback.getTimestamp());
                    commandStatus.setCommandFeedbackInfo(feedback.getMessage().getInfo());
                    commandStatus.setCommandFeedbackRawJSON(feedbackMsg);
                    // log warning if feedback id doesn't correspond to current command id
                    if (commandStatus.getCurrentCommandId() !=  null && !commandStatus.getCurrentCommandId().equals(feedback.getMessage().getCommandId())) {
                        log.warn("Received Feedback CommandId {} doesn't match current Command CommandId {}", feedback.getMessage().getCommandId(), commandStatus.getCurrentCommandId());
                    }

                    this.machineLastCommandStatusMap.put(machineName, commandStatus);
                    // publish changes to frontend
                    webSocketPublisher.sendCommandStatus(machineName, commandStatus);
                    // notify ExecuterVisitor
                    this.executerVisitor.receivedMachineCommandFeedback(commandStatus);
                    break;
                case "MACHINE_FEEDBACK":
                    MachineStatus machineStatus = this.machineLastMachineStatusMap.getOrDefault(machineName, new MachineStatus());
                    
                    machineStatus.setMachineFeedbackStatus(feedback.getMessage().getStatus());
                    machineStatus.setMachineFeedbackTimestamp(feedback.getTimestamp());
                    machineStatus.setMachineFeedbackInfo(feedback.getMessage().getInfo());
                    machineStatus.setMachineFeedbackRawJSON(feedbackMsg);
                    
                    this.machineLastMachineStatusMap.put(machineName, machineStatus);
                    // publish changes to frontend
                    webSocketPublisher.sendMachineStatus(machineName, machineStatus);
                    
                    break;
                default:
                    break;
            }
		} catch (JsonProcessingException e) {
			log.error("Error parsing feedback: {}", e.getMessage());
		}
    }
    
    /**
     * Creates an instance of AbstractMachine based on the provided configuration.
     *
     * @param machineConfiguration The configuration of the machine to create.
     * @param controllerInstance   The controller instance to pass to the machine constructor.
     * @return An instance of AbstractMachine.
     * @throws RuntimeException If there is an error during class scanning, constructor invocation,
     *                          or if no suitable constructor is found.
     */
    private AbstractMachine createMachineInstance(FactoryScadaConfiguration.ControllerConfiguration.MachineConfiguration machineConfiguration,
                                                  Protocol controllerInstance) {
        String machineType = machineConfiguration.type();

        try {
            log.info("creating MachineInstance for "+machineConfiguration.name());
            // Find the concrete machine class corresponding to the machine type
            Class<? extends AbstractMachine> machineClass = findMachineClass(machineType, appEnvironment.getMachineDomainsPackageName());

            // Instantiate the machine using the found class, constructor that takes String, Protocol and List<String> as parameters
            Constructor<? extends AbstractMachine> constructor = machineClass.getConstructor(String.class, Protocol.class, List.class);
            Map<String, String> machineRawCommandPlaceholder = this.commandPlaceholder().get(machineType);
            List<String> rawCommandNames = machineRawCommandPlaceholder != null ? machineRawCommandPlaceholder.keySet().stream().collect(Collectors.toList()) : new ArrayList<>();
            return constructor.newInstance(machineConfiguration.name(), controllerInstance, rawCommandNames);

        } catch (Exception e) {
            log.error("Error during machine instantiation: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Converts YAML data from the specified file path into a structured map of machine types and their commands with placeholders.
     *
     * @return A map where keys are machine type names and values are maps of command names to placeholders.
     * Returns an empty map if the YAML data is invalid or cannot be parsed.
     */
    private Map<String, Map<String, String>> commandPlaceholder() {
        // Define the CommandPlaceholder record with nested records for MachinesType and Command
        record CommandPlaceholder(List<MachinesType> machinesType) implements Serializable {
            record MachinesType(String name, List<Command> commands) {
                record Command(String name, String placeholder) {
                }
            }
        }
        // Convert the YAML file at the specified path to a CommandPlaceholder object
        CommandPlaceholder commandPlaceholder = convertYamlToObject(applicationContext, appEnvironment.getCommandPlaceholderConfigFile(), CommandPlaceholder.class);
        // Initialize the result map
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        // Check if the CommandPlaceholder object and its machinesType list are not null
        if (commandPlaceholder != null && commandPlaceholder.machinesType() != null) {
            // Iterate over each MachinesType in the machinesType list
            for (CommandPlaceholder.MachinesType machinesType : commandPlaceholder.machinesType()) {
                // Initialize a map to store command names and their placeholders for the current MachinesType
                Map<String, String> commandMap = new LinkedHashMap<>();
                // Iterate over each Command in the commands list of the current MachinesType
                for (CommandPlaceholder.MachinesType.Command command : machinesType.commands()) {
                    // Add the command name and placeholder to the command map
                    commandMap.put(command.name(), command.placeholder());
                }
                // Add the current MachinesType name and its corresponding command map to the result map
                result.put(machinesType.name(), commandMap);
            }
        }
        // Return the result map
        return result;
    }
    /**
     * Retrieves the missions parallelized configuration by converting a YAML file located at the specified path
     * into an instance of {@link FactoryScadaConfiguration} using Jackson ObjectMapper.
     *
     * @return The missions parallelizedConfiguration instance parsed from the YAML file.
     * @throws RuntimeException If there is an error during YAML parsing or file reading.
     */
    private FactoryMissionsParallelized_dto missionsParallelized() {
        return convertYamlToObject(applicationContext, appEnvironment.getMissionsConfigurationParallelizedFilePath(), FactoryMissionsParallelized_dto.class);
    }
}
