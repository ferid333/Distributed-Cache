package org.cache;

import org.cache.core.LocalCache;
import org.cache.core.ValueType;
import org.cache.eviction.LruEvictionPolicy;
import org.cache.network.connection.ClientConnectionHandler;
import org.cache.network.TcpCacheServer;
import org.cache.protocol.CommandParser;
import org.cache.protocol.CommandProcessor;
import org.cache.protocol.codec.ListValueCodec;
import org.cache.protocol.codec.StringKeyCodec;
import org.cache.protocol.codec.StringValueCodec;
import org.cache.protocol.codec.ValueCodecRegistry;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.Executors;

public class Main {
    private static final int SERVER_THREAD_COUNT = 16;

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 2020;
        var cache = new LocalCache<String>(1_000, new LruEvictionPolicy<>());
        var keyCodec = new StringKeyCodec();
        var valueCodecs = new ValueCodecRegistry();
        valueCodecs.register(ValueType.STRING, new StringValueCodec()).register(ValueType.LIST, new ListValueCodec());
        var commandParser = new CommandParser<>(keyCodec);
        var commandProcessor = new CommandProcessor<>(cache, commandParser, valueCodecs);


        try (cache;
             var serverSocket = new ServerSocket(port);
             var executor = Executors.newFixedThreadPool(SERVER_THREAD_COUNT);
             var server = new TcpCacheServer(
                     serverSocket,
                     executor,
                     socket -> new ClientConnectionHandler(socket, commandProcessor)
             )) {
            server.start();
        }
    }
}
