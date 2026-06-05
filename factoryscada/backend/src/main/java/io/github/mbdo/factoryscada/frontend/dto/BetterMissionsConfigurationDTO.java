package io.github.mbdo.factoryscada.frontend.dto;

import java.util.List;

public record BetterMissionsConfigurationDTO(
        String name,
        List<BetterMissionDTO> missions,
        List<BetterMissionMachineDTO> machines,
        List<GlobalMissionDTO> globalMissions,
        String activeGlobalMissionName,
        List<String> activeMissionNames) {
}
