package com.link.up.framework.connector;

import com.link.up.framework.job.ExecutionConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 已完成 Connector 准备的 Job。
 */
public final class PreparedJob {

    private final String jobName;

    private final PreparedSource<?> source;

    private final java.util.Map<String, List<PreparedSink>> sinksByDataSet;

    private final ExecutionConfig executionConfig;

    public PreparedJob(
            String jobName,
            PreparedSource<?> source,
            java.util.Map<String, List<PreparedSink>> sinksByDataSet,
            ExecutionConfig executionConfig) {

        this.jobName =
                Objects.requireNonNull(
                        jobName,
                        "jobName must not be null");

        this.source =
                Objects.requireNonNull(
                        source,
                        "source must not be null");

        java.util.Map<String, List<PreparedSink>> copy = new java.util.LinkedHashMap<String, List<PreparedSink>>();
        for (java.util.Map.Entry<String, List<PreparedSink>> entry : Objects.requireNonNull(sinksByDataSet, "sinksByDataSet must not be null").entrySet())
            copy.put(entry.getKey(), Collections.unmodifiableList(new ArrayList<PreparedSink>(entry.getValue())));
        this.sinksByDataSet = Collections.unmodifiableMap(copy);
        if (this.sinksByDataSet.isEmpty()) throw new IllegalArgumentException("sinksByDataSet must not be empty");

        this.executionConfig =
                Objects.requireNonNull(
                        executionConfig,
                        "executionConfig must not be null");
    }

    public String getJobName() {
        return jobName;
    }

    public PreparedSource<?> getSource() {
        return source;
    }

    public List<PreparedSink> getSinks(String dataSetId) {
        List<PreparedSink> sinks = sinksByDataSet.get(dataSetId);
        if (sinks == null) throw new IllegalArgumentException("No prepared sink for data set: " + dataSetId);
        return sinks;
    }

    public ExecutionConfig getExecutionConfig() {
        return executionConfig;
    }
}
