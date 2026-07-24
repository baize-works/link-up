package com.link.up.framework.planner;

import com.link.up.framework.connector.PreparedSink;
import com.link.up.framework.execution.TaskId;

import java.util.Objects;

/**
 * SinkTask 执行计划。
 */
public final class SinkTaskPlan {

    private final TaskId taskId;

    private final PreparedSink preparedSink;

    public SinkTaskPlan(
            TaskId taskId,
            PreparedSink preparedSink) {

        this.taskId =
                Objects.requireNonNull(
                        taskId,
                        "taskId must not be null");

        this.preparedSink =
                Objects.requireNonNull(
                        preparedSink,
                        "preparedSink must not be null");
    }

    public TaskId getTaskId() {
        return taskId;
    }

    public PreparedSink getPreparedSink() {
        return preparedSink;
    }
}