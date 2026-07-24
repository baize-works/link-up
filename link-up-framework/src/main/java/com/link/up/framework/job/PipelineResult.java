package com.link.up.framework.job;

import java.util.Objects;

/**
 * 单条 Pipeline 的最终执行结果。
 *
 * <p>除执行状态外，还保留 Source、Sink 和目标表等展示信息，
 * 供 Launcher、REST API 或 Web 控制台输出执行详情。
 */
public final class PipelineResult {

    private final String pipelineId;
    private final String dataSetId;

    private final String sourceIdentifier;
    private final String sourceTable;
    private final int sourceTaskCount;

    private final String sinkIdentifier;
    private final String sinkTable;
    private final int sinkTaskCount;

    private final PipelineStatus status;
    private final CommitSummary commitSummary;
    private final Throwable failure;

    /**
     * 兼容原有构造方法。
     */
    public PipelineResult(
            String pipelineId,
            String dataSetId,
            PipelineStatus status,
            CommitSummary commitSummary,
            Throwable failure) {

        this(
                pipelineId,
                dataSetId,
                "-",
                dataSetId,
                0,
                "-",
                "-",
                0,
                status,
                commitSummary,
                failure);
    }

    public PipelineResult(
            String pipelineId,
            String dataSetId,
            String sourceIdentifier,
            String sourceTable,
            int sourceTaskCount,
            String sinkIdentifier,
            String sinkTable,
            int sinkTaskCount,
            PipelineStatus status,
            CommitSummary commitSummary,
            Throwable failure) {

        this.pipelineId =
                requireText(
                        pipelineId,
                        "pipelineId");

        this.dataSetId =
                requireText(
                        dataSetId,
                        "dataSetId");

        this.sourceIdentifier =
                displayValue(sourceIdentifier);

        this.sourceTable =
                displayValue(sourceTable);

        this.sourceTaskCount =
                Math.max(0, sourceTaskCount);

        this.sinkIdentifier =
                displayValue(sinkIdentifier);

        this.sinkTable =
                displayValue(sinkTable);

        this.sinkTaskCount =
                Math.max(0, sinkTaskCount);

        this.status =
                Objects.requireNonNull(
                        status,
                        "status must not be null");

        this.commitSummary =
                Objects.requireNonNull(
                        commitSummary,
                        "commitSummary must not be null");

        this.failure = failure;
    }

    private static String requireText(
            String value,
            String name) {

        if (value == null
                || value.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    name + " must not be blank");
        }

        return value;
    }

    private static String displayValue(
            String value) {

        if (value == null
                || value.trim().isEmpty()) {

            return "-";
        }

        return value;
    }

    public String getPipelineId() {
        return pipelineId;
    }

    public String getDataSetId() {
        return dataSetId;
    }

    public String getSourceIdentifier() {
        return sourceIdentifier;
    }

    public String getSourceTable() {
        return sourceTable;
    }

    public int getSourceTaskCount() {
        return sourceTaskCount;
    }

    public String getSinkIdentifier() {
        return sinkIdentifier;
    }

    public String getSinkTable() {
        return sinkTable;
    }

    public int getSinkTaskCount() {
        return sinkTaskCount;
    }

    public PipelineStatus getStatus() {
        return status;
    }

    public CommitSummary getCommitSummary() {
        return commitSummary;
    }

    public Throwable getFailure() {
        return failure;
    }
}