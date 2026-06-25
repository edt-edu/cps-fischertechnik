package io.github.mbdo.factoryscada.frontend.controller;

import io.github.mbdo.factoryscada.frontend.dto.*;
import io.github.mbdo.factoryscada.service.MissionExtensionService;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RestController
@RequestMapping("/api/mission-extension")
public class MissionExtensionController {

    private final MissionExtensionService missionExtensionService;
    private final SimpMessagingTemplate messagingTemplate;

    public MissionExtensionController(MissionExtensionService missionExtensionService, SimpMessagingTemplate messagingTemplate) {
        this.missionExtensionService = missionExtensionService;
        this.messagingTemplate = messagingTemplate;
    }

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
    public void getConfigurationStomp() {
        messagingTemplate.convertAndSend("/topic/mission-extension/configuration",
            missionExtensionService.getConfiguration());
    }

    @MessageMapping("/mission-extension/logs")
    public void getLogsStomp() {
        messagingTemplate.convertAndSend("/topic/mission-extension/logs",
            missionExtensionService.getMissionLogsDTO());
    }

    @MessageMapping("/mission-extension/start")
    public void startMissionStomp(@Payload MissionExecutionCommandDTO command) {
        try {
            MissionCommandResponseDTO response = missionExtensionService.startMission(command);
            messagingTemplate.convertAndSend("/topic/mission-extension/configuration",
                missionExtensionService.getConfiguration());
            messagingTemplate.convertAndSend("/topic/mission-extension/command-response", response);
        } catch (IllegalArgumentException ex) {
            messagingTemplate.convertAndSend("/topic/mission-extension/command-response",
                new MissionCommandResponseDTO(ex.getMessage(), command.machineName(), command.missionName(), null));
        }
    }

    @MessageMapping("/mission-extension/stop")
    public void stopMissionStomp(@Payload MissionExecutionCommandDTO command) {
        MissionCommandResponseDTO response = missionExtensionService.stopMission(command.missionName());
        messagingTemplate.convertAndSend("/topic/mission-extension/configuration",
            missionExtensionService.getConfiguration());
        messagingTemplate.convertAndSend("/topic/mission-extension/command-response", response);
    }

    @MessageMapping("/mission-extension/stop-active")
    public void stopActiveMissionStomp() {
        MissionCommandResponseDTO response = missionExtensionService.stopActiveMission();
        messagingTemplate.convertAndSend("/topic/mission-extension/configuration",
            missionExtensionService.getConfiguration());
        messagingTemplate.convertAndSend("/topic/mission-extension/command-response", response);
    }

    // Global Mission STOMP Endpoints
    @MessageMapping("/mission-extension/global/start")
    public void startGlobalMissionStomp(@Payload GlobalMissionStartCommandDTO command) {
        try {
            MissionCommandResponseDTO response = missionExtensionService.startGlobalMission(
                command.globalMissionName(),
                command.machineOverrides());
            messagingTemplate.convertAndSend("/topic/mission-extension/configuration",
                missionExtensionService.getConfiguration());
            messagingTemplate.convertAndSend("/topic/mission-extension/command-response", response);
        } catch (IllegalArgumentException ex) {
            messagingTemplate.convertAndSend("/topic/mission-extension/command-response",
                new MissionCommandResponseDTO(ex.getMessage(), null, null, null));
        }
    }

    @MessageMapping("/mission-extension/global/override")
    public void setGlobalMissionMachineOverrideStomp(@Payload GlobalMissionMachineOverrideCommandDTO command) {
        try {
            MissionCommandResponseDTO response = missionExtensionService.setGlobalMissionMachineOverride(
                command.machineName(),
                command.missionName());
            messagingTemplate.convertAndSend("/topic/mission-extension/configuration",
                missionExtensionService.getConfiguration());
            messagingTemplate.convertAndSend("/topic/mission-extension/command-response", response);
        } catch (IllegalArgumentException ex) {
            messagingTemplate.convertAndSend("/topic/mission-extension/command-response",
                new MissionCommandResponseDTO(ex.getMessage(), command.machineName(), command.missionName(), null));
        }
    }

    @MessageMapping("/mission-extension/global/stop")
    public void stopGlobalMissionStomp() {
        MissionCommandResponseDTO response = missionExtensionService.stopGlobalMission();
        messagingTemplate.convertAndSend("/topic/mission-extension/configuration",
            missionExtensionService.getConfiguration());
        messagingTemplate.convertAndSend("/topic/mission-extension/command-response", response);
    }
}
