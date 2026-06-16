package io.github.mbdo.factoryscada.frontend.dto;

import java.util.List;

public record MissionExtensionConfigurationDTO(
    List<MachineMissionExtensionDTO> missions,
    List<MissionMachineDTO> machines,
    List<GlobalMissionDTO> globalMissions,
    String activeGlobalMissionName,
    List<String> activeMissionNames) {
}
