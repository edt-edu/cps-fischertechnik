package io.github.mbdo.factoryscada.frontend.dto.replanning;

/**
 * The mission or state of a machine isn't the one a replanning request expected: the machine moved on since the
 * replanning read it.
 */
public class MissionStateMismatchException extends IllegalStateException {
    public MissionStateMismatchException(String message) {
        super(message);
    }
}
