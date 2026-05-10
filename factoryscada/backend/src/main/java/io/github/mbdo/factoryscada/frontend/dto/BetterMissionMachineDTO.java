package io.github.mbdo.factoryscada.frontend.dto;

import java.util.List;

public record BetterMissionMachineDTO(
        String name,
        String type,
        List<BetterMissionOptionDTO> missions,
        String activeMissionName) {
}