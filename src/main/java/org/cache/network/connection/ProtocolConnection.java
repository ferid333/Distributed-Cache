package org.cache.network.connection;

import java.io.IOException;
import java.util.List;

public interface ProtocolConnection {

    List<String> readCommand() throws IOException;

    void write(String value) throws IOException;

    String send(String value) throws IOException;
}
