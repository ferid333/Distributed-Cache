package org.cache;


import org.cache.core.LocalCache;
import org.cache.eviction.LruEvictionPolicy;
import org.cache.network.TcpCacheServer;
import org.cache.protocol.CommandParser;
import org.cache.protocol.CommandProcessor;
import org.cache.protocol.codec.StringCodec;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 2020;
        var cache = new LocalCache<String, String>(1_000, new LruEvictionPolicy<>());
        var stringCodec = new StringCodec();
        var commandParser = new CommandParser<>(stringCodec, stringCodec);
        var commandProcessor = new CommandProcessor<>(cache, commandParser, stringCodec);


        try (cache; var server = new TcpCacheServer(port, commandProcessor)) {
            server.start();
        }
    }
}