package com.link.up.server.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.link.up.api.sink.TableDdl;
import com.link.up.server.runtime.JobSnapshot;

import java.util.List;
import java.util.Objects;

/**
 * Backward-compatible REST view of one pipeline with optional offline table DDL.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class PipelineResponse {

    private final JobSnapshot.Pipeline pipeline;
    private final TableDdl tableDdl;

    public PipelineResponse(
            JobSnapshot.Pipeline pipeline,
            TableDdl tableDdl) {
        this.pipeline = Objects.requireNonNull(
                pipeline,
                "pipeline must not be null");
        this.tableDdl = tableDdl;
    }

    public String getPipelineId() {
        return pipeline.getPipelineId();
    }

    public String getDataSetId() {
        return pipeline.getDataSetId();
    }

    public String getStatus() {
        return pipeline.getStatus();
    }

    public JobSnapshot.Source getSource() {
        return pipeline.getSource();
    }

    public JobSnapshot.Sink getSink() {
        return pipeline.getSink();
    }

    public TableDdl getTableDdl() {
        return tableDdl;
    }

    public JobSnapshot.Commit getCommitSummary() {
        return pipeline.getCommitSummary();
    }

    public List<JobSnapshot.Task> getTasks() {
        return pipeline.getTasks();
    }

    public List<JobSnapshot.Channel> getChannels() {
        return pipeline.getChannels();
    }

    public String getErrorMessage() {
        return pipeline.getErrorMessage();
    }
}
