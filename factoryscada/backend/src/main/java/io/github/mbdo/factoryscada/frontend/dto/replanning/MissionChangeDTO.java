package io.github.mbdo.factoryscada.frontend.dto.replanning;

/**
 * A change of the mission running on a machine.
 *
 * @param previousMission the mission that ran on the machine before the change, null if none
 * @param previousState   the state it was stopped in, null if none
 * @param mission         the mission running now, null if none
 * @param state           its state now (after its completion transitions), null if none
 */
public record MissionChangeDTO(
    String machine,
    String previousMission,
    String previousState,
    String mission,
    String state) {
}
