package io.github.mbdo.factoryscada.frontend.controller;

import io.github.mbdo.factoryscada.frontend.dto.BetterMissionsConfigurationDTO;
import io.github.mbdo.factoryscada.frontend.dto.GlobalMissionMachineOverrideCommandDTO;
import io.github.mbdo.factoryscada.frontend.dto.GlobalMissionStartCommandDTO;
import io.github.mbdo.factoryscada.frontend.dto.MissionExecutionCommandDTO;
import io.github.mbdo.factoryscada.frontend.dto.MissionCommandResponseDTO;
import io.github.mbdo.factoryscada.service.BetterMissionService;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RestController
@RequestMapping("/api/better-missions")
public class BetterMissionController {

    private final BetterMissionService betterMissionService;
    private final SimpMessagingTemplate messagingTemplate;

    public BetterMissionController(BetterMissionService betterMissionService, SimpMessagingTemplate messagingTemplate) {
        this.betterMissionService = betterMissionService;
        this.messagingTemplate = messagingTemplate;
    }

    @GetMapping("/configuration")
    public BetterMissionsConfigurationDTO getConfiguration() {
        return betterMissionService.getConfiguration();
    }

    @PostMapping("/start")
    public MissionCommandResponseDTO startMission(
            @org.springframework.web.bind.annotation.RequestBody MissionExecutionCommandDTO command) {
        try {
            return betterMissionService.startMission(command);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }
    }

    @PostMapping("/{machineName}/stop")
    public MissionCommandResponseDTO stopMission(@PathVariable String machineName) {
        return betterMissionService.stopMission(machineName);
    }

    @PostMapping("/stop-active")
    public MissionCommandResponseDTO stopActiveMission() {
        return betterMissionService.stopActiveMission();
    }

    // STOMP Endpoints for WebSocket subscribers
    @MessageMapping("/better-missions/configuration")
    public void getConfigurationStomp() {
        BetterMissionsConfigurationDTO configuration = betterMissionService.getConfiguration();
        messagingTemplate.convertAndSend("/topic/better-missions/configuration", configuration);
    }

    @MessageMapping("/better-missions/start")
    public void startMissionStomp(@Payload MissionExecutionCommandDTO command) {
        try {
            MissionCommandResponseDTO response = betterMissionService.startMission(command);
            messagingTemplate.convertAndSend("/topic/better-missions/configuration",
                    betterMissionService.getConfiguration());
            messagingTemplate.convertAndSend("/topic/better-missions/command-response", response);
        } catch (IllegalArgumentException ex) {
            messagingTemplate.convertAndSend("/topic/better-missions/command-response",
                    new MissionCommandResponseDTO(ex.getMessage(), command.machineName(), command.missionName(), null));
        }
    }

    @MessageMapping("/better-missions/stop")
    public void stopMissionStomp(@Payload MissionExecutionCommandDTO command) {
        MissionCommandResponseDTO response = betterMissionService.stopMission(command.machineName());
        messagingTemplate.convertAndSend("/topic/better-missions/configuration",
                betterMissionService.getConfiguration());
        messagingTemplate.convertAndSend("/topic/better-missions/command-response", response);
    }

    @MessageMapping("/better-missions/stop-active")
    public void stopActiveMissionStomp() {
        MissionCommandResponseDTO response = betterMissionService.stopActiveMission();
        messagingTemplate.convertAndSend("/topic/better-missions/configuration",
                betterMissionService.getConfiguration());
        messagingTemplate.convertAndSend("/topic/better-missions/command-response", response);
    }

    // Global Mission STOMP Endpoints
    @MessageMapping("/better-missions/global/start")
    public void startGlobalMissionStomp(@Payload GlobalMissionStartCommandDTO command) {
        try {
            MissionCommandResponseDTO response = betterMissionService.startGlobalMission(
                    command.globalMissionName(),
                    command.machineOverrides());
            messagingTemplate.convertAndSend("/topic/better-missions/configuration",
                    betterMissionService.getConfiguration());
            messagingTemplate.convertAndSend("/topic/better-missions/command-response", response);
        } catch (IllegalArgumentException ex) {
            messagingTemplate.convertAndSend("/topic/better-missions/command-response",
                    new MissionCommandResponseDTO(ex.getMessage(), null, null, null));
        }
    }

    @MessageMapping("/better-missions/global/override")
    public void setGlobalMissionMachineOverrideStomp(@Payload GlobalMissionMachineOverrideCommandDTO command) {
        try {
            MissionCommandResponseDTO response = betterMissionService.setGlobalMissionMachineOverride(
                    command.machineName(),
                    command.missionName());
            messagingTemplate.convertAndSend("/topic/better-missions/configuration",
                    betterMissionService.getConfiguration());
            messagingTemplate.convertAndSend("/topic/better-missions/command-response", response);
        } catch (IllegalArgumentException ex) {
            messagingTemplate.convertAndSend("/topic/better-missions/command-response",
                    new MissionCommandResponseDTO(ex.getMessage(), command.machineName(), command.missionName(), null));
        }
    }

    @MessageMapping("/better-missions/global/stop")
    public void stopGlobalMissionStomp() {
        MissionCommandResponseDTO response = betterMissionService.stopGlobalMission();
        messagingTemplate.convertAndSend("/topic/better-missions/configuration",
                betterMissionService.getConfiguration());
        messagingTemplate.convertAndSend("/topic/better-missions/command-response", response);
    }
}
