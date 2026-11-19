package io.github.mbdo.factoryscada.frontend.controller.ws;

import io.github.mbdo.factoryscada.domains.dynamicmission.dtos.DynamicMissionDTO;
import io.github.mbdo.factoryscada.frontend.WebSocketDoc;
import io.github.mbdo.factoryscada.service.dynamicmission.DynamicMissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Controller
@MessageMapping("/dynamic-mission")
public class DynamicMissionWsController {

  private final DynamicMissionService dynamicMissionService;

  @Autowired
  public DynamicMissionWsController(DynamicMissionService dynamicMissionService) {
    this.dynamicMissionService = dynamicMissionService;
  }

  @MessageMapping("/missions")
  @SendTo("/topic/dynamic-missions")
  @WebSocketDoc(description = "Returns a list of all available dynamic missions.",
                responseDescription = "A list of dynamic missions with their names and descriptions and involved machines.")
  public List<DynamicMissionDTO> getMissions() {
    return dynamicMissionService.getMissionDTOs();
  }

  @MessageMapping("/command/start/{missionName}")
  @WebSocketDoc(description = "Starts a dynamic mission by its name.",
                  indirectOutputs = { })
  public void startMission(@DestinationVariable("missionName") String encodedMissionName) {
    var missionName = UriUtils.decode(encodedMissionName, StandardCharsets.UTF_8);
    dynamicMissionService.startMissionByName(missionName);
  }

  @MessageMapping("/command/stop")
  @WebSocketDoc(description = "Stops the currently active dynamic mission.",
                  indirectOutputs = { })
  public void stopMission() {
    dynamicMissionService.stopActiveMission();
  }

  @MessageMapping("**")
  public void handleUnmappedMessage(@Header("simpDestination") String destination,
                                    @Payload(required = false) String messageContent) {
    log.error("Unmapped message: destination={} payload={}", destination, messageContent);
  }
}
