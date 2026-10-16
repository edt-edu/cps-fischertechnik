package io.github.mbdo.factoryscada.frontend.dto.replanning;

/**
 * @param machine         logical name of the machine
 * @param expectedMission if not null, the request is refused unless this mission runs on the machine ("" for none)
 * @param expectedState   if not null, the request is refused unless the running mission is in this state
 */
public record MachineRequestDTO(String machine, String expectedMission, String expectedState) {
}
