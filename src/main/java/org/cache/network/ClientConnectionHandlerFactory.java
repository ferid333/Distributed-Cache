package org.cache.network;

import java.net.Socket;

@FunctionalInterface
public interface ClientConnectionHandlerFactory {

    Runnable create(Socket socket);
}
