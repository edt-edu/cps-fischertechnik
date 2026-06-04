package fr.inria.mbdo.mission.runtime.config;

import java.util.List;

/**
 * Top-level catalog that aggregates all mission configuration for the
 * ren_mission_01 platform.
 *
 * <p>
 * This record is the single entry point consumed by
 * {@code MissionExtensionRegistry} to
 * discover and manage mission execution. It is assembled in
 * {@code ExtensionMissionAutoConfig} and exposed as a Spring bean.
 *
 * <h2>Structure</h2>
 * <ul>
 * <li>{@link #missionTemplates()} — all available mission scenarios (state
 * machines)</li>
 * <li>{@link #machineBindings()} — which missions can run on which
 * machines</li>
 * <li>{@link #globalMissions()} — high-level orchestrations assigning missions
 * to all machines</li>
 * </ul>
 *
 * <h2>Platform overview (ren_mission_01)</h2>
 * <p>
 * The physical platform consists of:
 * <ul>
 * <li><b>ConveyorBelt</b> — belt transport system with feed/swap sensors</li>
 * <li><b>SortingLine</b> — color-based token detection and ejection</li>
 * <li><b>MultiProcessingStation</b> — oven, saw, arm, turntable processing</li>
 * <li><b>VacuumGripper1</b> — 3-axis gripper serving SortingLine →
 * ConveyorBelt</li>
 * <li><b>VacuumGripper2</b> — 3-axis gripper serving ConveyorBelt →
 * MultiProcessingStation</li>
 * <li><b>ZoneCB</b> — mutual-exclusion zone protecting the conveyor belt
 * area</li>
 * <li><b>ZoneMPS</b> — mutual-exclusion zone protecting the MPS input area</li>
 * </ul>
 *
 * @param missionTemplates all registered mission templates
 * @param machineBindings  machine-to-mission associations
 * @param globalMissions   global orchestration definitions
 */
public record MissionBindingCatalog(
        List<MissionTemplate> missionTemplates,
        List<MachineMissionBinding> machineBindings,
        List<GlobalMission> globalMissions) {
}
