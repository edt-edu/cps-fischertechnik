package io.github.mbdo.factoryscada.frontend.controller;

import io.github.mbdo.factoryscada.domains.dynamicmission.dtos.DynamicMissionDTO;
import io.github.mbdo.factoryscada.service.dynamicmission.DynamicMissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Controller
@MessageMapping("/dynamic-mission")
public class DynamicMissionOrchestratorController {

  private final DynamicMissionService dynamicMissionService;

  @Autowired
  public DynamicMissionOrchestratorController(DynamicMissionService dynamicMissionService) {
    this.dynamicMissionService = dynamicMissionService;
  }

  @MessageMapping("/topic/missions")
  public List<DynamicMissionDTO> getMissions() {
    return dynamicMissionService.getMissionDTOs();
  }

  @MessageMapping("/command/start/{missionName}")
  public void startMission(@DestinationVariable("missionName") String encodedMissionName) {
    var missionName = UriUtils.decode(encodedMissionName, StandardCharsets.UTF_8);
    dynamicMissionService.startMissionByName(missionName);
  }

  @MessageMapping("/command/stop")
  public void stopMission() {
    dynamicMissionService.stopActiveMission();
  }

  @MessageMapping("**")
  public void handleUnmappedMessage(@Header("simpDestination") String destination,
                                    @Payload(required = false) String messageContent) {
    log.error("Unmapped message: destination={} payload={}", destination, messageContent);
  }
}
