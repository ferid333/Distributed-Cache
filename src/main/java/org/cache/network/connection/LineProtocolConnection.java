package org.cache.network.connection;

import org.cache.protocol.ProtocolConstants;
import org.cache.protocol.RegexConstants;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class LineProtocolConnection implements ProtocolConnection, AutoCloseable {

    private final Socket socket;
    private final InputStream input;
    private final OutputStream output;

    public LineProtocolConnection(Socket socket) throws IOException {
        this(socket, socket.getInputStream(), socket.getOutputStream());
    }

    LineProtocolConnection(Socket socket, InputStream input, OutputStream output) {
        this.socket = socket;
        this.input = input;
        this.output = output;
    }

    public List<String> readCommand() throws IOException {
        String line = readLine();

        if (line == null) {
            return null;
        }

        if (line.isBlank()) {
            return List.of();
        }

        return List.of(line.trim().split(RegexConstants.WHITESPACE));
    }

    public String readLine() throws IOException {
        StringBuilder line = new StringBuilder();

        while (true) {
            int next = input.read();

            if (next == -1) {
                return line.isEmpty() ? null : line.toString();
            }

            if (next == ProtocolConstants.NEW_LINE) {
                return line.toString();
            }

            if (next != ProtocolConstants.CARRIAGE_RETURN) {
                line.append((char) next);
            }
        }
    }

    @Override
    public void write(String line) throws IOException {
        output.write(line.getBytes(StandardCharsets.UTF_8));
        output.write(ProtocolConstants.CARRIAGE_RETURN);
        output.write(ProtocolConstants.NEW_LINE);
        output.flush();
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
