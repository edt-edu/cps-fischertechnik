package io.github.mbdo.factoryscada.frontend.dto;

public record MissionExecutionCommandDTO(
        String machineName,
        String missionName) {
}