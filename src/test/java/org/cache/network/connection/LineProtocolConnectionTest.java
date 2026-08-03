package org.cache.network.connection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.Socket;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LineProtocolConnectionTest {

    private LineProtocolConnection lineProtocolConnection;
    private ByteArrayOutputStream output;

    @BeforeEach
    void setup() throws Exception {
        Socket socket = mock(Socket.class);

        var input = new ByteArrayInputStream("GET fruit\r\n".getBytes(UTF_8));
        output = new ByteArrayOutputStream();

        when(socket.getInputStream()).thenReturn(input);
        when(socket.getOutputStream()).thenReturn(output);

        lineProtocolConnection = new LineProtocolConnection(socket);
    }

    @Test
    void readCommandSplitsLineIntoParts() throws Exception {
        List<String> command = lineProtocolConnection.readCommand();

        assertEquals(List.of("GET", "fruit"), command);
    }

    @Test
    void readLineReturnsLineWithoutCrlf() throws Exception {
        String givenLine = lineProtocolConnection.readLine();

        String expectedResponse = "GET fruit";

        assertEquals(expectedResponse, givenLine);
    }

    @Test
    void writeAppendsCrlf() throws Exception {
        String givenValue = "VALUE 333";

        lineProtocolConnection.write(givenValue);

        assertEquals("VALUE 333\r\n", output.toString(UTF_8));
    }
}
