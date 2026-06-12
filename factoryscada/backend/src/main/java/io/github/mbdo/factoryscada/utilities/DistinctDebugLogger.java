package io.github.mbdo.factoryscada.utilities;

import org.slf4j.Logger;
import org.slf4j.helpers.MessageFormatter;

import java.util.Objects;

/**
 * Helper logger where repeated debug messages are skipped, useful in big reactive loop that will log only new messages
 * Any new message reset the print of debug messages
 * Preserve lazy string formatting capability
 * - Consecutive identical DEBUG messages → suppress duplicates
 * - Any INFO/WARN/ERROR → always log and reset the debug memory
 * - A different DEBUG → log and replace the remembered debug message
 */
public class DistinctDebugLogger {

    private final Logger logger;
    private String previousDebugMessage;

    public DistinctDebugLogger(Logger logger) {
        this.logger = logger;
    }

    public void debug(String format, Object... args) {
        if (!logger.isDebugEnabled()) {
            return;
        }

        String message = MessageFormatter
                .arrayFormat(format, args)
                .getMessage();

        if (Objects.equals(message, previousDebugMessage)) {
            return;
        }

        previousDebugMessage = message;
        logger.debug(format, args);
    }

    public void info(String format, Object... args) {
        previousDebugMessage = null; // reset
        logger.info(format, args);
    }

    public void warn(String format, Object... args) {
        previousDebugMessage = null; // reset
        logger.warn(format, args);
    }

    public void error(String format, Object... args) {
        previousDebugMessage = null; // reset
        logger.error(format, args);
    }
}