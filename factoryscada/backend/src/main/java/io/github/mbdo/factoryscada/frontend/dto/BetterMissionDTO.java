package io.github.mbdo.factoryscada.frontend.dto;

import java.util.List;

public record BetterMissionDTO(
        String name,
        String description,
        String activeState,
        List<BetterMissionNodeDTO> nodes) {
}
