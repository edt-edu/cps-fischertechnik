package io.github.mbdo.factoryscada.service.dynamicmission;

import io.github.mbdo.factoryscada.core.AbstractMachine;
import io.github.mbdo.factoryscada.domains.dynamicmission.DynamicMission;
import io.github.mbdo.factoryscada.domains.dynamicmission.dtos.DynamicMissionDTO;
import io.github.mbdo.factoryscada.service.FactoryScada;
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
  private final FactoryScada factoryScada;

  @Autowired
  public DynamicMissionService(FactoryScada factoryScada, Demo demo, BrokenCBDemo brokenCBDemo) {
    this.factoryScada = factoryScada;

    this.missions = List.of(demo, brokenCBDemo);
    this.missionDTOs = missions
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
    //just stop all missions and their machines; should be fine for now
    for (DynamicMission dynamicMission : getMissions()) {
      dynamicMission.stop();
      dynamicMission
          .getInvolvedMachineNames()
          .forEach(machineName -> Optional
              .ofNullable(getFactoryScada().getFactoryScadaInstance().machines().get(machineName))
              .ifPresent(AbstractMachine::stop));
    }
  }

  public Optional<DynamicMission> getMissionByName(String name) {
    return missions.stream().filter(mission -> mission.getName().equals(name)).findFirst();
  }
}
