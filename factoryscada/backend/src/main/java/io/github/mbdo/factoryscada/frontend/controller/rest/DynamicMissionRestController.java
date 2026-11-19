package io.github.mbdo.factoryscada.frontend.controller.rest;

import io.github.mbdo.factoryscada.frontend.RestDoc;
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
  @RestDoc(description = "Starts a dynamic mission by its (URL encoded) name. Answers 404 if the mission is unknown.",
      responseDescription = "202 Accepted: the mission is started asynchronously.")
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
  @RestDoc(description = "Stops the currently active dynamic mission.",
      responseDescription = "202 Accepted: the mission is stopped asynchronously.")
  public ResponseEntity<Void> stopMission() {
    dynamicMissionService.stopActiveMission();
    return ResponseEntity.accepted().build();
  }
}
