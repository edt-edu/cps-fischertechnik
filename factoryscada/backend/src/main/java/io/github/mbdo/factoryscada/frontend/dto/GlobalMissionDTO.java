package io.github.mbdo.factoryscada.frontend.dto;

import java.util.List;

public record GlobalMissionDTO(
                String name,
                String description,
                List<String> missionNames) {
}
