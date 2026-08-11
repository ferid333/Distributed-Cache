package org.cache.network.tcp.connection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

public class RespCommandClient {

    private static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 2_000;
    private static final int DEFAULT_READ_TIMEOUT_MILLIS = 5_000;

    public List<String> send(String host, int port, List<String> commandParts) throws IOException {
        try (Socket socket = connect(host, port);
                RespConnection connection = new RespConnection(socket)) {
            return connection.sendCommandForResponse(commandParts);
        }
    }

    private Socket connect(String host, int port) throws IOException {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), DEFAULT_CONNECT_TIMEOUT_MILLIS);
            socket.setSoTimeout(DEFAULT_READ_TIMEOUT_MILLIS);
            return socket;
        } catch (IOException exception) {
            socket.close();
            throw exception;
        }
    }
}
