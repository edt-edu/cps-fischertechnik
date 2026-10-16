package io.github.mbdo.factoryscada.frontend.dto.replanning;

import java.util.List;

/**
 * A mission a machine can run.
 *
 * @param states       its states, from the initial one: a migration can start it in any of them
 * @param running      whether it runs now
 * @param activeState  its state if it runs, null otherwise
 */
public record ReplanningMissionDTO(
    String name,
    String description,
    List<String> states,
    boolean running,
    String activeState) {
}
