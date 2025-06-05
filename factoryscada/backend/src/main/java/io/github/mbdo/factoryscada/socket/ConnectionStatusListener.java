package io.github.mbdo.factoryscada.socket;

/**
 * interface to be notified when connection status changes.
 */
@FunctionalInterface
public interface ConnectionStatusListener {

    void onConnectionStatusChanged(boolean sendChannelConnected, boolean receivedChannelConnected);

}
