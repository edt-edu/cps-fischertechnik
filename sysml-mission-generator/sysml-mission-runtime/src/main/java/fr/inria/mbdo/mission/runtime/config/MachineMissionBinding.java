package fr.inria.mbdo.mission.runtime.config;

import java.util.List;

/**
 * Binds a specific machine instance to the mission templates it can execute.
 *
 * <p>
 * The runtime uses these bindings to determine which missions are valid for a
 * given machine, and to expose them in the UI for operator selection.
 *
 * <h2>Current bindings (ren_mission_01 platform)</h2>
 * <table>
 * <tr>
 * <th>Machine Name</th>
 * <th>Machine Type</th>
 * <th>Available Missions</th>
 * </tr>
 * <tr>
 * <td>ConveyorBelt</td>
 * <td>ConveyorBeltMachine</td>
 * <td>ConveyorBeltNominalMission</td>
 * </tr>
 * <tr>
 * <td>SortingLine</td>
 * <td>SortingLineMachine</td>
 * <td>SortingLineNominalMission</td>
 * </tr>
 * <tr>
 * <td>MultiProcessingStation</td>
 * <td>MultiProcessingStationMachine</td>
 * <td>MultiProcessingStationNominalMission</td>
 * </tr>
 * <tr>
 * <td>VacuumGripper1</td>
 * <td>VacuumGripperMachine</td>
 * <td>VacuumGripper1NominalMission</td>
 * </tr>
 * <tr>
 * <td>VacuumGripper2</td>
 * <td>VacuumGripperMachine</td>
 * <td>VacuumGripper2NominalMission</td>
 * </tr>
 * <tr>
 * <td>ZoneCB</td>
 * <td>Zone</td>
 * <td>ZoneMissionCBNominal</td>
 * </tr>
 * </table>
 *
 * @param machineName  unique name identifying the machine instance
 * @param machineType  interface type (e.g. "ConveyorBeltMachine",
 *                     "VacuumGripperMachine")
 * @param missionNames list of mission template names this machine can run
 */
public record MachineMissionBinding(
        String machineName,
        String machineType,
        List<String> missionNames) {
}
