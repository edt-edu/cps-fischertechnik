package io.github.mbdo.factoryscada.frontend.controller;

import io.github.mbdo.factoryscada.service.dynamicmission.DynamicMissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@RequestMapping("/api/dynamic-mission")
public class DynamicMissionRestController {

  private final DynamicMissionService dynamicMissionService;

  @Autowired
  public DynamicMissionRestController(DynamicMissionService dynamicMissionService) {
    this.dynamicMissionService = dynamicMissionService;
  }

  @PostMapping("/command/start/{missionName}")
  public ResponseEntity<Void> startMission(@PathVariable("missionName") String encodedMissionName) {
    var missionName = UriUtils.decode(encodedMissionName, StandardCharsets.UTF_8);
    if (dynamicMissionService.getMissionByName(missionName).isEmpty()) {
      log.warn("Cannot start unknown dynamic mission {}", missionName);
      return ResponseEntity.notFound().build();
    }

    dynamicMissionService.startMissionByName(missionName);
    return ResponseEntity.accepted().build();
  }

  @PostMapping("/command/stop")
  public ResponseEntity<Void> stopMission() {
    dynamicMissionService.stopActiveMission();
    return ResponseEntity.accepted().build();
  }
}
