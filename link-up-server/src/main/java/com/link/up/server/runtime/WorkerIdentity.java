package com.link.up.server.runtime;

import java.util.UUID;

/**
 * 单个 Link-Up Worker 进程的稳定节点身份和易变实例身份。
 */
public final class WorkerIdentity {

    private final String nodeId;
    private final String nodeName;
    private final String instanceId;
    private final String version;
    private final long startedAtMillis;

    public WorkerIdentity(
            String nodeId,
            String nodeName,
            String version) {

        this.nodeId = requireText(nodeId, "nodeId");
        this.nodeName = requireText(nodeName, "nodeName");
        this.version = requireText(version, "version");
        this.startedAtMillis = System.currentTimeMillis();
        this.instanceId =
                this.nodeId
                        + "-"
                        + startedAtMillis
                        + "-"
                        + UUID.randomUUID()
                        .toString();
    }

    public static String implementationVersion() {
        Package packageInfo =
                WorkerIdentity.class
                        .getPackage();

        String value =
                packageInfo == null
                        ? null
                        : packageInfo
                        .getImplementationVersion();

        return value == null
                || value.trim().isEmpty()
                ? "1.0.0"
                : value.trim();
    }

    private static String requireText(
            String value,
            String name) {

        if (value == null
                || value.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    name + " must not be blank");
        }

        return value.trim();
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getVersion() {
        return version;
    }

    public long getStartedAtMillis() {
        return startedAtMillis;
    }
}
