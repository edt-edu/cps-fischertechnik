package io.github.mbdo.factoryscada.frontend.dto;

import java.util.List;
import java.util.Map;

public record MissionLogsDTO(Map<String, List<String>> logsByMission) {}
