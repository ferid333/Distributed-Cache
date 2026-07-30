package org.cache.network;

import org.cache.network.connection.ClientConnectionHandler;
import org.cache.protocol.CommandProcessor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TcpCacheServer implements AutoCloseable {

    private final int port;
    private final CommandProcessor<?> commandProcessor;
    private final ExecutorService executor;
    private ServerSocket serverSocket;

    private static final int THREAD_COUNT = 16;

    public TcpCacheServer(int port, CommandProcessor<?> commandProcessor) {
        this.port = port;
        this.commandProcessor = commandProcessor;
        this.executor = Executors.newFixedThreadPool(THREAD_COUNT);
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("TCP cache server listening on port " + port);

        while (!Thread.currentThread().isInterrupted() && !serverSocket.isClosed()) {
            Socket socket = serverSocket.accept();
            executor.submit(new ClientConnectionHandler(socket, commandProcessor));
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
