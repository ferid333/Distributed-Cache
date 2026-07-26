package org.cache.network.connection;

import org.cache.protocol.CommandProcessor;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class ClientConnectionHandler implements Runnable {

    private final Socket socket;
    private final CommandProcessor<?, ?> commandProcessor;

    public ClientConnectionHandler(Socket socket, CommandProcessor<?, ?> commandProcessor) {
        this.socket = socket;
        this.commandProcessor = commandProcessor;
    }

    @Override
    public void run() {
        try (socket) {
            var connectionFactory = new ProtocolConnectionFactory(socket);
            var connection = connectionFactory.create();

            if (connection.isEmpty()) {
                return;
            }

            var protocolConnection = connection.get();

            List<String> command;
            while ((command = protocolConnection.readCommand()) != null) {
                String response = commandProcessor.process(command);
                protocolConnection.write(response);
            }

        } catch (IOException exception) {
            System.err.println("Client connection failed: " + exception.getMessage());
        }
    }
}
