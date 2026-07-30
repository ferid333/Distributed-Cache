package org.cache.network.connection;

import org.cache.protocol.ProtocolConstants;

import java.io.IOException;
import java.io.PushbackInputStream;
import java.net.Socket;
import java.util.Optional;

public class ProtocolConnectionFactory {

    private final Socket socket;

    public ProtocolConnectionFactory(Socket socket) {
        this.socket = socket;
    }

    public Optional<ProtocolConnection> create() throws IOException {
        var input = new PushbackInputStream(socket.getInputStream(), 1);
        var output = socket.getOutputStream();
        int firstByte = input.read();

        if (firstByte == -1) {
            return Optional.empty();
        }

        input.unread(firstByte);

        if (firstByte == ProtocolConstants.ARRAY_PREFIX) {
            return Optional.of(new RespConnection(socket, input, output));
        }

        return Optional.of(new LineProtocolConnection(socket, input, output));
    }
}
