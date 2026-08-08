package org.cache.config;

import org.cache.cluster.CacheNode;
import org.cache.eviction.EvictionPolicy;
import org.cache.eviction.EvictionPolicyType;
import org.cache.eviction.LruEvictionPolicy;
import org.cache.eviction.MruEvictionPolicy;
import org.cache.protocol.codec.IntegerKeyCodec;
import org.cache.protocol.codec.KeyCodec;
import org.cache.protocol.codec.StringKeyCodec;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.util.Objects;
import java.util.Properties;

public final class CacheConfigLoader {

    private static final String DEFAULT_CONFIG_FILE = "config.yml";
    private static final int DEFAULT_CAPACITY = 1_000;
    private static final long DEFAULT_TTL_MILLIS = 0L;
    private static final String DEFAULT_KEY_TYPE = "string";
    private static final String DEFAULT_EVICTION_POLICY = "lru";
    private static final String DEFAULT_NODE_ID = "node-a";
    private static final String DEFAULT_NODE_HOST = "localhost";
    private static final int DEFAULT_HTTP_PORT = 8080;
    private static final int DEFAULT_TCP_PORT = 2020;
    private static final int DEFAULT_CLUSTER_PORT = 10001;

    private final String configFile;

    public CacheConfigLoader() {
        this(DEFAULT_CONFIG_FILE);
    }

    public CacheConfigLoader(String configFile) {
        this.configFile = Objects.requireNonNull(configFile, "ConfigFile must not be null");
    }

    public CacheConfig load() {
        Properties properties = loadProperties();

        return new CacheConfig(
                getInt(properties, ConfigKey.CAPACITY, DEFAULT_CAPACITY),
                getLong(properties, ConfigKey.DEFAULT_TTL_MILLIS, DEFAULT_TTL_MILLIS),
                createKeyCodec(KeyType.from(getString(properties, ConfigKey.KEY_TYPE, DEFAULT_KEY_TYPE))),
                createEvictionPolicy(EvictionPolicyType.from(getString(
                        properties,
                        ConfigKey.EVICTION_POLICY,
                        DEFAULT_EVICTION_POLICY
                ))),
                buildCacheNode(properties)
        );
    }

    private Properties loadProperties() {
        Resource resource = new ClassPathResource(configFile);

        if (!resource.exists()) {
            return new Properties();
        }

        var factory = new YamlPropertiesFactoryBean();
        factory.setResources(resource);
        factory.afterPropertiesSet();

        Properties properties = factory.getObject();
        return (properties != null) ? properties : new Properties();
    }

    private CacheNode buildCacheNode(Properties properties) {
        return new CacheNode(
                getString(properties, ConfigKey.NODE_ID, DEFAULT_NODE_ID),
                getString(properties, ConfigKey.NODE_HOST, DEFAULT_NODE_HOST),
                getInt(properties, ConfigKey.NODE_HTTP_PORT, DEFAULT_HTTP_PORT),
                getInt(properties, ConfigKey.NODE_TCP_PORT, DEFAULT_TCP_PORT),
                getInt(properties, ConfigKey.NODE_CLUSTER_PORT, DEFAULT_CLUSTER_PORT)
        );
    }

    @SuppressWarnings("unchecked")
    private KeyCodec<Object> createKeyCodec(KeyType keyType) {
        return switch (keyType) {
            case STRING -> (KeyCodec<Object>) (KeyCodec<?>) new StringKeyCodec();
            case INTEGER -> (KeyCodec<Object>) (KeyCodec<?>) new IntegerKeyCodec();
        };
    }

    private EvictionPolicy<Object> createEvictionPolicy(EvictionPolicyType policy) {
        return switch (policy) {
            case LRU -> new LruEvictionPolicy<>();
            case MRU -> new MruEvictionPolicy<>();
        };
    }

    private String getString(Properties properties, ConfigKey key, String defaultValue) {
        String value = properties.getProperty(key.getPropertyName());
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return value.trim();
    }

    private int getInt(Properties properties, ConfigKey key, int defaultValue) {
        String value = properties.getProperty(key.getPropertyName());
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid integer value for configuration key '" + key.getPropertyName() + "': " + value, e);
        }
    }

    private long getLong(Properties properties, ConfigKey key, long defaultValue) {
        String value = properties.getProperty(key.getPropertyName());
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid long value for configuration key '" + key.getPropertyName() + "': " + value, e);
        }
    }
}
