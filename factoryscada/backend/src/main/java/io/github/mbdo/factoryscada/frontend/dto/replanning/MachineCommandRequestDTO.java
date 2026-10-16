package io.github.mbdo.factoryscada.frontend.dto.replanning;

import io.github.mbdo.factoryscada.core.dtos.Parameter;

import java.util.List;

/**
 * A command sent directly to a machine, outside of any mission (e.g. to reconcile its state with the state of the
 * mission it migrates to).
 *
 * @param machine    logical name of the machine
 * @param command    command name, in snake or camel case (e.g. "retract_arm", "moveToSensor", "PICK")
 * @param parameters its parameters, as in a SCADA command payload (e.g. a NAMEDPOSITION)
 * @param timeoutMs  how long to wait for the machine to be idle, then for the command feedback (default 60 s)
 */
public record MachineCommandRequestDTO(
    String machine,
    String command,
    List<Parameter> parameters,
    Long timeoutMs) {
}
