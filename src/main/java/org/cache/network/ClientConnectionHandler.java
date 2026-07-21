package org.cache.network;

import org.cache.protocol.CommandProcessor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientConnectionHandler implements Runnable {

    private final Socket socket;
    private final CommandProcessor<?, ?> commandProcessor;

    public ClientConnectionHandler(Socket socket, CommandProcessor<?, ?> commandProcessor) {
        this.socket = socket;
        this.commandProcessor = commandProcessor;
    }

    @Override
    public void run() {
        try (
                socket;
                var reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
                );
                var writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8)
        ) {
            String command;
            while ((command = reader.readLine()) != null) {
                String response = commandProcessor.process(command);
                writer.println(response);
            }
        } catch (IOException exception) {
            System.err.println("Client connection failed: " + exception.getMessage());
        }
    }
}
