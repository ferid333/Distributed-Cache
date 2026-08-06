package org.cache.network.connection;

import org.cache.network.tcp.connection.AutoProtocolConnection;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.Socket;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AutoProtocolConnectionTest {

    @Test
    void readsLineCommandAndWritesLineResponse() throws Exception {
        var output = new ByteArrayOutputStream();
        var connection = connection("GET fruit\r\n", output);

        List<String> command = connection.readCommand();
        connection.write("VALUE apple");

        assertEquals(List.of("GET", "fruit"), command);
        assertEquals("VALUE apple\r\n", output.toString(UTF_8));
    }

    @Test
    void readsRespCommandAndWritesRespResponse() throws Exception {
        var output = new ByteArrayOutputStream();
        var connection = connection("*2\r\n$3\r\nGET\r\n$5\r\nfruit\r\n", output);

        List<String> command = connection.readCommand();
        connection.write("VALUE apple");

        assertEquals(List.of("GET", "fruit"), command);
        assertEquals("*2\r\n$5\r\nVALUE\r\n$5\r\napple\r\n", output.toString(UTF_8));
    }

    @Test
    void switchesProtocolPerCommandOnSameConnection() throws Exception {
        var output = new ByteArrayOutputStream();
        var input = "PUSH fruits apple\r\n*2\r\n$3\r\nGET\r\n$6\r\nfruits\r\n";
        var connection = connection(input, output);

        List<String> lineCommand = connection.readCommand();
        connection.write("OK");
        List<String> respCommand = connection.readCommand();
        connection.write("VALUE apple");

        assertEquals(List.of("PUSH", "fruits", "apple"), lineCommand);
        assertEquals(List.of("GET", "fruits"), respCommand);
        assertEquals("OK\r\n*2\r\n$5\r\nVALUE\r\n$5\r\napple\r\n", output.toString(UTF_8));
    }

    private static AutoProtocolConnection connection(String inputValue, ByteArrayOutputStream output) throws Exception {
        Socket socket = mock(Socket.class);
        when(socket.getInputStream()).thenReturn(new ByteArrayInputStream(inputValue.getBytes(UTF_8)));
        when(socket.getOutputStream()).thenReturn(output);

        return new AutoProtocolConnection(socket);
    }
}
