package org.cache.network.connection;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.Socket;
import java.util.List;

import static org.cache.protocol.ProtocolConstants.ARRAY_PREFIX;

public class AutoProtocolConnection implements ProtocolConnection, AutoCloseable {

    private final Socket socket;
    private final PushbackInputStream input;
    private final RespConnection resp;
    private final LineProtocolConnection lines;
    private ProtocolConnection currentCommandConnection;

    public AutoProtocolConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.input = new PushbackInputStream(socket.getInputStream(), 1);
        OutputStream output = socket.getOutputStream();
        this.resp = new RespConnection(socket, input, output);
        this.lines = new LineProtocolConnection(socket, input, output);
    }

    @Override
    public List<String> readCommand() throws IOException {
        int firstByte = input.read();

        if (firstByte == -1) {
            return null;
        }

        input.unread(firstByte);
        currentCommandConnection = firstByte == ARRAY_PREFIX ? resp : lines;
        return currentCommandConnection.readCommand();
    }

    @Override
    public void write(String value) throws IOException {
        if (currentCommandConnection == null) {
            throw new IOException("Cannot write response before reading command");
        }

        currentCommandConnection.write(value);
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
