package org.cache.network.tcp.connection;

import java.util.function.Function;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class ClientConnectionHandler implements Runnable {

    private final Socket socket;
    private final Function<List<String>, String> commandProcessor;

    public ClientConnectionHandler(Socket socket, Function<List<String>, String> commandProcessor) {
        this.socket = socket;
        this.commandProcessor = commandProcessor;
    }

    @Override
    public void run() {
        try (socket) {
            var protocolConnection = new AutoProtocolConnection(socket);

            List<String> command;
            while ((command = protocolConnection.readCommand()) != null) {
                String response = commandProcessor.apply(command);
                protocolConnection.write(response);
            }

        } catch (IOException exception) {
            System.err.println("Client connection failed: " + exception.getMessage());
        }
    }
}
