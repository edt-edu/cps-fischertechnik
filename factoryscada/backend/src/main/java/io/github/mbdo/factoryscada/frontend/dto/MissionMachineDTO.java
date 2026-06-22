package io.github.mbdo.factoryscada.frontend.dto;

import java.util.List;

public record MissionMachineDTO(
    String name,
    String type,
    List<MissionExtensionMachineOptionDTO> missions,
    String activeMissionName) {
}
