package io.github.mbdo.factoryscada.frontend.dto;

import java.util.Map;

public record GlobalMissionDTO(
        String name,
        String description,
        Map<String, String> machineDefaultMissions) {
}
