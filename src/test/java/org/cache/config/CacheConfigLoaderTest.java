package org.cache.config;

import org.cache.eviction.LruEvictionPolicy;
import org.cache.eviction.MruEvictionPolicy;
import org.cache.protocol.codec.IntegerKeyCodec;
import org.cache.protocol.codec.StringKeyCodec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CacheConfigLoaderTest {

    @Test
    void loadUsesDefaultsWhenConfigFileDoesNotExist() {
        CacheConfig config = new CacheConfigLoader("missing-config.yml").load();

        assertEquals(1_000, config.capacity());
        assertEquals(0, config.defaultTtlMillis());
        assertInstanceOf(StringKeyCodec.class, config.keyCodec());
        assertInstanceOf(LruEvictionPolicy.class, config.evictionPolicy());
        assertEquals("node-a", config.cacheNode().id());
        assertEquals("localhost", config.cacheNode().host());
        assertEquals(8080, config.cacheNode().httpPort());
        assertEquals(2020, config.cacheNode().tcpPort());
        assertEquals(10001, config.cacheNode().clusterPort());
        assertNull(config.clusterInfo());
    }

    @Test
    void loadReadsConfiguredValues() {
        CacheConfig config = new CacheConfigLoader("config/full-config.yml").load();

        assertEquals(250, config.capacity());
        assertEquals(5_000, config.defaultTtlMillis());
        assertInstanceOf(IntegerKeyCodec.class, config.keyCodec());
        assertEquals(42, config.keyCodec().decode("42"));
        assertInstanceOf(MruEvictionPolicy.class, config.evictionPolicy());
        assertEquals("node-test", config.cacheNode().id());
        assertEquals("127.0.0.1", config.cacheNode().host());
        assertEquals(18080, config.cacheNode().httpPort());
        assertEquals(12020, config.cacheNode().tcpPort());
        assertEquals(11001, config.cacheNode().clusterPort());
        assertNull(config.clusterInfo());
    }

    @Test
    void loadReadsClusterValuesWhenConfigured() {
        CacheConfig config = new CacheConfigLoader("config/cluster-config.yml").load();

        assertNotNull(config.clusterInfo());
        assertEquals(2, config.clusterInfo().replicationFactor());
        assertEquals(3, config.clusterInfo().nodes().size());
        assertEquals("node-a", config.clusterInfo().nodes().getFirst().id());
    }

    @Test
    void loadUsesDefaultReplicationFactorWhenClusterOmitsIt() {
        CacheConfig config = new CacheConfigLoader("config/cluster-config-default-replication-factor.yml").load();

        assertNotNull(config.clusterInfo());
        assertEquals(1, config.clusterInfo().replicationFactor());
        assertEquals(1, config.clusterInfo().nodes().size());
    }

    @Test
    void loadRejectsReplicationFactorLessThanOne() {
        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> new CacheConfigLoader("config/invalid-cluster-replication-factor-too-small.yml").load()
        );

        assertEquals("Cluster replication factor must be at least 1", exception.getMessage());
    }

    @Test
    void loadRejectsReplicationFactorGreaterThanActiveNodes() {
        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> new CacheConfigLoader("config/invalid-cluster-replication-factor-too-large.yml").load()
        );

        assertEquals("Cluster replication factor must not exceed number of active nodes", exception.getMessage());
    }

    @Test
    void loadRejectsDuplicateClusterNodeIds() {
        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> new CacheConfigLoader("config/invalid-cluster-duplicate-node-id.yml").load()
        );

        assertEquals("Cluster node ids must be unique: node-a", exception.getMessage());
    }

    @Test
    void loadRejectsDuplicateClusterHostPorts() {
        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> new CacheConfigLoader("config/invalid-cluster-duplicate-host-port.yml").load()
        );

        assertEquals("Cluster node host-port combinations must be unique: localhost:2020", exception.getMessage());
    }

    @Test
    void loadRejectsUnsupportedKeyType() {
        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> new CacheConfigLoader("config/invalid-key-type.yml").load()
        );

        assertEquals("Unsupported key type: uuid", exception.getMessage());
    }

    @Test
    void loadRejectsUnsupportedEvictionPolicy() {
        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> new CacheConfigLoader("config/invalid-eviction-policy.yml").load()
        );

        assertEquals("Unsupported eviction policy: fifo", exception.getMessage());
    }

    @Test
    void loadRejectsInvalidNumericValues() {
        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> new CacheConfigLoader("config/invalid-capacity.yml").load()
        );

        assertEquals("Invalid integer value for configuration key 'capacity': many", exception.getMessage());
    }
}
