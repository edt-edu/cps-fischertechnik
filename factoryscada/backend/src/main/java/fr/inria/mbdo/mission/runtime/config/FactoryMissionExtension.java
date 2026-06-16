package fr.inria.mbdo.mission.runtime.config;

import fr.inria.mbdo.mission.runtime.api.AbstractMissionStrategy;

import java.util.List;

/**
 * Defines a high-level global mission that orchestrates multiple missions
 * running concurrently on the ren_mission_01 physical platform.
 *
 * <p>
 * A global mission specifies which mission templates to start together.
 * Each mission internally references the machine adapters it needs — multiple
 * missions can share the same machine adapters (e.g. VacuumGripper1 is used
 * by both VacuumGripper1NominalMission and SortingLineNominalMission).
 *
 * <h2>Example</h2>
 *
 * <pre>{@code
 * new GlobalMission(
 *                 "NominalProduction",
 *                 "All machines run their nominal missions in coordination.",
 *                 List.of(
 *                                 conveyorBeltNominalMission,
 *                                 sortingLineNominalMission,
 *                                 multiProcessingStationNominalMission,
 *                                 vacuumGripper1NominalMission,
 *                                 vacuumGripper2NominalMission,
 *                                 zoneMissionCBNominal))
 * }</pre>
 *
 * @param name            unique identifier of this global mission
 * @param description     human-readable description shown in the UI
 * @param machineMissions list of machine missions to start concurrently
 */
public record FactoryMissionExtension(
    String name,
    String description,
    List<AbstractMissionStrategy> machineMissions) {
}
