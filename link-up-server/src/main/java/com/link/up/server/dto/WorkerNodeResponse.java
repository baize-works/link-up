package com.link.up.server.dto;

import com.link.up.server.runtime.JobManager;
import com.link.up.server.runtime.ServerJobStatus;
import com.link.up.server.runtime.WorkerIdentity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 单节点离线 Worker 信息。
 */
public final class WorkerNodeResponse {

    private final String nodeId;
    private final String nodeName;
    private final String instanceId;
    private final String version;
    private final String status;
    private final long startedAtMillis;
    private final boolean offlineOnly;
    private final int maxConcurrentJobs;
    private final int maxQueuedJobs;
    private final int runningJobs;
    private final int queuedJobs;
    private final int activeJobs;
    private final List<ServerJobStatus> lifecycle;

    public WorkerNodeResponse(
            WorkerIdentity identity,
            JobManager manager,
            int maxConcurrentJobs,
            int maxQueuedJobs) {

        this.nodeId = identity.getNodeId();
        this.nodeName = identity.getNodeName();
        this.instanceId = identity.getInstanceId();
        this.version = identity.getVersion();
        this.status = manager.isClosed()
                ? "DOWN"
                : "UP";
        this.startedAtMillis =
                identity.getStartedAtMillis();
        this.offlineOnly = true;
        this.maxConcurrentJobs = maxConcurrentJobs;
        this.maxQueuedJobs = maxQueuedJobs;
        this.runningJobs =
                manager.getRunningJobCount();
        this.queuedJobs =
                manager.getQueuedJobCount();
        this.activeJobs =
                manager.getActiveJobCount();
        this.lifecycle =
                Collections.unmodifiableList(
                        Arrays.asList(
                                ServerJobStatus.CREATED,
                                ServerJobStatus.SUBMITTED,
                                ServerJobStatus.QUEUED,
                                ServerJobStatus.RUNNING,
                                ServerJobStatus.SUCCEEDED,
                                ServerJobStatus.FAILED,
                                ServerJobStatus.CANCELED,
                                ServerJobStatus.LOST));
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

    public String getStatus() {
        return status;
    }

    public long getStartedAtMillis() {
        return startedAtMillis;
    }

    public boolean isOfflineOnly() {
        return offlineOnly;
    }

    public int getMaxConcurrentJobs() {
        return maxConcurrentJobs;
    }

    public int getMaxQueuedJobs() {
        return maxQueuedJobs;
    }

    public int getRunningJobs() {
        return runningJobs;
    }

    public int getQueuedJobs() {
        return queuedJobs;
    }

    public int getActiveJobs() {
        return activeJobs;
    }

    public List<ServerJobStatus> getLifecycle() {
        return lifecycle;
    }
}
