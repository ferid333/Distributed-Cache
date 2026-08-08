package org.cache.config;

import org.cache.cluster.CacheNode;
import org.cache.cluster.ClusterInfo;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

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
    private static final String CLUSTER_NODES = ConfigKey.merge(ConfigKey.CLUSTER, ConfigKey.NODES);

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
                getInt(properties, ConfigKey.CAPACITY.getPropertyName(), DEFAULT_CAPACITY),
                getLong(properties, ConfigKey.DEFAULT_TTL_MILLIS, DEFAULT_TTL_MILLIS),
                createKeyCodec(KeyType.from(getString(properties, ConfigKey.KEY_TYPE.getPropertyName(),
                        DEFAULT_KEY_TYPE))),
                createEvictionPolicy(EvictionPolicyType.from(getString(
                        properties,
                        ConfigKey.EVICTION_POLICY.getPropertyName(),
                        DEFAULT_EVICTION_POLICY
                ))),
                buildCacheNode(properties),
                buildClusterInfo(properties)
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
                getString(properties, ConfigKey.merge(ConfigKey.NODE, ConfigKey.ID), DEFAULT_NODE_ID),
                getString(properties, ConfigKey.merge(ConfigKey.NODE, ConfigKey.HOST), DEFAULT_NODE_HOST),
                getInt(properties, ConfigKey.merge(ConfigKey.NODE, ConfigKey.HTTP_PORT), DEFAULT_HTTP_PORT),
                getInt(properties, ConfigKey.merge(ConfigKey.NODE, ConfigKey.TCP_PORT), DEFAULT_TCP_PORT),
                getInt(properties, ConfigKey.merge(ConfigKey.NODE, ConfigKey.CLUSTER_PORT), DEFAULT_CLUSTER_PORT)
        );
    }

    private ClusterInfo buildClusterInfo(Properties properties) {
        if (!hasClusterConfig(properties)) {
            return null;
        }

        int replicationFactor = getInt(properties,
                ConfigKey.merge(ConfigKey.CLUSTER, ConfigKey.REPLICATION_FACTOR), 1);

        List<CacheNode> nodes = new ArrayList<>();
        int index = 0;

        while (true) {
            String idKey = clusterNodeKey(index, ConfigKey.ID);
            String id = properties.getProperty(idKey);

            if (id == null) {
                break;
            }

            CacheNode node = new CacheNode(
                    getString(properties, idKey),
                    getString(properties, clusterNodeKey(index, ConfigKey.HOST)),
                    getInt(properties, clusterNodeKey(index, ConfigKey.HTTP_PORT)),
                    getInt(properties, clusterNodeKey(index, ConfigKey.TCP_PORT)),
                    getInt(properties, clusterNodeKey(index, ConfigKey.CLUSTER_PORT))
            );

            nodes.add(node);
            index++;
        }

        validateClusterInfo(replicationFactor, nodes);

        return new ClusterInfo(replicationFactor, nodes);
    }

    private void validateClusterInfo(int replicationFactor, List<CacheNode> nodes) {
        if (replicationFactor < 1) {
            throw new CacheConfigException("Cluster replication factor must be at least 1");
        }

        if (replicationFactor > nodes.size()) {
            throw new CacheConfigException("Cluster replication factor must not exceed number of active nodes");
        }

        Set<String> nodeIds = new HashSet<>();
        Set<String> hostPorts = new HashSet<>();

        for (CacheNode node : nodes) {
            if (!nodeIds.add(node.id())) {
                throw new CacheConfigException("Cluster node ids must be unique: " + node.id());
            }

            addHostPort(hostPorts, node.host(), node.httpPort());
            addHostPort(hostPorts, node.host(), node.tcpPort());
            addHostPort(hostPorts, node.host(), node.clusterPort());
        }
    }

    private void addHostPort(Set<String> hostPorts, String host, int port) {
        String hostPort = host + ":" + port;
        if (!hostPorts.add(hostPort)) {
            throw new CacheConfigException("Cluster node host-port combinations must be unique: " + hostPort);
        }
    }

    private boolean hasClusterConfig(Properties properties) {
        return properties.stringPropertyNames()
                .stream()
                .anyMatch(key -> key.equals(ConfigKey.CLUSTER.getPropertyName())
                        || key.startsWith(ConfigKey.CLUSTER.getPropertyName() + "."));
    }

    private String clusterNodeKey(int index, ConfigKey field) {
        return CLUSTER_NODES + "[" + index + "]." + field.getPropertyName();
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

    private String getString(Properties properties, String key, String defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return value.trim();
    }

    private String getString(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new CacheConfigException("Missing required configuration key: " + key);
        }

        return value.trim();
    }

    private int getInt(Properties properties, String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new CacheConfigException("Invalid integer value for configuration key '" + key + "': " + value, e);
        }
    }

    private int getInt(Properties properties, String key) {
        String value = getString(properties, key);

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new CacheConfigException("Invalid integer value for configuration key '" + key + "': " + value, e);
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
            throw new CacheConfigException("Invalid long value for configuration key '" + key.getPropertyName() +
                    "': " + value, e);
        }
    }
}
