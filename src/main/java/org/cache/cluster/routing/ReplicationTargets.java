package org.cache.cluster.routing;

import org.cache.cluster.CacheNode;

import java.util.List;

public record ReplicationTargets(boolean includesCurrentNode, List<CacheNode> remoteNodes) {
}