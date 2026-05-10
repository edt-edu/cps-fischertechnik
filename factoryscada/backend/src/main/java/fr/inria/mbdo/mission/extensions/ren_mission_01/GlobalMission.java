package fr.inria.mbdo.mission.extensions.ren_mission_01;

import java.util.Map;

/**
 * Defines a high-level global mission that orchestrates missions across all
 * machines.
 * Each machine gets a default mission, but can be overridden per-machine.
 */
public record GlobalMission(
        String name,
        String description,
        Map<String, String> machineDefaultMissions) {
}
