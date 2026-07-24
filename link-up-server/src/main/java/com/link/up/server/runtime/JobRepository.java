package com.link.up.server.runtime;

import java.util.List;

public interface JobRepository {

    void save(JobSnapshot snapshot);

    JobSnapshot get(String jobId);

    List<JobSnapshot> list();
}