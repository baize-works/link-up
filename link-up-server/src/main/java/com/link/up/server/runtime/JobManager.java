package com.link.up.server.runtime;

import com.link.up.framework.job.JobDefinition;

import java.util.List;

public interface JobManager
        extends AutoCloseable {

    JobSnapshot submit(JobDefinition definition);

    default JobSnapshot submit(
            JobSubmission submission) {
        return submit(submission.getDefinition());
    }

    JobSnapshot getJob(String jobId);

    default JobSnapshot getJobByExternalExecutionId(
            String externalExecutionId) {
        throw new JobNotFoundException(
                externalExecutionId);
    }

    default JobExecutionMetadata getMetadata(
            String jobId) {
        return null;
    }

    List<JobSnapshot> listJobs();

    JobSnapshot cancel(String jobId);

    default int getRunningJobCount() {
        return 0;
    }

    default int getQueuedJobCount() {
        return 0;
    }

    default int getActiveJobCount() {
        return getRunningJobCount()
                + getQueuedJobCount();
    }

    boolean isClosed();

    void close();
}
