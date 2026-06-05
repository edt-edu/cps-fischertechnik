package io.github.mbdo.factoryscada.socket;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Class to handle socket communication using threads for sending and receiving messages.
 */
@Getter
@Slf4j
public class SocketProtocol implements Protocol {

    // Constant for reconnection delay
    private static final int RECONNECT_DELAY_MS = 2_000;

    // Connection information
    private final String hostname;
    private final int sendPort;
    private final int receivePort;

    // BlockingQueue for thread-safe message management
    @JsonIgnore
    private final BlockingQueue<String> sendQueue;
    @JsonIgnore
    private final BlockingQueue<String> receiveQueue;

    // Threads for sending and receiving
    @JsonIgnore
    private volatile Thread sendThread;
    @JsonIgnore
    private volatile Thread receiveThread;

    // Message listener callback
    @JsonIgnore
    private MessageListener messageListener;

    // Connecton Status listener callback
    @JsonIgnore
    private List<ConnectionStatusListener> connectionStatusListeners = new ArrayList<ConnectionStatusListener>();;
    
    // connection status
    @JsonIgnore
    private boolean sendChannelConnected = false;
    @JsonIgnore
    private boolean receiveChannelConnected = false;
    
    @JsonIgnore
    private boolean forceReconnection = false;

    /**
     * Constructor to initialize connection information.
     *
     * @param hostname    The server hostname.
     * @param sendPort    The port for sending messages.
     * @param receivePort The port for receiving messages.
     */
    public SocketProtocol(String hostname, int sendPort, int receivePort) {
        this.hostname = hostname;
        this.sendPort = sendPort;
        this.receivePort = receivePort;
        sendQueue = new LinkedBlockingQueue<>();
        receiveQueue = new LinkedBlockingQueue<>();
    }

    /**
     * Constructor to initialize connection information.
     *
     * @param hostname        The server hostname.
     * @param sendPort        The port for sending messages.
     * @param receivePort     The port for receiving messages.
     * @param messageListener The listener to be called when a message is received.
     * @param connectionStatusListener The listener to be called when connection status change.
     */
    public SocketProtocol(String hostname, int sendPort, int receivePort, MessageListener messageListener, ConnectionStatusListener connectionStatusListener) {
        this.hostname = hostname;
        this.sendPort = sendPort;
        this.receivePort = receivePort;
        this.messageListener = messageListener;
        this.connectionStatusListeners.add(connectionStatusListener);
        sendQueue = new LinkedBlockingQueue<>();
        receiveQueue = new LinkedBlockingQueue<>();
    }
    
    public void addListener(ConnectionStatusListener connectionStatusListener) {
    	this.connectionStatusListeners.add(connectionStatusListener);
    }

    /**
     * Handles sending messages by connecting to the socket and sending messages from the sendQueue.
     */
    private void handleSend() {
        while (!Thread.currentThread().isInterrupted()) {
            try (Socket socket = new Socket(hostname, sendPort);) {
            	OutputStream out = socket.getOutputStream();
            	
                log.info("Connected to send socket at {}:{}", hostname, sendPort);
                this.setSendChannelConnected(true);
                while (!Thread.currentThread().isInterrupted()) {
                    String message = sendQueue.take();
                    out.write(message.getBytes());
                    out.write("\n".getBytes());
                    out.flush();
                    log.info("Message sent to {}:{}: {}", hostname, sendPort, message);
                }
                this.setSendChannelConnected(false);
            } catch (IOException | InterruptedException e) {
            	this.setSendChannelConnected(false);
                handleException(e, "sending", sendPort);
            }
        }
    }

    /**
     * Handles receiving messages by connecting to the socket and reading messages into the receiveQueue.
     */
    private void handleReceive() {
        while (!Thread.currentThread().isInterrupted()) {
            try (Socket socket = new Socket(hostname, receivePort);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                log.info("Connected to receive socket at {}:{}", hostname, receivePort);
                this.setReceiveChannelConnected(true);
                String message;
                while ((message = reader.readLine()) != null && !Thread.currentThread().isInterrupted()) {
                    receiveQueue.add(message);
                    log.info("Message received from {}:{}: {}", hostname, receivePort, message);

                    // Notify the listener
                    if (messageListener != null) {
                        messageListener.onMessageReceived(message);
                    }
                }

                this.setReceiveChannelConnected(false);

            } catch (IOException e) {

                this.setReceiveChannelConnected(false);
                
                // if reception socket is closed, need to close send socket too and try to reconnected everything
                send("WATCHDOG\n");
                handleException(e, "receiving", receivePort);
            }
        }
    }

    /**
     * Handles socket exceptions, including reconnection attempts.
     *
     * @param e      The exception that occurred.
     * @param action The action being performed (sending or receiving).
     * @param port   The port on which the exception occurred.
     */
    private void handleException(Exception e, String action, int port) {
        if (e instanceof InterruptedException) {
            log.error("Thread interrupted while waiting for {} on {}: {}: {}", action, hostname, port, e.getMessage());
            Thread.currentThread().interrupt();
        } else {
            log.warn("Failed while {} message on {}:{} : {}", action, hostname, port, e.getMessage());
            try {
                Thread.sleep(RECONNECT_DELAY_MS);
            } catch (InterruptedException ex) {
                log.error("Thread interrupted while waiting for reconnection to {}: {}: {}", hostname, port, ex.getMessage());
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Sends a message by adding it to the sendQueue.
     *
     * @param message The message to send.
     */
    @Override
    public void send(String message) {
        sendQueue.add(message);
    }

    /**
     * Receives a message by taking it from the receiveQueue.
     *
     * @return The received message.
     */
    @Override
    public String receive() {
        try {
            return receiveQueue.take();
        } catch (InterruptedException e) {
            log.error("Thread interrupted while waiting for message reception on {}:{} : {}", hostname, receivePort, e.getMessage());
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for message", e);
        }
    }

    /**
     * Starts the send and receive threads.
     */
    @Override
    public void start() {
        sendThread = new Thread(this::handleSend);
        receiveThread = new Thread(this::handleReceive);
        sendThread.start();
        receiveThread.start();
    }

    /**
     * Stops the send and receive threads by interrupting them.
     */
    @Override
    public void stop() {
        if (sendThread != null) {
            sendThread.interrupt();
        }
        if (receiveThread != null) {
            receiveThread.interrupt();
        }
    }
    @Override
	public boolean isSendChannelConnected() {
		return this.sendChannelConnected;
	}
	@Override
	public boolean isReceivedChannelConnected() {
		return this.receiveChannelConnected;
	}

	// set and potentially notify changes to listener
	synchronized private void setSendChannelConnected(boolean sendChannelConnected) {
		// Notify the listeners		
    	if(this.sendChannelConnected != sendChannelConnected) {
    		this.sendChannelConnected = sendChannelConnected;
    		log.debug("Notify sendChannelConnected to " + connectionStatusListeners.size());
    		for (ConnectionStatusListener connectionStatusListener : connectionStatusListeners) {
    			connectionStatusListener.onConnectionStatusChanged(sendChannelConnected, this.receiveChannelConnected);	
			}
    	}
	}

	// set and potentially notify changes to listener
	synchronized private void setReceiveChannelConnected(boolean receiveChannelConnected) {
		// Notify the listeners
    	if(this.receiveChannelConnected != receiveChannelConnected) {
    		this.receiveChannelConnected = receiveChannelConnected;
    		log.debug("Notify setReceiveChannelConnected to {}", connectionStatusListeners.size());
    		for (ConnectionStatusListener connectionStatusListener : connectionStatusListeners) {
    			connectionStatusListener.onConnectionStatusChanged(this.sendChannelConnected, receiveChannelConnected);	
			}    		
    	}
	}
}
