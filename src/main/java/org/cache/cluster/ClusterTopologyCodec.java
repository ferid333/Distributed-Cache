package org.cache.cluster;

import java.util.ArrayList;
import java.util.List;

public class ClusterTopologyCodec {

    private static final String SECTION_SEPARATOR = "|";
    private static final String SECTION_SEPARATOR_REGEX = "\\|";
    private static final String NODE_SEPARATOR = ";";
    private static final String NODE_FIELD_SEPARATOR = ",";
    private static final String EMPTY_TOPOLOGY_NODES = "";
    private static final String INVALID_TOPOLOGY_MESSAGE = "Invalid cluster topology";
    private static final String INVALID_NODE_MESSAGE = "Invalid cluster node";
    private static final int HEADER_PARTS = 4;
    private static final int VERSION_INDEX = 0;
    private static final int REPLICATION_FACTOR_INDEX = 1;
    private static final int VIRTUAL_NODE_COUNT_INDEX = 2;
    private static final int NODES_INDEX = 3;
    private static final int NODE_FIELD_COUNT = 6;
    private static final int NODE_ID_INDEX = 0;
    private static final int NODE_HOST_INDEX = 1;
    private static final int NODE_HTTP_PORT_INDEX = 2;
    private static final int NODE_TCP_PORT_INDEX = 3;
    private static final int NODE_CLUSTER_PORT_INDEX = 4;
    private static final int NODE_STATUS_INDEX = 5;

    public String encode(ClusterTopology topology) {
        String nodes = topology.nodes()
                .stream()
                .map(this::encodeNode)
                .reduce((first, second) -> first + NODE_SEPARATOR + second)
                .orElse(EMPTY_TOPOLOGY_NODES);

        return topology.version()
                + SECTION_SEPARATOR
                + topology.replicationFactor()
                + SECTION_SEPARATOR
                + topology.virtualNodeCount()
                + SECTION_SEPARATOR
                + nodes;
    }

    public ClusterTopology decode(String value) {
        String[] parts = value.split(SECTION_SEPARATOR_REGEX, HEADER_PARTS);
        if (parts.length != HEADER_PARTS) {
            throw new CacheInfoException(INVALID_TOPOLOGY_MESSAGE);
        }

        long version = ClusterNumberParser.parseLong(parts[VERSION_INDEX], TopologyField.VERSION.getValue());
        int replicationFactor = ClusterNumberParser.parseInt(
                parts[REPLICATION_FACTOR_INDEX],
                TopologyField.REPLICATION_FACTOR.getValue()
        );
        int virtualNodeCount = ClusterNumberParser.parseInt(
                parts[VIRTUAL_NODE_COUNT_INDEX],
                TopologyField.VIRTUAL_NODE_COUNT.getValue()
        );
        List<CacheNode> nodes = decodeNodes(parts[NODES_INDEX]);

        return new ClusterTopology(version, nodes, replicationFactor, virtualNodeCount);
    }

    private String encodeNode(CacheNode node) {
        return node.getId()
                + NODE_FIELD_SEPARATOR
                + node.getHost()
                + NODE_FIELD_SEPARATOR
                + node.getHttpPort()
                + NODE_FIELD_SEPARATOR
                + node.getTcpPort()
                + NODE_FIELD_SEPARATOR
                + node.getClusterPort()
                + NODE_FIELD_SEPARATOR
                + node.getStatus().name();
    }

    private List<CacheNode> decodeNodes(String value) {
        if (value.isBlank()) {
            return List.of();
        }

        List<CacheNode> nodes = new ArrayList<>();
        for (String nodeValue : value.split(NODE_SEPARATOR)) {
            nodes.add(decodeNode(nodeValue));
        }

        return List.copyOf(nodes);
    }

    private CacheNode decodeNode(String value) {
        String[] fields = value.split(NODE_FIELD_SEPARATOR, NODE_FIELD_COUNT);
        if (fields.length != NODE_FIELD_COUNT) {
            throw new CacheInfoException(INVALID_NODE_MESSAGE);
        }

        return new CacheNode(
                fields[NODE_ID_INDEX],
                fields[NODE_HOST_INDEX],
                ClusterNumberParser.parseInt(fields[NODE_HTTP_PORT_INDEX], TopologyField.HTTP_PORT.getValue()),
                ClusterNumberParser.parseInt(fields[NODE_TCP_PORT_INDEX], TopologyField.TCP_PORT.getValue()),
                ClusterNumberParser.parseInt(fields[NODE_CLUSTER_PORT_INDEX], TopologyField.CLUSTER_PORT.getValue()),
                NodeStatus.valueOf(fields[NODE_STATUS_INDEX])
        );
    }
}
