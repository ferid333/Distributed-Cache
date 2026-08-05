package org.cache.protocol.handlers;

import java.util.List;

public interface CommandHandler {

    String handle(List<String> parts);
}
