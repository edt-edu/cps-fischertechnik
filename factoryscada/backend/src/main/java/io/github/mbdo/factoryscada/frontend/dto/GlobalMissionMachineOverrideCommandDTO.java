package io.github.mbdo.factoryscada.frontend.dto;

/**
 * Command to override a machine's mission within an active global mission.
 */
public record GlobalMissionMachineOverrideCommandDTO(
        String machineName,
        String missionName) {
}
