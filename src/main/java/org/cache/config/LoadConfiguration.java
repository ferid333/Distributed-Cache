package org.cache.config;

import org.cache.cluster.CacheNode;
import org.cache.eviction.EvictionPolicy;
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
public final class LoadConfiguration {

    private static final String DEFAULT_CONFIG_FILE = "config.yml";

    private final String configFile;

    public LoadConfiguration() {
        this(DEFAULT_CONFIG_FILE);
    }

    public LoadConfiguration(String configFile) {
        this.configFile = Objects.requireNonNull(configFile, "ConfigFile must not be null");
    }

    public CacheConfig load() {
        Properties properties = loadProperties();

        return new CacheConfig(
                getInt(properties, "capacity", 1_000),
                getLong(properties, "defaultTtlMillis", 0L),
                createKeyCodec(properties.getProperty("key-type", "string")),
                createEvictionPolicy(properties.getProperty("eviction-policy", "lru")),
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
                properties.getProperty("node.id", "node-a"),
                properties.getProperty("node.host", "localhost"),
                getInt(properties, "node.http-port", 8080),
                getInt(properties, "node.tcp-port", 2020),
                getInt(properties, "node.cluster-port", 10001)
        );
    }

    private KeyCodec<?> createKeyCodec(String keyType) {
        String normalizedType = keyType.trim().toLowerCase();
        return switch (normalizedType) {
            case "string" -> new StringKeyCodec();
            case "integer", "int" -> new IntegerKeyCodec();
            default -> throw new IllegalArgumentException("Unsupported key type: " + keyType);
        };
    }

    private <K> EvictionPolicy<K> createEvictionPolicy(String policy) {
        String normalizedPolicy = policy.trim().toLowerCase();
        return switch (normalizedPolicy) {
            case "lru" -> new LruEvictionPolicy<>();
            case "mru" -> new MruEvictionPolicy<>();
            default -> throw new IllegalArgumentException("Unsupported eviction policy: " + policy);
        };
    }

    private int getInt(Properties properties, String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid integer value for configuration key '" + key + "': " + value, e);
        }
    }

    private long getLong(Properties properties, String key, long defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid long value for configuration key '" + key + "': " + value, e);
        }
    }
}