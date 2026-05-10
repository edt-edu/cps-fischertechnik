package io.github.mbdo.factoryscada.frontend.dto;

import java.util.List;

public record BetterMissionNodeDTO(
        String id,
        String type,
        String description,
        List<String> outputs,
        String placeholder,
        List<BetterMissionNodeDTO> outputNodes,
        boolean active) {
}
