package io.github.mbdo.factoryscada.frontend.dto.replanning;

import java.util.List;

/**
 * A machine of the mission extension, as seen by the replanning (the digital twin): the missions it can run and the
 * state of the one it runs.
 *
 * @param name              logical name of the machine in the mission extension (e.g. "REN_MISSION_01/VGR01")
 * @param physicalMachine   name of the SCADA machine it is mapped to (e.g. "VacuumGripper01"), null for a zone
 * @param type              machine type (e.g. "VacuumGripperMachine", "Zone")
 * @param idle              whether the physical machine executes no command, null for a zone
 * @param activeMission     the mission running on the machine, null if none
 * @param activeState       the state of that mission, null if none
 * @param availableMissions the missions the machine can run
 */
public record ReplanningMachineDTO(
    String name,
    String physicalMachine,
    String type,
    Boolean idle,
    String activeMission,
    String activeState,
    List<ReplanningMissionDTO> availableMissions) {
}
