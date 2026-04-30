package io.github.mbdo.factoryscada.service.dynamicmission;

import io.github.mbdo.factoryscada.domains.dynamicmission.DynamicMission;
import io.github.mbdo.factoryscada.domains.dynamicmission.dtos.DynamicMissionDTO;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Getter
public class DynamicMissionService {
  private final List<DynamicMission> missions;
  private final List<DynamicMissionDTO> missionDTOs;

  @Autowired
  public DynamicMissionService(Demo demo) {
    missions = List.of(demo);
    missionDTOs = missions
        .stream()
        .map(mission -> new DynamicMissionDTO(mission.getName(),
                                              mission.getDescription(),
                                              mission.getInvolvedMachineNames().stream().toList()))
        .toList();
  }

  public void startMissionByName(String missionName) {
    log.info("Attempting to start mission {}", missionName);

    //stop any active missions
    stopActiveMission();

    var mission = getMissionByName(missionName);
    if (mission.isEmpty()) {
      log.error("Cannot find a mission with name {}", missionName);
      return;
    }

    mission.get().start();
  }

  public void stopActiveMission() {
    log.info("Stopping active mission...");
    getMissions().forEach(DynamicMission::stop);
  }

  public Optional<DynamicMission> getMissionByName(String name) {
    return missions.stream().filter(mission -> mission.getName().equals(name)).findFirst();
  }
}
