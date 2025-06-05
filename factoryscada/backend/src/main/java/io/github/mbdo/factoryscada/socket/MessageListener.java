package io.github.mbdo.factoryscada.socket;

/**
 * Functional interface for message received callback.
 */
@FunctionalInterface
public interface MessageListener {

    void onMessageReceived(String message);

}
