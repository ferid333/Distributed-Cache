package org.cache.network;

import org.cache.network.tcp.ClientConnectionHandlerFactory;
import org.cache.network.tcp.TcpCacheServer;
import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TcpCacheServerTest {

    @Test
    void startSubmitsHandlerForAcceptedSocket() throws Exception {
        ServerSocket serverSocket = mock(ServerSocket.class);
        Socket acceptedSocket = mock(Socket.class);
        ExecutorService executor = mock(ExecutorService.class);
        Runnable handler = mock(Runnable.class);
        ClientConnectionHandlerFactory handlerFactory = mock(ClientConnectionHandlerFactory.class);

        when(serverSocket.getLocalPort()).thenReturn(2020);
        when(serverSocket.isClosed()).thenReturn(false, true);
        when(serverSocket.accept()).thenReturn(acceptedSocket);
        when(handlerFactory.create(acceptedSocket)).thenReturn(handler);
        var server = new TcpCacheServer(serverSocket, executor, handlerFactory);

        server.start();

        verify(handlerFactory).create(acceptedSocket);
        verify(executor).submit(handler);
    }
}
