package org.cache.network.tcp.connection;

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.function.Function;

import static org.cache.protocol.handlers.ResponseConstants.ERROR;

public class ClientConnectionHandler implements Runnable {

    private static final String DEFAULT_ERROR_MESSAGE = "internal server error";

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
                protocolConnection.write(process(command));
            }

        } catch (IOException exception) {
            System.err.println("Client connection failed: " + exception.getMessage());
        }
    }

    private String process(List<String> command) {
        try {
            return commandProcessor.apply(command);
        } catch (RuntimeException exception) {
            String message = exception.getMessage();
            if (message == null || message.isBlank()) {
                message = DEFAULT_ERROR_MESSAGE;
            }

            return ERROR.name() + " " + message;
        }
    }
}
