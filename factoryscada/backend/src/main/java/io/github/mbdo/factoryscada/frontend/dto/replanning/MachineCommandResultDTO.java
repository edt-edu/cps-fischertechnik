package io.github.mbdo.factoryscada.frontend.dto.replanning;

/**
 * The outcome of a {@link MachineCommandRequestDTO}.
 *
 * @param status    the PLC feedback status (DONE, ABORTED_ERROR, ABORTED_TIMEOUT, INTERRUPTED), or TIMEOUT if no
 *                  feedback arrived in time, or BUSY if the machine never became idle
 * @param done      whether the command succeeded (status DONE)
 */
public record MachineCommandResultDTO(
    String machine,
    String physicalMachine,
    String command,
    String commandId,
    String status,
    boolean done,
    long durationMs) {
}
