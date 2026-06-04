package fr.inria.mbdo.mission.runtime.config;

import java.util.Map;

/**
 * Defines a high-level global mission that orchestrates missions across all
 * machines on the ren_mission_01 physical platform.
 *
 * <p>
 * A global mission assigns a default mission template to each machine by name.
 * At runtime, any machine's assignment can be overridden through the
 * {@code /better-missions/global/override} STOMP endpoint.
 *
 * <h2>Platform machines (ren_mission_01)</h2>
 * <ul>
 * <li><b>ConveyorBelt</b> — moves workpieces between stations</li>
 * <li><b>SortingLine</b> — detects and sorts colored tokens</li>
 * <li><b>MultiProcessingStation</b> — performs multi-step processing (oven,
 * saw, arm)</li>
 * <li><b>VacuumGripper1</b> — picks tokens from SortingLine, places on
 * ConveyorBelt feed</li>
 * <li><b>VacuumGripper2</b> — transfers workpieces from ConveyorBelt to
 * MultiProcessingStation</li>
 * <li><b>ZoneCB</b> — mutual-exclusion zone for Conveyor Belt access</li>
 * </ul>
 *
 * <h2>Example</h2>
 *
 * <pre>{@code
 * new GlobalMission(
 *         "NominalProduction",
 *         "All machines run their nominal missions in coordination.",
 *         Map.of(
 *                 "ConveyorBelt", "ConveyorBeltNominalMission",
 *                 "SortingLine", "SortingLineNominalMission",
 *                 "MultiProcessingStation", "MultiProcessingStationNominalMission",
 *                 "VacuumGripper1", "VacuumGripper1NominalMission",
 *                 "VacuumGripper2", "VacuumGripper2NominalMission",
 *                 "ZoneCB", "ZoneMissionCBNominal"))
 * }</pre>
 *
 * @param name                   unique identifier of this global mission
 * @param description            human-readable description shown in the UI
 * @param machineDefaultMissions map of machine name → default mission template
 *                               name
 */
public record GlobalMission(
        String name,
        String description,
        Map<String, String> machineDefaultMissions) {
}
