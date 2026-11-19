package io.github.mbdo.factoryscada.frontend.controller.rest;

import io.github.mbdo.factoryscada.frontend.RestDoc;
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
    @RestDoc(description = "Returns the current configuration of the mission extension, including the list of available missions and their statuses.",
            responseDescription = "The current configuration of the mission extension.")
    public MissionExtensionConfigurationDTO getConfiguration() {
        return missionExtensionService.getConfiguration();
    }

    @GetMapping("/logs")
    @RestDoc(description = "Returns the logs of the mission extension, including the history of executed missions and their statuses.",
            responseDescription = "The logs of the mission extension.")
    public MissionLogsDTO getLogs() {
        return missionExtensionService.getMissionLogsDTO();
    }

    @PostMapping("/start")
    @RestDoc(description = "Starts a mission extension for a given machine with the given mission name. Answers 404 if the machine or mission is unknown.",
            responseDescription = "The result of the command.")
    public MissionCommandResponseDTO startMission(
        @org.springframework.web.bind.annotation.RequestBody MissionExecutionCommandDTO command) {
        try {
            return missionExtensionService.startMission(command);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }
    }

    @PostMapping("/{missionName}/stop")
    @RestDoc(description = "Stops a mission with the given name.",
            responseDescription = "The result of the command.")
    public MissionCommandResponseDTO stopMission(@PathVariable String missionName) {
        return missionExtensionService.stopMission(missionName);
    }

    @PostMapping("/stop-active")
    @RestDoc(description = "Stops all the currently active missions (from mission extension).",
            responseDescription = "The result of the command.")
    public MissionCommandResponseDTO stopActiveMission() {
        return missionExtensionService.stopActiveMission();
    }
}
