package io.github.mbdo.factoryscada.socket;

import io.github.mbdo.factoryscada.socket.exception.ProtocolException;

/**
 * Interface to define the communication protocol via socket.
 */
public interface Protocol {
    /**
     * Sends a message.
     *
     * @param message The message to be sent.
     * @throws ProtocolException If there is an error during sending.
     */
    void send(String message) throws ProtocolException;

    /**
     * Receives a message.
     *
     * @return The received message.
     * @throws ProtocolException If there is an error during receiving.
     */
    String receive() throws ProtocolException;

    /**
     * Starts the protocol, initializing any necessary resources.
     *
     * @throws ProtocolException If there is an error during startup.
     */
    void start() throws ProtocolException;

    /**
     * Stops the protocol, releasing any held resources.
     *
     * @throws ProtocolException If there is an error during shutdown.
     */
    void stop() throws ProtocolException;
    
    boolean isSendChannelConnected();
    boolean isReceivedChannelConnected();
}
