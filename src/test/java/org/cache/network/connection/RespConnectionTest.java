package org.cache.network.connection;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.Socket;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RespConnectionTest {

    @Test
    void readCommandReadsRespArray() throws Exception {
        var connection = connectionWithInput("*2\r\n$3\r\nGET\r\n$5\r\nfruit\r\n");

        List<String> command = connection.readCommand();

        assertEquals(List.of("GET", "fruit"), command);
    }

    @Test
    void writeValueResponseAsRespArray() throws Exception {
        var output = new ByteArrayOutputStream();
        var connection = connectionWithOutput(output);

        connection.write("VALUE apple");

        assertEquals("*2\r\n$5\r\nVALUE\r\n$5\r\napple\r\n", output.toString(UTF_8));
    }

    @Test
    void writeListResponseAsRespArrayOfValues() throws Exception {
        var output = new ByteArrayOutputStream();
        var connection = connectionWithOutput(output);

        connection.write("LIST apple, banana");

        assertEquals("*2\r\n$5\r\napple\r\n$6\r\nbanana\r\n", output.toString(UTF_8));
    }

    @Test
    void writeEmptyListResponseAsEmptyRespArray() throws Exception {
        var output = new ByteArrayOutputStream();
        var connection = connectionWithOutput(output);

        connection.write("LIST");

        assertEquals("*0\r\n", output.toString(UTF_8));
    }

    private static RespConnection connectionWithInput(String inputValue) throws Exception {
        return connection(new ByteArrayInputStream(inputValue.getBytes(UTF_8)), new ByteArrayOutputStream());
    }

    private static RespConnection connectionWithOutput(ByteArrayOutputStream output) throws Exception {
        return connection(new ByteArrayInputStream(new byte[0]), output);
    }

    private static RespConnection connection(ByteArrayInputStream input, ByteArrayOutputStream output) throws Exception {
        Socket socket = mock(Socket.class);
        when(socket.getInputStream()).thenReturn(input);
        when(socket.getOutputStream()).thenReturn(output);

        return new RespConnection(socket);
    }
}
