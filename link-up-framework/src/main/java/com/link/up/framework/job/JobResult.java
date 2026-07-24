package com.link.up.framework.job;

import com.link.up.api.dirtydata.DirtyDataSummary;
import com.link.up.framework.metrics.JobMetrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Job 执行结果。
 */
public final class JobResult {

    private final String jobName;

    private final JobStatus status;

    private final long startTimeMillis;

    private final long endTimeMillis;

    private final JobMetrics metrics;

    private final Throwable failure;
    private final CommitSummary commitSummary;
    private final DirtyDataSummary dirtyDataSummary;
    private final List<PipelineResult> pipelineResults;

    public JobResult(
            String jobName,
            JobStatus status,
            long startTimeMillis,
            long endTimeMillis,
            JobMetrics metrics,
            Throwable failure) {
        this(jobName, status, startTimeMillis, endTimeMillis, metrics, failure, CommitSummary.empty(), DirtyDataSummary.empty(), Collections.<PipelineResult>emptyList());
    }

    public JobResult(
            String jobName, JobStatus status, long startTimeMillis, long endTimeMillis,
            JobMetrics metrics, Throwable failure, CommitSummary commitSummary) {
        this(jobName, status, startTimeMillis, endTimeMillis, metrics, failure, commitSummary, DirtyDataSummary.empty(), Collections.<PipelineResult>emptyList());
    }

    public JobResult(String jobName, JobStatus status, long startTimeMillis, long endTimeMillis, JobMetrics metrics, Throwable failure, CommitSummary commitSummary, DirtyDataSummary dirtyDataSummary) {
        this(jobName, status, startTimeMillis, endTimeMillis, metrics, failure, commitSummary, dirtyDataSummary, Collections.<PipelineResult>emptyList());
    }

    public JobResult(String jobName, JobStatus status, long startTimeMillis, long endTimeMillis, JobMetrics metrics, Throwable failure, CommitSummary commitSummary, DirtyDataSummary dirtyDataSummary, List<PipelineResult> pipelineResults) {

        this.jobName =
                Objects.requireNonNull(
                        jobName,
                        "jobName must not be null");

        this.status =
                Objects.requireNonNull(
                        status,
                        "status must not be null");

        this.startTimeMillis = startTimeMillis;
        this.endTimeMillis = endTimeMillis;

        this.metrics =
                Objects.requireNonNull(
                        metrics,
                        "metrics must not be null");

        this.failure = failure;
        this.commitSummary = Objects.requireNonNull(commitSummary, "commitSummary must not be null");
        this.dirtyDataSummary = Objects.requireNonNull(dirtyDataSummary, "dirtyDataSummary must not be null");
        this.pipelineResults = Collections.unmodifiableList(new ArrayList<PipelineResult>(pipelineResults));
    }

    public void throwIfFailed() throws Exception {
        if (failure == null) {
            return;
        }

        if (failure instanceof Exception) {
            throw (Exception) failure;
        }

        if (failure instanceof Error) {
            throw (Error) failure;
        }

        throw new RuntimeException(failure);
    }

    public String getJobName() {
        return jobName;
    }

    public JobStatus getStatus() {
        return status;
    }

    public long getStartTimeMillis() {
        return startTimeMillis;
    }

    public long getEndTimeMillis() {
        return endTimeMillis;
    }

    public long getDurationMillis() {
        return Math.max(
                0L,
                endTimeMillis - startTimeMillis);
    }

    public JobMetrics getMetrics() {
        return metrics;
    }

    public Throwable getFailure() {
        return failure;
    }

    public CommitSummary getCommitSummary() {
        return commitSummary;
    }

    public DirtyDataSummary getDirtyDataSummary() {
        return dirtyDataSummary;
    }

    public List<PipelineResult> getPipelineResults() {
        return pipelineResults;
    }

    public boolean isSuccess() {
        return status == JobStatus.SUCCEEDED;
    }
}
