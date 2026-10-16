package io.github.mbdo.factoryscada.frontend.controller;

import io.github.mbdo.factoryscada.frontend.dto.replanning.MissionChangeDTO;
import io.github.mbdo.factoryscada.frontend.dto.replanning.MachineCommandRequestDTO;
import io.github.mbdo.factoryscada.frontend.dto.replanning.MachineCommandResultDTO;
import io.github.mbdo.factoryscada.frontend.dto.replanning.MachineRequestDTO;
import io.github.mbdo.factoryscada.frontend.dto.replanning.MissionSwapRequestDTO;
import io.github.mbdo.factoryscada.frontend.dto.replanning.ReplanningMachineDTO;
import io.github.mbdo.factoryscada.service.ReplanningService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST API of the replanning (the digital twin) on the mission extension. Machines are named by their logical name in
 * the mission extension (e.g. "REN_MISSION_01/VGR01", "ZoneCB"), given in the request bodies since they contain '/'.
 */
@RestController
@RequestMapping("/api/replanning")
public class ReplanningController {

    private final ReplanningService replanningService;

    public ReplanningController(ReplanningService replanningService) {
        this.replanningService = replanningService;
    }

    /** Every machine, the missions it can run (with their states), and the mission and state it runs. */
    @GetMapping("/machines")
    public List<ReplanningMachineDTO> getMachines() {
        return replanningService.getMachines();
    }

    /**
     * Replaces the mission running on a machine, the new one starting from its initial state or in a given state.
     * Answers 409 if the machine doesn't run the expected mission or state.
     */
    @PostMapping("/mission/swap")
    public MissionChangeDTO swapMission(@RequestBody MissionSwapRequestDTO request) {
        return replanningService.swapMission(request);
    }

    /**
     * Stops the mission running on a machine (the command it executes, if any, is not interrupted), and returns the
     * mission and state it was stopped in. Answers 409 if the machine doesn't run the expected mission or state.
     */
    @PostMapping("/mission/stop")
    public MissionChangeDTO stopMission(@RequestBody MachineRequestDTO request) {
        return replanningService.stopMission(request);
    }

    /** Sends a command to a machine running no mission, and waits for its feedback. */
    @PostMapping("/command")
    public MachineCommandResultDTO executeCommand(@RequestBody MachineCommandRequestDTO request) {
        return replanningService.executeCommand(request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> conflict(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", String.valueOf(e.getMessage())));
    }
}
