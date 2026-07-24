package com.link.up.framework.planner;

import com.link.up.framework.job.ExecutionConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Job 的计划；Pipeline 是一级执行模型。
 */
public final class ExecutionPlan {
    private final String jobName;
    private final ExecutionConfig executionConfig;
    private final List<PipelinePlan> pipelinePlans;

    public ExecutionPlan(String jobName, ExecutionConfig executionConfig, List<PipelinePlan> pipelinePlans) {
        this.jobName = Objects.requireNonNull(jobName, "jobName must not be null");
        this.executionConfig = Objects.requireNonNull(executionConfig, "executionConfig must not be null");
        this.pipelinePlans = Collections.unmodifiableList(new ArrayList<PipelinePlan>(Objects.requireNonNull(pipelinePlans, "pipelinePlans must not be null")));
    }

    public String getJobName() {
        return jobName;
    }

    public ExecutionConfig getExecutionConfig() {
        return executionConfig;
    }

    public List<PipelinePlan> getPipelinePlans() {
        return pipelinePlans;
    }

    /**
     * 仅用于兼容旧的观测代码，执行层不得以此重建 Pipeline。
     */
    public List<SourceTaskPlan<?>> getSourceTaskPlans() {
        List<SourceTaskPlan<?>> result = new ArrayList<SourceTaskPlan<?>>();
        for (PipelinePlan p : pipelinePlans) result.addAll(p.getSourceTaskPlans());
        return Collections.unmodifiableList(result);
    }

    public List<SinkTaskPlan> getSinkTaskPlans() {
        List<SinkTaskPlan> result = new ArrayList<SinkTaskPlan>();
        for (PipelinePlan p : pipelinePlans) result.addAll(p.getSinkTaskPlans());
        return Collections.unmodifiableList(result);
    }

    public boolean isEmpty() {
        return pipelinePlans.isEmpty();
    }
}
