package io.github.mbdo.factoryscada.frontend.dto;

public record MissionCommandResponseDTO(
        String message,
        String machineName,
        String missionName,
        String activeMissionName) {
}
