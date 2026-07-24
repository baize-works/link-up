package com.link.up.framework.execution;

import com.link.up.framework.metrics.TaskMetrics;

import java.util.Objects;

/**
 * Task 运行上下文。
 */
public final class TaskContext {

    private final TaskId taskId;

    private final CancellationToken cancellationToken;

    private final TaskMetrics metrics;

    private final ClassLoader classLoader;

    public TaskContext(
            TaskId taskId,
            CancellationToken cancellationToken,
            TaskMetrics metrics,
            ClassLoader classLoader) {

        this.taskId =
                Objects.requireNonNull(
                        taskId,
                        "taskId must not be null");

        this.cancellationToken =
                Objects.requireNonNull(
                        cancellationToken,
                        "cancellationToken must not be null");

        this.metrics =
                Objects.requireNonNull(
                        metrics,
                        "metrics must not be null");

        this.classLoader =
                Objects.requireNonNull(
                        classLoader,
                        "classLoader must not be null");
    }

    public TaskId getTaskId() {
        return taskId;
    }

    public CancellationToken getCancellationToken() {
        return cancellationToken;
    }

    public TaskMetrics getMetrics() {
        return metrics;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }
}