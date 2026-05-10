package io.github.mbdo.factoryscada.frontend.dto;

import java.util.Map;

/**
 * Command to start a global mission with optional per-machine mission
 * overrides.
 */
public record GlobalMissionStartCommandDTO(
        String globalMissionName,
        Map<String, String> machineOverrides) {
}
