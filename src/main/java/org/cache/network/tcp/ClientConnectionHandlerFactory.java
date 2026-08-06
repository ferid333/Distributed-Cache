package org.cache.network.tcp;

import java.net.Socket;

@FunctionalInterface
public interface ClientConnectionHandlerFactory {

    Runnable create(Socket socket);
}
