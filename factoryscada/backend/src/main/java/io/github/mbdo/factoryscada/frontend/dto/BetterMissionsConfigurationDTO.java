package io.github.mbdo.factoryscada.frontend.dto;

import java.util.List;
import java.util.Map;

public record BetterMissionsConfigurationDTO(
                String name,
                List<BetterMissionDTO> missions,
                List<BetterMissionMachineDTO> machines,
                List<GlobalMissionDTO> globalMissions,
                String activeGlobalMissionName,
                Map<String, String> globalMissionMachineOverrides) {
}
