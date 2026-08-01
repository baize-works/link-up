package com.link.up.server.runtime;

import java.util.List;

public interface JobRepository {

    void save(JobSnapshot snapshot);

    default void save(
            JobSnapshot snapshot,
            JobExecutionMetadata metadata) {
        save(snapshot);
    }

    JobSnapshot get(String jobId);

    default JobExecutionMetadata getMetadata(
            String jobId) {
        return null;
    }

    List<JobSnapshot> list();
}
