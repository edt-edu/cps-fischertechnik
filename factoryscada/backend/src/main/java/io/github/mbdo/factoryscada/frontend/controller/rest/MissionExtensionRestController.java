package io.github.mbdo.factoryscada.frontend.controller.rest;

import io.github.mbdo.factoryscada.frontend.controller.ws.MissionExtensionWsController;
import io.github.mbdo.factoryscada.frontend.dto.*;
import io.github.mbdo.factoryscada.service.MissionExtensionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST endpoints of the mission extension, for HTTP clients.
 * <p>
 * The same features (and a few more, such as global missions) are also available through STOMP in
 * {@link MissionExtensionWsController}.
 */
@RestController
@RequestMapping("/api/mission-extension")
public class MissionExtensionRestController {

    private final MissionExtensionService missionExtensionService;

    public MissionExtensionRestController(MissionExtensionService missionExtensionService) {
        this.missionExtensionService = missionExtensionService;
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
}
