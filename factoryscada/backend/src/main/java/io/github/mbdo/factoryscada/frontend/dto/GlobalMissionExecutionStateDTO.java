package io.github.mbdo.factoryscada.frontend.dto;

import java.util.Map;

/**
 * Represents the active global mission and any per-machine mission overrides.
 */
public record GlobalMissionExecutionStateDTO(
        String activeGlobalMissionName,
        Map<String, String> machineMissionOverrides) {
}
