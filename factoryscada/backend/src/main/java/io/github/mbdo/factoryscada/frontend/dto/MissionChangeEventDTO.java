package io.github.mbdo.factoryscada.frontend.dto;

/**
 * A change of the mission running on a machine of the mission extension, as shown in the SCADA UI.
 *
 * @param time            when it happened (ISO-8601)
 * @param previousMission the mission running before, null if none
 * @param previousState   its state when it stopped, null if none
 * @param mission         the mission running after, null if none (stopped)
 * @param state           its state after the change, null if none
 * @param origin          OPERATOR (the SCADA UI) or REPLANNING (the digital twin, through /api/replanning)
 */
public record MissionChangeEventDTO(
    String time,
    String machine,
    String previousMission,
    String previousState,
    String mission,
    String state,
    String origin) {
}
