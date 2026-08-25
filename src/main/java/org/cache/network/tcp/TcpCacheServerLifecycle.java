package org.cache.network.tcp;

import org.springframework.context.SmartLifecycle;

import java.io.IOException;

public class TcpCacheServerLifecycle implements SmartLifecycle {

    private final TcpCacheServer server;
    private final String threadName;
    private volatile boolean running;
    private Thread serverThread;

    public TcpCacheServerLifecycle(TcpCacheServer server, String threadName) {
        this.server = server;
        this.threadName = threadName;
    }

    @Override
    public void start() {
        if (running) {
            return;
        }

        running = true;
        serverThread = new Thread(this::runServer, threadName);
        serverThread.start();
    }

    @Override
    public void stop() {
        running = false;

        try {
            server.close();
        } catch (IOException exception) {
            System.err.println("Failed to stop TCP cache server: " + exception.getMessage());
        }

        if (serverThread != null) {
            serverThread.interrupt();
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private void runServer() {
        try {
            server.start();
        } catch (IOException exception) {
            if (running) {
                System.err.println("TCP cache server stopped unexpectedly: " + exception.getMessage());
            }
        } finally {
            running = false;
        }
    }
}
