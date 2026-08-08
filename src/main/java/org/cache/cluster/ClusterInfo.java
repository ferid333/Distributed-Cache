package org.cache.cluster;

import java.util.List;

public record ClusterInfo(
        Integer replicationFactor,
        List<CacheNode> nodes
) {
}
