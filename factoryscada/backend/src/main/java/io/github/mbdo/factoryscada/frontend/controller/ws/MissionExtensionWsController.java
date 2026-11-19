package io.github.mbdo.factoryscada.frontend.controller.ws;

import io.github.mbdo.factoryscada.frontend.WebSocketDoc;
import io.github.mbdo.factoryscada.frontend.WebSocketPublisher;
import io.github.mbdo.factoryscada.frontend.controller.rest.MissionExtensionRestController;
import io.github.mbdo.factoryscada.frontend.dto.*;
import io.github.mbdo.factoryscada.service.MissionExtensionService;
import io.github.mbdo.factoryscada.service.ReplanningService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.util.List;

/**
 * STOMP endpoints of the mission extension, for WebSocket clients.
 * <p>
 * Part of these features is also available through HTTP in {@link MissionExtensionRestController}.
 */
@Controller
@MessageMapping("/mission-extension")
public class MissionExtensionWsController {

    private final MissionExtensionService missionExtensionService;
    private final ReplanningService replanningService;
    private final WebSocketPublisher webSocketPublisher;

    public MissionExtensionWsController(MissionExtensionService missionExtensionService,
                                        ReplanningService replanningService,
                                        WebSocketPublisher webSocketPublisher) {
        this.missionExtensionService = missionExtensionService;
        this.replanningService = replanningService;
        this.webSocketPublisher = webSocketPublisher;
    }

    @MessageMapping("/configuration")
    @WebSocketDoc (description = "Returns the current configuration of the mission extension, including the list of available missions and their statuses.",
                responseDescription = "The current configuration of the mission extension.")
    public void getConfigurationStomp() {
        webSocketPublisher.sendMissionExtensionConfiguration(missionExtensionService.getConfiguration());
    }

    @MessageMapping("/logs")
    @WebSocketDoc (description = "Returns the logs of the mission extension, including the history of executed missions and their statuses.",
                responseDescription = "The logs of the mission extension.")
    public void getLogsStomp() {
        webSocketPublisher.sendMissionExtensionLogs(missionExtensionService.getMissionLogsDTO());
    }

    @MessageMapping("/start")
    @WebSocketDoc (description = "Starts a mission extension for a given machine with the given mission name.",
                indirectOutputs = { "/topic/mission-extension/configuration", "/topic/mission-extension/command-response" })
    public void startMissionStomp(@Payload MissionExecutionCommandDTO command) {
        try {
            MissionCommandResponseDTO response = missionExtensionService.startMission(command);
            webSocketPublisher.sendMissionExtensionConfiguration(missionExtensionService.getConfiguration());
            webSocketPublisher.sendMissionExtensionCommandResponse(response);
        } catch (IllegalArgumentException ex) {
            webSocketPublisher.sendMissionExtensionCommandResponse(new MissionCommandResponseDTO(ex.getMessage(), command.machineName(), command.missionName(), null));
        }
    }

    @MessageMapping("/stop")
    @WebSocketDoc (description = "Stops a mission with the given  name.",
                indirectOutputs = { "/topic/mission-extension/configuration", "/topic/mission-extension/command-response" })
    public void stopMissionStomp(@Payload MissionExecutionCommandDTO command) {
        MissionCommandResponseDTO response = missionExtensionService.stopMission(command.missionName());
        webSocketPublisher.sendMissionExtensionConfiguration(missionExtensionService.getConfiguration());
        webSocketPublisher.sendMissionExtensionCommandResponse(response);
    }

    @MessageMapping("/stop-active")
    @WebSocketDoc (description = "Stops all the currently active missions (from mission extension).",
                indirectOutputs = { "/topic/mission-extension/configuration", "/topic/mission-extension/command-response" })
    public void stopActiveMissionStomp() {
        MissionCommandResponseDTO response = missionExtensionService.stopActiveMission();
        webSocketPublisher.sendMissionExtensionConfiguration(missionExtensionService.getConfiguration());
        webSocketPublisher.sendMissionExtensionCommandResponse(response);
    }

    // Global Mission STOMP Endpoints
    @MessageMapping("/global/start")
    @WebSocketDoc (description = "Starts all machine missions related to the global mission.",
                indirectOutputs = { "/topic/mission-extension/configuration", "/topic/mission-extension/command-response" })
    public void startGlobalMissionStomp(@Payload GlobalMissionStartCommandDTO command) {
        try {
            MissionCommandResponseDTO response = missionExtensionService.startGlobalMission(
                command.globalMissionName(),
                command.machineOverrides());
            webSocketPublisher.sendMissionExtensionConfiguration(missionExtensionService.getConfiguration());
            webSocketPublisher.sendMissionExtensionCommandResponse(response);
        } catch (IllegalArgumentException ex) {
            webSocketPublisher.sendMissionExtensionCommandResponse(new MissionCommandResponseDTO(ex.getMessage(), null, null, null));
        }
    }

    @MessageMapping("/global/override")
    @WebSocketDoc (description = "Overrides the current global mission with new global mission. Replaces the current machine missions with the new ones while trying to maintains  consistent state.",
                indirectOutputs = { "/topic/mission-extension/configuration", "/topic/mission-extension/command-response" })
    public void setGlobalMissionMachineOverrideStomp(@Payload GlobalMissionMachineOverrideCommandDTO command) {
        try {
            MissionCommandResponseDTO response = missionExtensionService.setGlobalMissionMachineOverride(
                command.machineName(),
                command.missionName());
            webSocketPublisher.sendMissionExtensionConfiguration(missionExtensionService.getConfiguration());
            webSocketPublisher.sendMissionExtensionCommandResponse(response);
        } catch (IllegalArgumentException ex) {
            webSocketPublisher.sendMissionExtensionCommandResponse(new MissionCommandResponseDTO(ex.getMessage(), command.machineName(), command.missionName(), null));
        }
    }

    /**
     * Force stop: every mission stops at once, in its current state, then every machine is sent a stop, which
     * interrupts the command it executes.
     */
    @MessageMapping("/global/stop")
    @WebSocketDoc (description = "Stops the current global mission, all machine missions related to it, and the machines themselves.",
                indirectOutputs = { "/topic/mission-extension/configuration", "/topic/mission-extension/command-response" })
    public void stopGlobalMissionStomp() {
        MissionCommandResponseDTO stopped = missionExtensionService.stopGlobalMission();
        List<String> machines = replanningService.stopAllMachines();
        MissionCommandResponseDTO response = new MissionCommandResponseDTO(
            stopped.message() + " Stop sent to: " + (machines.isEmpty() ? "no machine" : String.join(", ", machines)),
            null, null, null);
        webSocketPublisher.sendMissionExtensionConfiguration(missionExtensionService.getConfiguration());
        webSocketPublisher.sendMissionExtensionCommandResponse(response);
    }
}
