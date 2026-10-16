package io.github.mbdo.factoryscada.frontend.dto;

import java.util.List;

public record MachineMissionExtensionDTO(
    String name,
    String description,
    String activeState,
    boolean awaitingCommandResult,
    List<String> involvedMachines,
    String dotGraph) {
}
