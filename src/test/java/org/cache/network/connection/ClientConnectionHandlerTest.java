package org.cache.network.connection;

import org.cache.network.tcp.connection.ClientConnectionHandler;
import org.cache.protocol.CommandProcessor;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.Socket;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClientConnectionHandlerTest {

    @Test
    void runProcessesCommandAndWritesResponse() throws Exception {
        Socket socket = mock(Socket.class);
        CommandProcessor<?> commandProcessor = mock(CommandProcessor.class);
        var input = new ByteArrayInputStream("GET fruit\r\n".getBytes(UTF_8));
        var output = new ByteArrayOutputStream();

        when(socket.getInputStream()).thenReturn(input);
        when(socket.getOutputStream()).thenReturn(output);
        when(commandProcessor.process(List.of("GET", "fruit"))).thenReturn("VALUE apple");

        new ClientConnectionHandler(socket, commandProcessor).run();

        verify(commandProcessor).process(List.of("GET", "fruit"));
        verify(socket).close();
        assertEquals("VALUE apple\r\n", output.toString(UTF_8));
    }
}
