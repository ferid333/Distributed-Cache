package org.cache;

import org.cache.cluster.CacheNode;
import org.cache.config.CacheConfig;
import org.cache.config.CacheConfigLoader;
import org.cache.core.Cache;
import org.cache.core.CacheService;
import org.cache.core.LocalCache;
import org.cache.core.ValueType;
import org.cache.eviction.EvictionPolicy;
import org.cache.network.tcp.TcpCacheServer;
import org.cache.network.tcp.connection.ClientConnectionHandler;
import org.cache.protocol.CommandProcessor;
import org.cache.protocol.codec.KeyCodec;
import org.cache.protocol.codec.ListValueCodec;
import org.cache.protocol.codec.StringValueCodec;
import org.cache.protocol.codec.ValueCodecRegistry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

@SpringBootApplication
public class Main {
    private static final int SERVER_THREAD_COUNT = 16;
    private static CacheConfig configuredCacheConfig;

    public static void main(String[] args) {
        configuredCacheConfig = loadConfiguration();
        var app = new SpringApplication(Main.class);
        app.setDefaultProperties(Map.of(
                "server.port", configuredCacheConfig.cacheNode().httpPort()
        ));
        app.run(args);
    }

    private static CacheConfig loadConfiguration() {
        var cacheLoader = new CacheConfigLoader();
        return cacheLoader.load();
    }

    @Bean
    public CacheConfig cacheConfig() {
        if (configuredCacheConfig == null) {
            configuredCacheConfig = loadConfiguration();
        }

        return configuredCacheConfig;
    }

    @Bean(destroyMethod = "close")
    public LocalCache<Object> cache(CacheConfig cacheConfig, EvictionPolicy<Object> evictionPolicy) {
        return new LocalCache<>(cacheConfig.capacity(), evictionPolicy);
    }

    @Bean
    public CacheNode cacheNode(CacheConfig cacheConfig) {
        return cacheConfig.cacheNode();
    }

    @Bean
    public KeyCodec<Object> keyCodec(CacheConfig cacheConfig) {
        return cacheConfig.keyCodec();
    }

    @Bean
    public EvictionPolicy<Object> evictionPolicy(CacheConfig cacheConfig) {
        return cacheConfig.evictionPolicy();
    }

    @Bean
    public ValueCodecRegistry valueCodecs() {
        return new ValueCodecRegistry()
                .register(ValueType.STRING, new StringValueCodec())
                .register(ValueType.LIST, new ListValueCodec());
    }

    @Bean
    public CacheService<Object> cacheService(Cache<Object> cache, ValueCodecRegistry valueCodecs) {
        return new CacheService<>(cache, valueCodecs);
    }

    @Bean
    public CommandProcessor<Object> commandProcessor(KeyCodec<Object> keyCodec, CacheService<Object> cacheService) {
        return new CommandProcessor<>(keyCodec, cacheService);
    }

    @Bean(destroyMethod = "shutdownNow")
    public ExecutorService tcpClientExecutor() {
        return Executors.newFixedThreadPool(SERVER_THREAD_COUNT);
    }

    @Bean
    public TcpCacheServer tcpCacheServer(
            CacheNode cacheNode,
            ExecutorService tcpClientExecutor,
            CommandProcessor<Object> commandProcessor
    ) throws IOException {
        return new TcpCacheServer(
                new ServerSocket(cacheNode.tcpPort()),
                tcpClientExecutor,
                socket -> new ClientConnectionHandler(socket, commandProcessor)
        );
    }
}
