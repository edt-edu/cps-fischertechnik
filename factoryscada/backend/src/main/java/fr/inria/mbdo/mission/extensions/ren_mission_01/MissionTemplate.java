package fr.inria.mbdo.mission.extensions.ren_mission_01;

import fr.inria.mbdo.mission.runtime.api.MachineMissionStrategy;

import java.util.function.Supplier;

public record MissionTemplate(
        String name,
        String description,
        MachineMissionStrategy previewStrategy,
        Supplier<MachineMissionStrategy> factory) {
}
