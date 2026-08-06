package com.link.up.server.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Incremental Link-Up job log page using a byte-offset cursor. */
public final class JobLogPageResponse {

    private final String jobId;
    private final String externalExecutionId;
    private final String runId;
    private final List<JobLogEntry> items;
    private final long nextCursor;
    private final boolean completed;

    public JobLogPageResponse(
            String jobId,
            String externalExecutionId,
            String runId,
            List<JobLogEntry> items,
            long nextCursor,
            boolean completed) {

        this.jobId = jobId;
        this.externalExecutionId = externalExecutionId;
        this.runId = runId;
        this.items =
                Collections.unmodifiableList(
                        new ArrayList<JobLogEntry>(items));
        this.nextCursor = nextCursor;
        this.completed = completed;
    }

    public String getJobId() {
        return jobId;
    }

    public String getExternalExecutionId() {
        return externalExecutionId;
    }

    public String getRunId() {
        return runId;
    }

    public List<JobLogEntry> getItems() {
        return items;
    }

    public long getNextCursor() {
        return nextCursor;
    }

    public boolean isCompleted() {
        return completed;
    }
}
