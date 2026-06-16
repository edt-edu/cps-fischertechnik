package io.github.mbdo.factoryscada.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.inria.mbdo.mission.runtime.config.MissionExtensionConfig;
import io.github.mbdo.factoryscada.core.AbstractMachine;
import io.github.mbdo.factoryscada.core.CommandFeedbackDTO;
import io.github.mbdo.factoryscada.domain.CommandStatus;
import io.github.mbdo.factoryscada.domain.MachineStatus;
import io.github.mbdo.factoryscada.domains.factoryscada.dtos.FactoryScadaConfiguration;
import io.github.mbdo.factoryscada.domains.factoryscada.dtos.FactoryScadaInstance;
import io.github.mbdo.factoryscada.domains.mission.dsl.dtos.FactoryMissionsParallelized_dto;
import io.github.mbdo.factoryscada.domains.mission.dsl.dtos.MissionParallelized_dto;
import io.github.mbdo.factoryscada.domains.mission.dsl.dtos.Node_dto;
import io.github.mbdo.factoryscada.frontend.WebSocketPublisher;
import io.github.mbdo.factoryscada.mission.dsl.visitor.ExecuterVisitor;
import io.github.mbdo.factoryscada.mission.dsl.visitor.InitializerVisitor;
import io.github.mbdo.factoryscada.mqtt.MqttConfig;
import io.github.mbdo.factoryscada.mqtt.MqttGateway;
import io.github.mbdo.factoryscada.mqtt.MqttGatewayService;
import io.github.mbdo.factoryscada.socket.Protocol;
import io.github.mbdo.factoryscada.socket.SocketProtocol;
import io.github.mbdo.factoryscada.socket.exception.ProtocolException;
import io.github.mbdo.factoryscada.utilities.AppEnvironment;
import io.github.mbdo.factoryscada.utilities.BoundedLogBuffer;
import io.github.mbdo.factoryscada.utilities.CommandIdGenerator;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static io.github.mbdo.factoryscada.utilities.Utilities.convertYamlToObject;
import static io.github.mbdo.factoryscada.utilities.Utilities.findMachineClass;

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
     * data coming from the mission configuration yaml file, usually :
     * "missions-configuration.yml"
     */
    private final FactoryMissionsParallelized_dto missionsParallelized_dto;
    private final Map<String, CommandStatus> machineLastCommandStatusMap = new HashMap<>();
    private final Map<String, MachineStatus> machineLastMachineStatusMap = new HashMap<>();
    private final SimpMessagingTemplate template;
    private final AppEnvironment appEnvironment;
    private final ExecuterVisitor executerVisitor;
    private final CommandIdGenerator commandIdGenerator;

    // Mission extension config
    private final MissionExtensionConfig missionExtensionConfig;

    private final ApplicationContext applicationContext;
    private final int logLimit;
    public Map<String, Integer> sessionLogLimits = new ConcurrentHashMap<>();

    private BoundedLogBuffer<String> frontendLogsList;

    // MQTT messages
    private final MqttConfig mqttConfig;
    private final MqttGateway mqttGateway;
    private final MqttGatewayService mqttGatewayService;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Autowired
    public FactoryScada(SimpMessagingTemplate template, AppEnvironment appEnvironment,
            ApplicationContext applicationContext, WebSocketPublisher webSocketPublisher,
            @Value("${log.limit:500}") int logLimit, MqttConfig mqttConfig, MqttGateway mqttGateway,
            MqttGatewayService mqttGatewayService, MissionExtensionConfig missionExtensionConfig) {
        this.applicationContext = applicationContext;
        this.appEnvironment = appEnvironment;
        this.template = template;
        this.webSocketPublisher = webSocketPublisher;
        this.mqttConfig = mqttConfig;
        this.webSocketPublisher.factoryscada = this;
        this.commandIdGenerator = new CommandIdGenerator();
        this.mqttGateway = mqttGateway;
        this.mqttGatewayService = mqttGatewayService;
        this.factoryScadaInstance = factoryInstance();
        this.commandPlaceholder = commandPlaceholder();
        this.factoryScadaConfiguration = factoryConfiguration();
        this.logLimit = logLimit;
        this.frontendLogsList = new BoundedLogBuffer<String>(logLimit);
        this.missionExtensionConfig = missionExtensionConfig;

        // Initialization and validation of mission graph
        this.missionsParallelized_dto = missionsParallelized();
        for (MissionParallelized_dto mission : this.missionsParallelized_dto.getMissions()) {
            InitializerVisitor resolver = new InitializerVisitor(mission.getNodes());
            for (Node_dto node : mission.getNodes()) {
                node.accept(resolver);
            }
        }
        this.executerVisitor = new ExecuterVisitor(this, this.missionsParallelized_dto, this.template);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initAfterStartup() {
    }

    @PostConstruct
    public void init() {
        // bean itself initialized
    }

    @EventListener(org.springframework.context.event.ContextRefreshedEvent.class)
    public void onContextReady() {
        // fires after all singleton beans are instantiated and initialized
        log.info("Starting threads for PLC sockets");
        for (Map.Entry<String, Protocol> c : this.getFactoryScadaInstance().controllers().entrySet()) {

            log.debug("Starting thread sockets for {}", c.getKey());
            try {
                c.getValue().start();
            } catch (ProtocolException e) {
                log.error("Failed to start protocol threads for PLC {}", c.getKey(), e);

                // send connection failure to MQTT
                String topic = "FactoryScada/Backend/internal/plc_connection/" + c.getKey()
                        + "/status";
                mqttGateway.sendToMqtt("unreachable", topic);
            }
        }

        missionExtensionConfig.bindMachines(factoryScadaInstance.machines());
        log.info("Mission extension adapters bound to {} machines", factoryScadaInstance.machines().size());
    }

    /**
     * Retrieves the factory configuration by converting a YAML file located at the
     * specified path
     * into an instance of {@link FactoryScadaConfiguration} using Jackson
     * ObjectMapper.
     *
     * @return The FactoryConfiguration instance parsed from the YAML file.
     * @throws RuntimeException If there is an error during YAML parsing or file
     *                          reading.
     */
    private FactoryScadaConfiguration factoryConfiguration() {
        return convertYamlToObject(applicationContext, appEnvironment.getConfigurationFilePath(),
                FactoryScadaConfiguration.class);
    }

    /**
     * Creates a {@link FactoryScadaInstance} based on the provided
     * {@link FactoryScadaConfiguration}.
     * This method initializes controllers and machines defined in the
     * configuration,
     * creating instances of {@link Protocol} and {@link AbstractMachine}
     * respectively.
     *
     * @return A {@link FactoryScadaInstance} containing initialized controllers and
     *         machines based on the configuration.
     * @throws RuntimeException If there is an error during controller or machine
     *                          instantiation.
     */
    private FactoryScadaInstance factoryInstance() {
        @Valid
        FactoryScadaConfiguration factoryScadaConfiguration = factoryConfiguration();
        Map<String, Protocol> controllers = new HashMap<>();
        Map<String, AbstractMachine> machines = new HashMap<>();

        for (FactoryScadaConfiguration.ControllerConfiguration controllerConfiguration : factoryScadaConfiguration
                .controllers()) {
            // Create controller instance and subscribe to a feedback topic
            Protocol controllerInstance = new SocketProtocol(
                    controllerConfiguration.host(),
                    controllerConfiguration.commandPort(),
                    controllerConfiguration.notificationPort(),
                    (message) -> {
                        // forward raw feedback / notify frontend
                        webSocketPublisher.sendControllerFeedback(message);

                        // send logs to frontend
                        this.addLogsForFrontend(message);

                        // store feedback for later request and use
                        updateMachineLastCommandStatusFeedback(message);
                    },
                    (sendChannelConnected, receivedChannelConnected) -> {
                        webSocketPublisher.sendPlcStatus(controllerConfiguration.name(), sendChannelConnected,
                                receivedChannelConnected);

                        // send connection state to MQTT
                        String topic = "FactoryScada/Backend/internal/plc_connection/" + controllerConfiguration.name()
                                + "/status";
                        if (sendChannelConnected && receivedChannelConnected) {
                            sendWithRetry("connected", topic);
                        } else {
                            sendWithRetry("disconnected", topic);
                        }
                    });

            controllers.put(controllerConfiguration.name(), controllerInstance);

            // Create machines for each controller
            if (controllerConfiguration.machines() != null) {
                for (FactoryScadaConfiguration.ControllerConfiguration.MachineConfiguration machineConfiguration : controllerConfiguration
                        .machines()) {
                    AbstractMachine machineInstance = createMachineInstance(machineConfiguration, controllerInstance);
                    machineInstance.setFactoryScada(this);
                    machines.put(machineConfiguration.name(), machineInstance);
                }
            } else {
                log.warn("No machine declared on controller " + controllerConfiguration.name()
                        + " ; you should verify the configuration file");
            }
        }

        return new FactoryScadaInstance(controllers, machines);
    }

    /**
     * parse the feedback message to update the LastCommandStatus or the
     * LastMachineStatus
     *
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
                    CommandStatus commandStatus = this.machineLastCommandStatusMap.getOrDefault(machineName,
                            new CommandStatus());

                    commandStatus.setCommandFeedbackStatus(feedback.getMessage().getStatus());
                    commandStatus.setCommandFeedbackTimestamp(feedback.getTimestamp());
                    commandStatus.setCommandFeedbackInfo(feedback.getMessage().getInfo());
                    commandStatus.setCommandFeedbackRawJSON(feedbackMsg);
                    // log warning if feedback id doesn't correspond to current command id
                    var currentCommandId = commandStatus.getCurrentCommandId();
                    var feedbackCommandId = feedback.getMessage().getCommandId();
                    var feedbackIdMatchingCommandId = currentCommandId == null ||
                            currentCommandId.equals(feedbackCommandId);
                    if (!feedbackIdMatchingCommandId) {
                        log.warn("Received Feedback CommandId {} doesn't match current Command CommandId {}",
                                feedbackCommandId, currentCommandId);
                    }

                    this.machineLastCommandStatusMap.put(machineName, commandStatus);
                    // publish changes to frontend
                    webSocketPublisher.sendCommandStatus(machineName, commandStatus);
                    // notify ExecuterVisitor
                    this.executerVisitor.receivedMachineCommandFeedback(commandStatus);

                    // update idle status of machine
                    Optional.ofNullable(getFactoryScadaInstance().machines().get(machineName)).ifPresent(machine -> {
                        var feedbackMessage = commandStatus.getCommandFeedbackStatus();
                        var isDone = feedbackMessage.contains("DONE");
                        log.info("Received command feedback for machine {} : {}", machineName, feedbackMessage);
                        log.debug("Updating idle state for machine {} to {} (reason: command feedback {})", machineName,
                                isDone,
                                feedbackCommandId);
                        machine.setIdle(isDone);
                        machine.notifyCommandFeedback(isDone, feedbackMessage);
                    });

                    break;
                case "MACHINE_FEEDBACK":
                    MachineStatus machineStatus = this.machineLastMachineStatusMap.getOrDefault(machineName,
                            new MachineStatus());

                    var status = feedback.getMessage().getStatus();
                    machineStatus.setMachineFeedbackStatus(status);
                    machineStatus.setMachineFeedbackTimestamp(feedback.getTimestamp());
                    machineStatus.setMachineFeedbackInfo(feedback.getMessage().getInfo());
                    machineStatus.setMachineFeedbackRawJSON(feedbackMsg);

                    this.machineLastMachineStatusMap.put(machineName, machineStatus);
                    // publish changes to frontend
                    webSocketPublisher.sendMachineStatus(machineName, machineStatus);

                    // update idle status of machine
                    Optional.ofNullable(getFactoryScadaInstance().machines().get(machineName)).ifPresent(machine -> {
                        log.info("Received status for machine {} : {}", machineName, status);
                        log.debug("Updating idle state for machine {} to {} (reason: machine feedback)", machineName,
                                status.contains("IDLE"));
                        machine.setIdle(status.contains("IDLE"));
                    });

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
     * @param controllerInstance   The controller instance to pass to the machine
     *                             constructor.
     * @return An instance of AbstractMachine.
     * @throws RuntimeException If there is an error during class scanning,
     *                          constructor invocation,
     *                          or if no suitable constructor is found.
     */
    private AbstractMachine createMachineInstance(
            FactoryScadaConfiguration.ControllerConfiguration.MachineConfiguration machineConfiguration,
            Protocol controllerInstance) {
        String machineType = machineConfiguration.type();

        try {
            log.info("creating MachineInstance for {}", machineConfiguration.name());
            // Find the concrete machine class corresponding to the machine type
            Class<? extends AbstractMachine> machineClass = findMachineClass(machineType,
                    appEnvironment.getMachineDomainsPackageName());

            // Instantiate the machine using the found class, constructor that takes String,
            // Protocol and List<String> as parameters
            Constructor<? extends AbstractMachine> constructor = machineClass
                    .getConstructor(AbstractMachine.Parameters.class);
            Map<String, String> machineRawCommandPlaceholder = this.commandPlaceholder().get(machineType);
            var parameters = createMachineParameters(machineConfiguration, controllerInstance,
                    machineRawCommandPlaceholder);
            return constructor.newInstance(parameters);

        } catch (Exception e) {
            log.error("Error during machine instantiation: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @NonNull
    private AbstractMachine.Parameters createMachineParameters(
            FactoryScadaConfiguration.ControllerConfiguration.MachineConfiguration machineConfiguration,
            Protocol controllerInstance,
            Map<String, String> machineRawCommandPlaceholder) {
        List<String> rawCommandNames = machineRawCommandPlaceholder != null
                ? new ArrayList<>(machineRawCommandPlaceholder.keySet())
                : new ArrayList<>();
        return new AbstractMachine.Parameters(machineConfiguration.name(),
                controllerInstance,
                rawCommandNames,
                commandIdGenerator,
                mqttGatewayService);
    }

    /**
     * Converts YAML data from the specified file path into a structured map of
     * machine types and their commands with placeholders.
     *
     * @return A map where keys are machine type names and values are maps of
     *         command names to placeholders.
     *         Returns an empty map if the YAML data is invalid or cannot be parsed.
     */
    private Map<String, Map<String, String>> commandPlaceholder() {
        // Define the CommandPlaceholder record with nested records for MachinesType and
        // Command
        record CommandPlaceholder(List<MachinesType> machinesType) implements Serializable {
            record MachinesType(String name, List<Command> commands) {
                record Command(String name, String placeholder) {
                }
            }
        }
        // Convert the YAML file at the specified path to a CommandPlaceholder object
        CommandPlaceholder commandPlaceholder = convertYamlToObject(applicationContext,
                appEnvironment.getCommandPlaceholderConfigFile(), CommandPlaceholder.class);
        // Initialize the result map
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        // Check if the CommandPlaceholder object and its machinesType list are not null
        if (commandPlaceholder != null && commandPlaceholder.machinesType() != null) {
            // Iterate over each MachinesType in the machinesType list
            for (CommandPlaceholder.MachinesType machinesType : commandPlaceholder.machinesType()) {
                // Initialize a map to store command names and their placeholders for the
                // current MachinesType
                Map<String, String> commandMap = new LinkedHashMap<>();
                // Iterate over each Command in the commands list of the current MachinesType
                for (CommandPlaceholder.MachinesType.Command command : machinesType.commands()) {
                    // Add the command name and placeholder to the command map
                    commandMap.put(command.name(), command.placeholder());
                }
                // Add the current MachinesType name and its corresponding command map to the
                // result map
                result.put(machinesType.name(), commandMap);
            }
        }
        // Return the result map
        return result;
    }

    /**
     * Retrieves the missions parallelized configuration by converting a YAML file
     * located at the specified path
     * into an instance of {@link FactoryScadaConfiguration} using Jackson
     * ObjectMapper.
     *
     * @return The missions parallelizedConfiguration instance parsed from the YAML
     *         file.
     * @throws RuntimeException If there is an error during YAML parsing or file
     *                          reading.
     */
    private FactoryMissionsParallelized_dto missionsParallelized() {
        return convertYamlToObject(applicationContext, appEnvironment.getMissionsConfigurationParallelizedFilePath(),
                FactoryMissionsParallelized_dto.class);
    }

    /**
     * This function is used to add logs in the list containing all frontend logs
     */
    public void addLogsForFrontend(String log) {
        frontendLogsList.add(LocalDateTime.now() + " : " + log);
        this.getWebSocketPublisher()
                .sendFrontendLogs(String.join("\n", this.getFrontendLogsList().snapshot()));
    }

    private void sendWithRetry(String payload, String topic) {
        try {
            mqttGateway.sendToMqtt(payload, topic);
        } catch (Exception e) {
            log.warn(
                    "Failed to publish MQTT message to broker={} topic={}, MQTT system may be not ready: retrying in 1s",
                    mqttConfig.getMqttHost(), topic);
            scheduler.schedule(() -> sendWithRetry(payload, topic), 1, TimeUnit.SECONDS);
        }
    }
}
