package org.cache;

import org.cache.core.Cache;
import org.cache.core.CacheService;
import org.cache.core.LocalCache;
import org.cache.core.ValueType;
import org.cache.eviction.LruEvictionPolicy;
import org.cache.network.tcp.TcpCacheServer;
import org.cache.network.tcp.connection.ClientConnectionHandler;
import org.cache.protocol.CommandProcessor;
import org.cache.protocol.codec.ListValueCodec;
import org.cache.protocol.codec.StringKeyCodec;
import org.cache.protocol.codec.StringValueCodec;
import org.cache.protocol.codec.ValueCodecRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

@SpringBootApplication
public class Main {
    private static final int SERVER_THREAD_COUNT = 16;
    private static final int DEFAULT_CACHE_CAPACITY = 1_000;

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @Bean(destroyMethod = "close")
    public LocalCache<String> cache() {
        return new LocalCache<>(DEFAULT_CACHE_CAPACITY, new LruEvictionPolicy<>());
    }

    @Bean
    public ValueCodecRegistry valueCodecs() {
        return new ValueCodecRegistry()
                .register(ValueType.STRING, new StringValueCodec())
                .register(ValueType.LIST, new ListValueCodec());
    }

    @Bean
    public CacheService<String> cacheService(Cache<String> cache, ValueCodecRegistry valueCodecs) {
        return new CacheService<>(cache, valueCodecs);
    }

    @Bean
    public CommandProcessor<String> commandProcessor(CacheService<String> cacheService) {
        return new CommandProcessor<>(new StringKeyCodec(), cacheService);
    }

    @Bean(destroyMethod = "shutdownNow")
    public ExecutorService tcpClientExecutor() {
        return Executors.newFixedThreadPool(SERVER_THREAD_COUNT);
    }

    @Bean
    public TcpCacheServer tcpCacheServer(
            @Value("${cache.tcp.port:2020}") int port,
            ExecutorService tcpClientExecutor,
            CommandProcessor<String> commandProcessor
    ) throws IOException {
        return new TcpCacheServer(
                new ServerSocket(port),
                tcpClientExecutor,
                socket -> new ClientConnectionHandler(socket, commandProcessor)
        );
    }
}
