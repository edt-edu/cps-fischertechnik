package io.github.mbdo.factoryscada.socket;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SocketProtocolTest {

    private static final String HOSTNAME = "localhost";
    private static final int RECEIVE_PORT = 5001;
    private static final int SEND_PORT = 5002;

    private ServerSocket sendServerSocket;
    private ServerSocket receiveServerSocket;

    private SocketProtocol socketProtocol;

    /**
     * Handles both sending and receiving sockets in a single thread.
     * The server receives messages on the send socket and forwards them to the received socket.
     */
    private void handleSockets() {
        try (
                Socket sendSocket = sendServerSocket.accept();
                Socket receiveSocket = receiveServerSocket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(sendSocket.getInputStream()));
                PrintWriter out = new PrintWriter(new OutputStreamWriter(receiveSocket.getOutputStream()), true)
        ) {
            String message;
            while ((message = in.readLine()) != null) {
                out.println(message); // Forward received message to the received socket
            }
        } catch (Exception e) {
            e.printStackTrace(); // Log exception for debugging
        }
    }

    @BeforeEach
    public void setUp() throws Exception {
        // Initialize server sockets
        sendServerSocket = new ServerSocket(SEND_PORT);
        receiveServerSocket = new ServerSocket(RECEIVE_PORT);

        // Initialize SocketProtocol with host and ports
        socketProtocol = new SocketProtocol(HOSTNAME, SEND_PORT, RECEIVE_PORT);
        socketProtocol.start();

        // Start the server thread to handle both send and receive sockets
        new Thread(this::handleSockets).start();
    }

    @AfterEach
    public void tearDown() throws Exception {
        // Stop the socket protocol if it is not null
        if (socketProtocol != null) {
            socketProtocol.stop();
        }

        // Close server sockets if they are open
        if (sendServerSocket != null && !sendServerSocket.isClosed()) {
            sendServerSocket.close();
        }
        if (receiveServerSocket != null && !receiveServerSocket.isClosed()) {
            receiveServerSocket.close();
        }
    }

    @Test
    public void testSendMessageAndReceiveMessage() {
        String testMessage = "Hello, World!";

        // Send message through the protocol
        socketProtocol.send(testMessage);

        // Verify that the message was received correctly
        String receivedMessage = socketProtocol.receive();
        assertEquals(testMessage, receivedMessage, "The sent message should match the received message.");
    }

    @Test
    public void testSendMultipleMessages() {
        String[] testMessages = {"Hello, World!", "Second message", "Third message"};

        // Send multiple messages through the protocol
        for (String message : testMessages) {
            socketProtocol.send(message);
        }

        // Verify that all messages were received correctly
        for (String expectedMessage : testMessages) {
            String receivedMessage = socketProtocol.receive();
            assertEquals(expectedMessage, receivedMessage, "Each sent message should match the received message.");
        }
    }

    @Test
    public void testSendAndReceiveWithInterruption() {
        String testMessage = "Hello, Interrupted World!";

        // Send message through the protocol
        socketProtocol.send(testMessage);

        // Simulate thread interruption
        Thread.currentThread().interrupt();

        // Verify that RuntimeException is thrown due to thread interruption while receiving
        assertThrows(RuntimeException.class, () -> {
            socketProtocol.receive();
        }, "Receiving a message should throw a RuntimeException if the thread is interrupted.");
    }
}
