package fr.inria.mbdo.mission.runtime.config;

import fr.inria.mbdo.mission.runtime.api.MachineMissionStrategy;

import java.util.function.Supplier;

/**
 * Describes a reusable mission scenario that can be instantiated at runtime.
 *
 * <p>
 * Each template holds a static {@link #previewStrategy} for UI introspection
 * (state names,
 * transitions) and a {@link #factory} that produces fresh, independent
 * instances when the
 * mission is actually started.
 *
 * <h2>Registered templates for ren_mission_01</h2>
 * <table>
 * <tr>
 * <th>Name</th>
 * <th>Machine(s)</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>ConveyorBeltNominalMission</td>
 * <td>ConveyorBelt, VacuumGripper1, VacuumGripper2</td>
 * <td>Moves items to sensor, notifies grippers on completion.</td>
 * </tr>
 * <tr>
 * <td>SortingLineNominalMission</td>
 * <td>SortingLine, VacuumGripper1</td>
 * <td>Sorts tokens by color, publishes color availability events.</td>
 * </tr>
 * <tr>
 * <td>MultiProcessingStationNominalMission</td>
 * <td>MultiProcessingStation, ZoneMPS</td>
 * <td>Acquires MPS zone, runs processing sequence, broadcasts completion.</td>
 * </tr>
 * <tr>
 * <td>VacuumGripper1NominalMission</td>
 * <td>VacuumGripper1, SortingLine, ZoneCB</td>
 * <td>Picks colored tokens and places them on conveyor belt feed.</td>
 * </tr>
 * <tr>
 * <td>VacuumGripper2NominalMission</td>
 * <td>VacuumGripper2, ZoneCB, ZoneMPS</td>
 * <td>Transfers workpieces from conveyor belt to multi-processing station.</td>
 * </tr>
 * <tr>
 * <td>ZoneMissionCBNominal</td>
 * <td>VacuumGripper1, VacuumGripper2</td>
 * <td>Manages mutual exclusion for the conveyor belt zone.</td>
 * </tr>
 * </table>
 *
 * @param name            unique identifier matching the value used in
 *                        {@link MachineMissionBinding}
 * @param description     human-readable description for UI display
 * @param previewStrategy a pre-built instance used for state-machine
 *                        introspection (read-only)
 * @param factory         supplier that creates a fresh mission instance for
 *                        execution
 */
public record MissionTemplate(
        String name,
        String description,
        MachineMissionStrategy previewStrategy,
        Supplier<MachineMissionStrategy> factory) {
}
