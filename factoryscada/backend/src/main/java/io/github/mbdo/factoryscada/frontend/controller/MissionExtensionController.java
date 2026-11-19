package io.github.mbdo.factoryscada.frontend.controller;

import io.github.mbdo.factoryscada.frontend.WebSocketDoc;
import io.github.mbdo.factoryscada.frontend.WebSocketPublisher;
import io.github.mbdo.factoryscada.frontend.dto.*;
import io.github.mbdo.factoryscada.service.MissionExtensionService;
import io.github.mbdo.factoryscada.service.ReplanningService;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Controller
@RestController
@RequestMapping("/api/mission-extension")
public class MissionExtensionController {

    private final MissionExtensionService missionExtensionService;
    private final ReplanningService replanningService;
    private final WebSocketPublisher webSocketPublisher;

    public MissionExtensionController(MissionExtensionService missionExtensionService,
                                      ReplanningService replanningService,
                                      WebSocketPublisher webSocketPublisher) {
        this.missionExtensionService = missionExtensionService;
        this.replanningService = replanningService;
        this.webSocketPublisher = webSocketPublisher;
    }

    // REST Endpoints for HTTP clients
    @GetMapping("/configuration")
    public MissionExtensionConfigurationDTO getConfiguration() {
        return missionExtensionService.getConfiguration();
    }

    @GetMapping("/logs")
    public MissionLogsDTO getLogs() {
        return missionExtensionService.getMissionLogsDTO();
    }

    @PostMapping("/start")
    public MissionCommandResponseDTO startMission(
        @org.springframework.web.bind.annotation.RequestBody MissionExecutionCommandDTO command) {
        try {
            return missionExtensionService.startMission(command);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }
    }

    @PostMapping("/{missionName}/stop")
    public MissionCommandResponseDTO stopMission(@PathVariable String missionName) {
        return missionExtensionService.stopMission(missionName);
    }

    @PostMapping("/stop-active")
    public MissionCommandResponseDTO stopActiveMission() {
        return missionExtensionService.stopActiveMission();
    }

    // STOMP Endpoints for WebSocket subscribers
    @MessageMapping("/mission-extension/configuration")
    @WebSocketDoc (description = "Returns the current configuration of the mission extension, including the list of available missions and their statuses.",
                responseDescription = "The current configuration of the mission extension.")
    public void getConfigurationStomp() {
        webSocketPublisher.sendMissionExtensionConfiguration(missionExtensionService.getConfiguration());
    }

    @MessageMapping("/mission-extension/logs")
    @WebSocketDoc (description = "Returns the logs of the mission extension, including the history of executed missions and their statuses.",
                responseDescription = "The logs of the mission extension.")
    public void getLogsStomp() {
        webSocketPublisher.sendMissionExtensionLogs(missionExtensionService.getMissionLogsDTO());
    }

    @MessageMapping("/mission-extension/start")
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

    @MessageMapping("/mission-extension/stop")
    @WebSocketDoc (description = "Stops a mission with the given  name.",
                indirectOutputs = { "/topic/mission-extension/configuration", "/topic/mission-extension/command-response" })
    public void stopMissionStomp(@Payload MissionExecutionCommandDTO command) {
        MissionCommandResponseDTO response = missionExtensionService.stopMission(command.missionName());
        webSocketPublisher.sendMissionExtensionConfiguration(missionExtensionService.getConfiguration());
        webSocketPublisher.sendMissionExtensionCommandResponse(response);
    }

    @MessageMapping("/mission-extension/stop-active")
    @WebSocketDoc (description = "Stops all the currently active missions (from mission extension).",
                indirectOutputs = { "/topic/mission-extension/configuration", "/topic/mission-extension/command-response" })
    public void stopActiveMissionStomp() {
        MissionCommandResponseDTO response = missionExtensionService.stopActiveMission();
        webSocketPublisher.sendMissionExtensionConfiguration(missionExtensionService.getConfiguration());
        webSocketPublisher.sendMissionExtensionCommandResponse(response);
    }

    // Global Mission STOMP Endpoints
    @MessageMapping("/mission-extension/global/start")
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

    @MessageMapping("/mission-extension/global/override")
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
    @MessageMapping("/mission-extension/global/stop")
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
