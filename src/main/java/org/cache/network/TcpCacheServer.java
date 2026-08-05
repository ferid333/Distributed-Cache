package org.cache.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

public class TcpCacheServer implements AutoCloseable {

    private final ExecutorService executor;
    private final ClientConnectionHandlerFactory handlerFactory;
    private final ServerSocket serverSocket;

    public TcpCacheServer(
            ServerSocket serverSocket,
            ExecutorService executor,
            ClientConnectionHandlerFactory handlerFactory
    ) {
        this.serverSocket = serverSocket;
        this.executor = executor;
        this.handlerFactory = handlerFactory;
    }

    public void start() throws IOException {
        System.out.println("TCP cache server listening on port " + serverSocket.getLocalPort());

        while (!Thread.currentThread().isInterrupted() && !serverSocket.isClosed()) {
            Socket socket = serverSocket.accept();
            executor.submit(handlerFactory.create(socket));
        }
    }

    @Override
    public void close() throws IOException {
        executor.shutdownNow();

        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
    }
}
