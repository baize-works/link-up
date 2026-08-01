package com.link.up.server.dto;

import com.link.up.server.runtime.JobExecutionMetadata;
import com.link.up.server.runtime.JobSnapshot;
import com.link.up.server.runtime.JobStateTransition;
import com.link.up.server.runtime.ServerJobStatus;
import com.link.up.server.runtime.WorkerIdentity;

import java.util.Collections;
import java.util.List;

/**
 * 对控制面稳定输出的离线作业协议视图。
 */
public final class JobResponse {

    private final JobSnapshot snapshot;
    private final JobExecutionMetadata metadata;
    private final WorkerIdentity worker;

    public JobResponse(
            JobSnapshot snapshot,
            JobExecutionMetadata metadata,
            WorkerIdentity worker) {

        this.snapshot = snapshot;
        this.metadata = metadata;
        this.worker = worker;
    }

    public JobSnapshot.Summary toSummary() {
        return snapshot.toSummary();
    }

    public String getJobId() {
        return snapshot.getJobId();
    }

    public String getExternalExecutionId() {
        return metadata == null
                ? null
                : metadata.getExternalExecutionId();
    }

    public String getIdempotencyKey() {
        return metadata == null
                ? null
                : metadata.getIdempotencyKey();
    }

    public String getJobName() {
        return snapshot.getJobName();
    }

    public int getDefinitionVersion() {
        return metadata == null
                ? 1
                : metadata.getDefinitionVersion();
    }

    public String getWorkerNodeId() {
        return worker.getNodeId();
    }

    public String getWorkerInstanceId() {
        return worker.getInstanceId();
    }

    public ServerJobStatus getStatus() {
        return snapshot.getStatus();
    }

    public long getStateVersion() {
        return metadata == null
                ? 0L
                : metadata.getStateVersion();
    }

    public boolean isCancellationRequested() {
        return metadata != null
                && metadata.isCancellationRequested();
    }

    public long getCreateTimeMillis() {
        return snapshot.getCreateTimeMillis();
    }

    public long getSubmittedTimeMillis() {
        return metadata == null
                ? 0L
                : metadata.getSubmittedTimeMillis();
    }

    public long getQueuedTimeMillis() {
        return metadata == null
                ? 0L
                : metadata.getQueuedTimeMillis();
    }

    public long getStartTimeMillis() {
        return snapshot.getStartTimeMillis();
    }

    public long getEndTimeMillis() {
        return snapshot.getEndTimeMillis();
    }

    public long getDurationMillis() {
        return snapshot.getDurationMillis();
    }

    public JobSnapshot.Metrics getMetrics() {
        return snapshot.getMetrics();
    }

    public JobSnapshot.Commit getCommitSummary() {
        return snapshot.getCommitSummary();
    }

    public List<JobSnapshot.Pipeline> getPipelines() {
        return snapshot.getPipelines();
    }

    public List<JobStateTransition> getTransitions() {
        return metadata == null
                ? Collections
                .<JobStateTransition>emptyList()
                : metadata.getTransitions();
    }

    public String getErrorCode() {
        return snapshot.getErrorCode();
    }

    public String getErrorMessage() {
        return snapshot.getErrorMessage();
    }
}
