package io.github.mbdo.factoryscada.frontend.dto.replanning;

/**
 * Replaces the mission running on a machine.
 *
 * @param machine logical name of the machine
 * @param mission the mission to start
 * @param state   the state to start it in, or null to start it from its initial state (running its entry action)
 * @param expectedMission if not null, the swap is refused unless this mission runs on the machine ("" for none)
 * @param expectedState   if not null, the swap is refused unless the running mission is in this state
 */
public record MissionSwapRequestDTO(String machine, String mission, String state, String expectedMission,
                                    String expectedState) {
}
